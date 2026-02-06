package com.nei10u.tip.goods.processor;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nei10u.tip.mapper.GoodsMapper;
import com.nei10u.tip.goods.sync.TbGoodsSyncProperties;
import com.nei10u.tip.model.DtkGoods;
import com.nei10u.tip.service.DtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * TB 数据域：本地商品库同步/清理（基于大淘客 DTK）。
 *
 * 说明：
 * - 该模块只操作本地表 `dtk_goods`（实体：{@link DtkGoods}）
 * - 不承诺强一致，只追求“可持续增量同步 + 失效标记 + 过期券清理”
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbGoodsSyncProcessor {

    private final GoodsMapper goodsMapper;
    private final DtkApiService dtkApiService;
    private final SqlSessionFactory sqlSessionFactory;
    private final TbGoodsSyncProperties props;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ReentrantLock SYNC_LOCK = new ReentrantLock();

    public void syncGoods() {
        boolean locked = tryLock("syncGoods");
        if (!locked) return;
        try {
            long count = goodsMapper.selectCount(new LambdaQueryWrapper<>());
            if (count == 0) {
                syncFullGoods();
            } else {
                syncIncrementalGoods();
            }
        } finally {
            unlockIfNeeded(locked);
        }
    }

    public void syncStaleGoods() {
        boolean locked = tryLock("syncStaleGoods");
        if (!locked) return;
        try {
            log.info("开始同步失效商品...");
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusMinutes(10);

            String startTimeStr = startTime.format(TIME_FORMATTER);
            String endTimeStr = endTime.format(TIME_FORMATTER);

            int pageId = 1;
            int pageSize = 100;
            boolean hasMore = true;

            while (hasMore) {
                try {
                    String response = dtkApiService.getStaleGoodsByTime(pageId, pageSize, startTimeStr, endTimeStr);
                    List<String> staleGoodsIds = parseStaleGoodsResponse(response);
                    if (staleGoodsIds.isEmpty()) {
                        hasMore = false;
                    } else {
                        LambdaUpdateWrapper<DtkGoods> uw = new LambdaUpdateWrapper<DtkGoods>()
                                .in(DtkGoods::getGoodsId, staleGoodsIds)
                                .set(DtkGoods::getStatus, 0)
                                .set(DtkGoods::getUpdateTime, LocalDateTime.now());
                        goodsMapper.update(null, uw);
                        log.info("标记失效商品第 {} 页，共 {} 条", pageId, staleGoodsIds.size());
                        pageId++;
                        Thread.sleep(200);
                    }
                } catch (Exception e) {
                    log.error("同步失效商品第 {} 页失败", pageId, e);
                    hasMore = false;
                }
            }
        } finally {
            unlockIfNeeded(locked);
        }
    }

    public void syncUpdatedGoods() {
        boolean locked = tryLock("syncUpdatedGoods");
        if (!locked) return;
        try {
            log.info("开始同步更新商品...");
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusMinutes(5);

            String startTimeStr = startTime.format(TIME_FORMATTER);
            String endTimeStr = endTime.format(TIME_FORMATTER);

            int pageId = 1;
            int pageSize = 100;
            boolean hasMore = true;

            while (hasMore) {
                try {
                    String response = dtkApiService.getNewestGoods(pageId, pageSize, startTimeStr, endTimeStr);
                    List<DtkGoods> updated = parseGoodsResponse(response);
                    if (updated.isEmpty()) {
                        hasMore = false;
                    } else {
                        for (DtkGoods g : updated) {
                            if (g.getTitle() == null && g.getGoodsId() != null) {
                                fetchAndFillGoodsDetail(g);
                            }
                        }
                        saveGoodsBatch(updated);
                        log.info("更新商品第 {} 页，共 {} 条", pageId, updated.size());
                        pageId++;
                        Thread.sleep(200);
                    }
                } catch (Exception e) {
                    log.error("同步更新商品第 {} 页失败", pageId, e);
                    hasMore = false;
                }
            }
        } finally {
            unlockIfNeeded(locked);
        }
    }

    public int cleanupExpiredCouponGoods() {
        boolean locked = tryLock("cleanupExpiredCouponGoods");
        if (!locked) return 0;
        try {
            LocalDateTime now = LocalDateTime.now();
            return goodsMapper.delete(new LambdaQueryWrapper<DtkGoods>()
                    .isNotNull(DtkGoods::getCouponEndTime)
                    .lt(DtkGoods::getCouponEndTime, now));
        } finally {
            unlockIfNeeded(locked);
        }
    }

    private void syncFullGoods() {
        log.info("开始全量同步商品...");
        int pageId = 1;
        int pageSize = 100;
        boolean hasMore = true;

        while (hasMore) {
            try {
                String response = dtkApiService.getGoodsList(pageId, pageSize);
                List<DtkGoods> list = parseGoodsResponse(response);
                if (list.isEmpty()) {
                    hasMore = false;
                    log.info("全量同步结束，第 {} 页无数据", pageId);
                } else {
                    saveGoodsBatch(list);
                    log.info("全量同步第 {} 页，共 {} 条", pageId, list.size());
                    pageId++;
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                log.error("全量同步第 {} 页失败", pageId, e);
                hasMore = false;
            }
        }
    }

    private void syncIncrementalGoods() {
        log.info("开始增量同步商品...");
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(10);

        String startTimeStr = startTime.format(TIME_FORMATTER);
        String endTimeStr = endTime.format(TIME_FORMATTER);

        int pageId = 1;
        int pageSize = 100;
        boolean hasMore = true;

        while (hasMore) {
            try {
                String response = dtkApiService.pullGoodsByTime(pageId, pageSize, null, startTimeStr, endTimeStr);
                List<DtkGoods> list = parseGoodsResponse(response);
                if (list.isEmpty()) {
                    hasMore = false;
                } else {
                    saveGoodsBatch(list);
                    log.info("增量同步第 {} 页，共 {} 条", pageId, list.size());
                    pageId++;
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                log.error("增量同步第 {} 页失败", pageId, e);
                hasMore = false;
            }
        }
    }

    private void fetchAndFillGoodsDetail(DtkGoods dtkGoods) {
        try {
            String detailResponse = dtkApiService.getGoodsDetails(dtkGoods.getGoodsId());
            if (!StringUtils.hasText(detailResponse)) return;

            JSONObject json = com.alibaba.fastjson2.JSON.parseObject(detailResponse);
            if (json == null || json.getIntValue("code") != 0) return;

            JSONObject data = json.getJSONObject("data");
            if (data == null) return;

            if (dtkGoods.getTitle() == null) dtkGoods.setTitle(data.getString("title"));
            if (dtkGoods.getDtitle() == null) dtkGoods.setDtitle(data.getString("dtitle"));
            if (dtkGoods.getDescription() == null) dtkGoods.setDescription(data.getString("desc"));
            if (dtkGoods.getMainPic() == null) dtkGoods.setMainPic(data.getString("mainPic"));
            if (dtkGoods.getMarketingMainPic() == null) dtkGoods.setMarketingMainPic(data.getString("marketingMainPic"));
            if (dtkGoods.getShopName() == null) dtkGoods.setShopName(data.getString("shopName"));
        } catch (Exception e) {
            log.warn("Fetch dtkGoods detail failed for {}: {}", dtkGoods.getGoodsId(), e.getMessage());
        }
    }

    private void saveGoodsBatch(List<DtkGoods> list) {
        if (list == null || list.isEmpty()) return;

        // Sort by GoodsId to prevent deadlock during concurrent updates
        list.sort(Comparator.comparing(DtkGoods::getGoodsId));

        List<String> goodsIds = list.stream()
                .map(DtkGoods::getGoodsId)
                .filter(Objects::nonNull)
                .toList();

        if (!goodsIds.isEmpty()) {
            List<DtkGoods> existing = goodsMapper.selectList(new LambdaQueryWrapper<DtkGoods>()
                    .in(DtkGoods::getGoodsId, goodsIds));
            Map<String, Long> idMap = existing.stream()
                    .filter(e -> e.getGoodsId() != null && e.getId() != null)
                    .collect(Collectors.toMap(DtkGoods::getGoodsId, DtkGoods::getId, (a, b) -> a));

            for (DtkGoods g : list) {
                Long id = idMap.get(g.getGoodsId());
                if (id != null) g.setId(id);
                g.setStatus(1);
                g.setUpdateTime(LocalDateTime.now());
                if (g.getId() == null) {
                    g.setCreateTime(LocalDateTime.now());
                }
            }
        }

        // 使用 MyBatis batch 执行（避免逐条 round-trip）
        int batchSize = props.getBatchSize() > 0 ? props.getBatchSize() : 300;
        SqlSession session = null;
        try {
            session = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
            GoodsMapper batchMapper = session.getMapper(GoodsMapper.class);

            int i = 0;
            for (DtkGoods g : list) {
                if (g.getId() == null) {
                    batchMapper.insert(g);
                } else {
                    batchMapper.updateById(g);
                }

                i++;
                if (i % batchSize == 0) {
                    session.flushStatements();
                    session.commit();
                }
            }

            session.flushStatements();
            session.commit();
        } catch (Exception e) {
            if (session != null) {
                try {
                    session.rollback();
                } catch (Exception ignore) {
                }
            }
            throw e;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private boolean tryLock(String op) {
        if (!props.isLockEnabled()) return true;
        long timeoutMs = Math.max(props.getLockTimeoutMs(), 0);
        try {
            boolean ok = timeoutMs == 0
                    ? SYNC_LOCK.tryLock()
                    : SYNC_LOCK.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (!ok) {
                log.warn("TB sync skipped (lock busy), op={}", op);
            }
            return ok;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("TB sync skipped (interrupted), op={}", op);
            return false;
        }
    }

    private void unlockIfNeeded(boolean locked) {
        if (!props.isLockEnabled()) return;
        if (!locked) return;
        try {
            SYNC_LOCK.unlock();
        } catch (Exception ignore) {
        }
    }

    private List<String> parseStaleGoodsResponse(String response) {
        List<String> list = new ArrayList<>();
        if (!StringUtils.hasText(response)) return list;

        try {
            JSONObject json = com.alibaba.fastjson2.JSON.parseObject(response);
            if (json == null || json.getIntValue("code") != 0) return list;
            JSONObject data = json.getJSONObject("data");
            if (data == null) return list;
            JSONArray goodsArray = data.getJSONArray("list");
            if (goodsArray == null) return list;
            for (int i = 0; i < goodsArray.size(); i++) {
                JSONObject obj = goodsArray.getJSONObject(i);
                if (obj != null) list.add(obj.getString("goodsId"));
            }
        } catch (Exception e) {
            log.error("Parse stale goods response error", e);
        }
        return list;
    }

    private List<DtkGoods> parseGoodsResponse(String response) {
        List<DtkGoods> list = new ArrayList<>();
        if (!StringUtils.hasText(response)) return list;

        try {
            JSONObject json = com.alibaba.fastjson2.JSON.parseObject(response);
            if (json == null || json.getIntValue("code") != 0) return list;

            JSONObject data = json.getJSONObject("data");
            if (data == null) return list;

            JSONArray goodsArray = data.getJSONArray("list");
            if (goodsArray == null) return list;

            for (int i = 0; i < goodsArray.size(); i++) {
                JSONObject obj = goodsArray.getJSONObject(i);
                if (obj == null) continue;

                if (i == 0) {
                    log.info("DEBUG - API Response Keys: {}", obj.keySet());
                }

                DtkGoods g = new DtkGoods();
                g.setGoodsId(obj.getString("goodsId"));
                if (obj.containsKey("title")) g.setTitle(obj.getString("title"));
                if (obj.containsKey("dtitle")) g.setDtitle(obj.getString("dtitle"));
                if (obj.containsKey("desc")) g.setDescription(obj.getString("desc"));
                if (obj.containsKey("mainPic")) g.setMainPic(obj.getString("mainPic"));
                if (obj.containsKey("marketingMainPic")) g.setMarketingMainPic(obj.getString("marketingMainPic"));
                if (obj.containsKey("actualPrice")) g.setPrice(obj.getBigDecimal("actualPrice"));
                if (obj.containsKey("originalPrice")) g.setOriginalPrice(obj.getBigDecimal("originalPrice"));
                if (obj.containsKey("couponPrice")) g.setCouponPrice(obj.getBigDecimal("couponPrice"));
                if (obj.containsKey("couponLink")) g.setCouponLink(obj.getString("couponLink"));

                try {
                    if (obj.containsKey("couponStartTime")) {
                        String cStart = obj.getString("couponStartTime");
                        if (StringUtils.hasText(cStart))
                            g.setCouponStartTime(LocalDateTime.parse(cStart, TIME_FORMATTER));
                    }
                    if (obj.containsKey("couponEndTime")) {
                        String cEnd = obj.getString("couponEndTime");
                        if (StringUtils.hasText(cEnd))
                            g.setCouponEndTime(LocalDateTime.parse(cEnd, TIME_FORMATTER));
                    }
                    if (obj.containsKey("activityStartTime")) {
                        String aStart = obj.getString("activityStartTime");
                        if (StringUtils.hasText(aStart))
                            g.setActivityStartTime(LocalDateTime.parse(aStart, TIME_FORMATTER));
                    }
                    if (obj.containsKey("activityEndTime")) {
                        String aEnd = obj.getString("activityEndTime");
                        if (StringUtils.hasText(aEnd))
                            g.setActivityEndTime(LocalDateTime.parse(aEnd, TIME_FORMATTER));
                    }
                } catch (Exception e) {
                    log.debug("Date parse error: {}", e.getMessage());
                }

                if (obj.containsKey("commissionRate")) g.setCommissionRate(obj.getBigDecimal("commissionRate"));
                if (obj.containsKey("monthSales")) g.setSalesVolume(obj.getInteger("monthSales"));
                if (obj.containsKey("shopType")) g.setShopType(obj.getInteger("shopType"));
                if (obj.containsKey("shopName")) g.setShopName(obj.getString("shopName"));
                if (obj.containsKey("shopLevel")) g.setShopLevel(obj.getInteger("shopLevel"));
                if (obj.containsKey("brandName")) g.setBrandName(obj.getString("brandName"));
                if (obj.containsKey("activityType")) g.setActivityType(obj.getInteger("activityType"));

                list.add(g);
            }
        } catch (Exception e) {
            log.error("Parse goods response error", e);
        }
        return list;
    }
}

