package com.nei10u.tip.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nei10u.tip.mapper.DtkGoodsMapper;
import com.nei10u.tip.model.DtkGoods;
import com.nei10u.tip.service.DtkApiService;
import com.nei10u.tip.service.DtkGoodsService;
import com.nei10u.tip.service.ZtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DtkGoodsServiceImpl extends ServiceImpl<DtkGoodsMapper, DtkGoods> implements DtkGoodsService {

    private final DtkApiService dtkApiService;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public JSONObject getGoodsList(int pageId, int pageSize) {
        // 从数据库读取商品，过滤掉失效商品
        Page<DtkGoods> page = new Page<>(pageId, pageSize);
        // 执行分页查询，条件为状态正常，按更新时间倒序
        this.page(page, new LambdaQueryWrapper<DtkGoods>()
                .eq(DtkGoods::getStatus, 1) // Only fetch valid goods
                .orderByDesc(DtkGoods::getUpdateTime));

        // 构建返回结果JSONObject
        JSONObject result = new JSONObject();
        // 设置商品列表
        result.put("list", page.getRecords());
        // 设置总记录数
        result.put("totalNum", page.getTotal());
        // 设置当前页码
        result.put("pageId", String.valueOf(pageId));

        return result;
    }

    @Override
    public JSONObject getTbTrend(String type, String pageId, String cid, int pageSize) {
        String response = dtkApiService.getRankingList(type, cid);
        if (StringUtils.hasText(response)) {
            return JSON.parseObject(response);
        }
        return new JSONObject();
    }

    @Override
    public JSONObject getJdCurrentTrend(String pageId, String cid) {
        // TODO: Implement JD Trend
        return new JSONObject();
    }

    @Override
    public JSONObject getJd30DaysTrend(String pageId, String cid) {
        // TODO: Implement JD 30 Days Trend
        return new JSONObject();
    }

    @Override
    public JSONObject getPddTrend(String type, String pageId, String cid) {
        // TODO: Implement PDD Trend
        return new JSONObject();
    }

    @Override
    public JSONObject getInfo(String platform, String goodsId, String userId) {
        if ("tb".equals(platform) || "taobao".equals(platform)) {
            String response = dtkApiService.getGoodsDetails(goodsId);
            if (StringUtils.hasText(response)) {
                JSONObject json = JSON.parseObject(response);
                if (json != null && json.containsKey("data")) {
                    JSONObject data = json.getJSONObject("data");
                    // Calculate commission if missing.
                    //
                    // 根据大淘客“平台商品素材”(id=131)字段定义，详情数据通常只保证：
                    // - actualPrice：券后价
                    // - commissionRate：佣金比例（百分比，例如 15 表示 15%）
                    // - divisor：分母（部分场景用于修正比例，常见为 1）
                    // 因此不应依赖 estimateAmount 是否存在来决定是否补算 commission。
                    if (data != null) {
                        // 1、估算佣金
                        BigDecimal existingCommission = data.getBigDecimal("commission");
                        if (existingCommission == null) {
                            BigDecimal estimateAmount = data.getBigDecimal("estimateAmount");
                            if (estimateAmount == null) {
                                BigDecimal price = data.getBigDecimal("actualPrice");
                                if (price == null) {
                                    price = data.getBigDecimal("price");
                                }
                                if (price == null) {
                                    price = data.getBigDecimal("originalPrice");
                                }

                                // fastjson2 对数字/字符串均可用 getBigDecimal，但这里保留容错兜底
                                BigDecimal rate = data.getBigDecimal("commissionRate");
                                if (rate == null && data.containsKey("commissionRate")) {
                                    Object raw = data.get("commissionRate");
                                    try {
                                        if (raw != null) {
                                            rate = new BigDecimal(raw.toString());
                                        }
                                    } catch (Exception ignore) {
                                        // keep null
                                    }
                                }

                                BigDecimal divisor = data.getBigDecimal("divisor");
                                if (divisor == null || divisor.compareTo(BigDecimal.ZERO) <= 0) {
                                    divisor = BigDecimal.ONE;
                                }

                                if (price != null && rate != null) {
                                    // commission = actualPrice * commissionRate(%) / 100 / divisor
                                    BigDecimal commission = price
                                            .multiply(rate)
                                            .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
                                            .divide(divisor, 2, RoundingMode.HALF_UP);
                                    data.put("commission", commission);
                                }
                            } else {
                                data.put("commission", estimateAmount);
                            }
                        }
                        //2、处理详情页图片
                        String detailImages = data.getString("detailPics");
                        if (StringUtils.hasText(detailImages)) {
                            List<String> detailPics = List.of(detailImages.split(","));
                            if (!CollectionUtils.isEmpty(detailPics)) {
                                data.put("detailPics", detailPics);
                            }
                        }
                        String carouselImages = data.getString("imgs");
                        if (StringUtils.hasText(carouselImages)) {
                            List<String> carouselPics = List.of(carouselImages.split(","));
                            if (!CollectionUtils.isEmpty(carouselPics)) {
                                data.put("carouselPics", carouselPics);
                            }
                        }
                        String relatedImages = data.getString("reimgs");
                        if (StringUtils.hasText(relatedImages)) {
                            List<String> relatedPics = List.of(relatedImages.split(","));
                            if (!CollectionUtils.isEmpty(relatedPics)) {
                                data.put("relatedPics", relatedPics);
                            }
                        }
                    }
                }
                return json;
            }
        }
        return new JSONObject();
    }

    @Override
    public JSONObject convertLink(String platform, String goodsId, String userId) {
        if ("tb".equals(platform) || "taobao".equals(platform)) {
            // Assuming pid is configured in dtkApiService or passed null to use default
            String response = dtkApiService.getPrivilegeLink(goodsId, null, null, null, null);
            if (StringUtils.hasText(response)) {
                return JSON.parseObject(response);
            }
        }
        return new JSONObject();
    }

    @Override
    public void syncGoods() {
        // 查询当前商品总数
        long count = this.count();
        // 如果没有商品，执行全量同步
        if (count == 0) {
            syncFullGoods();
        } else {
            // 否则执行增量同步
            syncIncrementalGoods();
        }
    }

    @Override
    public void syncStaleGoods() {
        // 记录开始日志
        log.info("开始同步失效商品...");
        // 计算结束时间为当前时间
        LocalDateTime endTime = LocalDateTime.now();
        // 计算开始时间为10分钟前
        LocalDateTime startTime = endTime.minusMinutes(10);

        // 格式化开始时间
        String startTimeStr = startTime.format(TIME_FORMATTER);
        // 格式化结束时间
        String endTimeStr = endTime.format(TIME_FORMATTER);

        // 初始化页码
        int pageId = 1;
        // 初始化每页大小
        int pageSize = 100;
        // 标记是否有更多数据
        boolean hasMore = true;

        // 循环拉取所有页
        while (hasMore) {
            try {
                // 调用API获取失效商品
                String response = dtkApiService.getStaleGoodsByTime(pageId, pageSize, startTimeStr, endTimeStr);
                // 解析API响应获取失效商品ID列表
                List<String> staleGoodsIds = parseStaleGoodsResponse(response);

                // 如果结果为空，停止循环
                if (staleGoodsIds.isEmpty()) {
                    hasMore = false;
                } else {
                    // 更新这些商品的状态为0（失效）
                    this.lambdaUpdate()
                            .in(DtkGoods::getGoodsId, staleGoodsIds)
                            .set(DtkGoods::getStatus, 0)
                            .set(DtkGoods::getUpdateTime, LocalDateTime.now())
                            .update();

                    // 记录进度日志
                    log.info("标记失效商品第 {} 页，共 {} 条", pageId, staleGoodsIds.size());
                    // 增加页码
                    pageId++;
                    // 休眠200毫秒避免限流
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                // 记录错误日志
                log.error("同步失效商品第 {} 页失败", pageId, e);
                // 发生异常停止循环
                hasMore = false;
            }
        }
    }

    @Override
    public void syncUpdatedGoods() {
        // 记录开始日志
        log.info("开始同步更新商品...");
        // 计算结束时间为当前时间
        LocalDateTime endTime = LocalDateTime.now();
        // 计算开始时间为5分钟前
        LocalDateTime startTime = endTime.minusMinutes(5);

        // 格式化开始时间
        String startTimeStr = startTime.format(TIME_FORMATTER);
        // 格式化结束时间
        String endTimeStr = endTime.format(TIME_FORMATTER);

        // 初始化页码
        int pageId = 1;
        // 初始化每页大小
        int pageSize = 100;
        // 标记是否有更多数据
        boolean hasMore = true;

        // 循环拉取所有页
        while (hasMore) {
            try {
                // 调用API获取更新商品
                String response = dtkApiService.getNewestGoods(pageId, pageSize, startTimeStr, endTimeStr);
                // 解析API响应获取更新的商品列表
                List<DtkGoods> updatedDtkGoodsList = parseGoodsResponse(response);

                // 如果结果为空，停止循环
                if (updatedDtkGoodsList.isEmpty()) {
                    hasMore = false;
                } else {
                    // 检查并补充缺失的详细信息 (如title为空的情况)
                    for (DtkGoods dtkGoods : updatedDtkGoodsList) {
                        if (dtkGoods.getTitle() == null && dtkGoods.getGoodsId() != null) {
                            fetchAndFillGoodsDetail(dtkGoods);
                        }
                    }

                    // 批量保存或更新商品
                    saveGoodsBatch(updatedDtkGoodsList);
                    // 记录进度日志
                    log.info("更新商品第 {} 页，共 {} 条", pageId, updatedDtkGoodsList.size());
                    // 增加页码
                    pageId++;
                    // 休眠200毫秒避免限流
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                // 记录错误日志
                log.error("同步更新商品第 {} 页失败", pageId, e);
                // 发生异常停止循环
                hasMore = false;
            }
        }
    }

    private void syncFullGoods() {
        // 记录开始全量同步日志
        log.info("开始全量同步商品...");
        // 初始化页码
        int pageId = 1;
        // 初始化每页大小
        int pageSize = 100;
        // 标记是否有更多数据
        boolean hasMore = true;

        // 循环拉取所有页
        while (hasMore) {
            try {
                // 调用API获取商品列表
                String response = dtkApiService.getGoodsList(pageId, pageSize);
                // 解析响应为商品列表
                List<DtkGoods> dtkGoodsList = parseGoodsResponse(response);

                // 如果结果为空，说明没有更多数据
                if (dtkGoodsList.isEmpty()) {
                    hasMore = false;
                    log.info("全量同步结束，第 {} 页无数据", pageId);
                } else {
                    // 批量保存商品
                    saveGoodsBatch(dtkGoodsList);
                    // 记录进度日志
                    log.info("全量同步第 {} 页，共 {} 条", pageId, dtkGoodsList.size());
                    // 增加页码
                    pageId++;
                    // 休眠200毫秒
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                // 记录错误日志
                log.error("全量同步第 {} 页失败", pageId, e);
                // 发生异常停止循环
                hasMore = false;
            }
        }
    }

    private void syncIncrementalGoods() {
        // 记录开始增量同步日志
        log.info("开始增量同步商品...");
        // 计算结束时间
        LocalDateTime endTime = LocalDateTime.now();
        // 计算开始时间（10分钟前）
        LocalDateTime startTime = endTime.minusMinutes(10);

        // 格式化时间
        String startTimeStr = startTime.format(TIME_FORMATTER);
        String endTimeStr = endTime.format(TIME_FORMATTER);

        // 初始化页码
        int pageId = 1;
        // 初始化每页大小
        int pageSize = 100;
        // 标记是否有更多数据
        boolean hasMore = true;

        // 循环拉取所有页
        while (hasMore) {
            try {
                // 调用API拉取时间段内的商品
                String response = dtkApiService.pullGoodsByTime(pageId, pageSize, null, startTimeStr, endTimeStr);
                // 解析响应为商品列表
                List<DtkGoods> dtkGoodsList = parseGoodsResponse(response);

                // 如果结果为空，停止循环
                if (dtkGoodsList.isEmpty()) {
                    hasMore = false;
                } else {
                    // 批量保存商品
                    saveGoodsBatch(dtkGoodsList);
                    // 记录进度日志
                    log.info("增量同步第 {} 页，共 {} 条", pageId, dtkGoodsList.size());
                    // 增加页码
                    pageId++;
                    // 休眠200毫秒
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                // 记录错误日志
                log.error("增量同步第 {} 页失败", pageId, e);
                // 发生异常停止循环
                hasMore = false;
            }
        }
    }

    private void fetchAndFillGoodsDetail(DtkGoods dtkGoods) {
        try {
            // 调用详情API
            String detailResponse = dtkApiService.getGoodsDetails(dtkGoods.getGoodsId());
            if (!StringUtils.hasText(detailResponse))
                return;

            JSONObject json = JSON.parseObject(detailResponse);
            if (json.getIntValue("code") != 0)
                return;

            JSONObject data = json.getJSONObject("data");
            if (data == null)
                return;

            // 补充字段
            if (dtkGoods.getTitle() == null)
                dtkGoods.setTitle(data.getString("title"));
            if (dtkGoods.getDtitle() == null)
                dtkGoods.setDtitle(data.getString("dtitle"));
            if (dtkGoods.getDescription() == null)
                dtkGoods.setDescription(data.getString("desc"));
            if (dtkGoods.getMainPic() == null)
                dtkGoods.setMainPic(data.getString("mainPic"));
            if (dtkGoods.getMarketingMainPic() == null)
                dtkGoods.setMarketingMainPic(data.getString("marketingMainPic"));
            if (dtkGoods.getShopName() == null)
                dtkGoods.setShopName(data.getString("shopName"));
            // ... 可以根据需要补充更多字段

        } catch (Exception e) {
            log.warn("Fetch dtkGoods detail failed for {}: {}", dtkGoods.getGoodsId(), e.getMessage());
        }
    }

    private void saveGoodsBatch(List<DtkGoods> dtkGoodsList) {
        // 如果列表为空直接返回
        if (dtkGoodsList.isEmpty())
            return;

        // Sort by GoodsId to prevent deadlock during concurrent updates
        dtkGoodsList.sort(Comparator.comparing(DtkGoods::getGoodsId));

        // 提取所有商品ID
        List<String> goodsIds = dtkGoodsList.stream()
                .map(DtkGoods::getGoodsId)
                .collect(Collectors.toList());

        // 查询数据库中已存在的商品
        List<DtkGoods> existingGoods = this.list(new LambdaQueryWrapper<DtkGoods>()
                .in(DtkGoods::getGoodsId, goodsIds));

        // 将已存在商品转为Map，key为goodsId，value为DB ID
        Map<String, Long> existingIdMap = existingGoods.stream()
                .collect(Collectors.toMap(DtkGoods::getGoodsId, DtkGoods::getId));

        // 遍历待保存商品列表
        for (DtkGoods dtkGoods : dtkGoodsList) {
            // 如果商品已存在，设置DB ID以进行更新
            if (existingIdMap.containsKey(dtkGoods.getGoodsId())) {
                dtkGoods.setId(existingIdMap.get(dtkGoods.getGoodsId()));
            }
            // 确保状态为有效
            dtkGoods.setStatus(1);
            // 设置更新时间
            dtkGoods.setUpdateTime(LocalDateTime.now());
            // 如果是新商品，设置创建时间
            if (dtkGoods.getId() == null) {
                dtkGoods.setCreateTime(LocalDateTime.now());
            }
        }

        // 批量保存或更新
        boolean success = this.saveOrUpdateBatch(dtkGoodsList);
        if (!success) {
            log.warn("Batch save returned false for {} items", dtkGoodsList.size());
        }
    }

    private List<String> parseStaleGoodsResponse(String response) {
        // 初始化结果列表
        List<String> list = new ArrayList<>();
        // 如果响应为空，返回空列表
        if (!StringUtils.hasText(response))
            return list;

        try {
            // 解析JSON响应
            JSONObject json = JSON.parseObject(response);
            // 检查返回码
            if (json.getIntValue("code") != 0) {
                return list;
            }
            // 获取data对象
            JSONObject data = json.getJSONObject("data");
            if (data == null)
                return list;
            // 获取list数组
            JSONArray goodsArray = data.getJSONArray("list");
            if (goodsArray == null)
                return list;

            // 遍历数组提取ID
            for (int i = 0; i < goodsArray.size(); i++) {
                JSONObject obj = goodsArray.getJSONObject(i);
                list.add(obj.getString("goodsId"));
            }
        } catch (Exception e) {
            // 记录解析异常日志
            log.error("Parse stale goods response error", e);
        }
        return list;
    }

    private List<DtkGoods> parseGoodsResponse(String response) {
        // 初始化结果列表
        List<DtkGoods> list = new ArrayList<>();
        // 如果响应为空，返回空列表
        if (!StringUtils.hasText(response))
            return list;

        try {
            // 解析JSON响应
            JSONObject json = JSON.parseObject(response);
            // 检查返回码
            if (json.getIntValue("code") != 0) {
                // log.warn("API Error: {}", json.getString("msg")); // Reduce noise
                return list;
            }

            // 获取data对象
            JSONObject data = json.getJSONObject("data");
            if (data == null)
                return list;

            // 获取list数组
            JSONArray goodsArray = data.getJSONArray("list");
            if (goodsArray == null)
                return list;

            // 遍历数组解析商品
            for (int i = 0; i < goodsArray.size(); i++) {
                JSONObject obj = goodsArray.getJSONObject(i);

                // 打印第一个商品的Keys用于调试 (只打印一次)
                if (i == 0) {
                    log.info("DEBUG - API Response Keys: {}", obj.keySet());
                }

                DtkGoods dtkGoods = new DtkGoods();

                // 设置商品ID
                dtkGoods.setGoodsId(obj.getString("goodsId"));

                // 设置商品标题
                if (obj.containsKey("title"))
                    dtkGoods.setTitle(obj.getString("title"));
                // 设置短标题
                if (obj.containsKey("dtitle"))
                    dtkGoods.setDtitle(obj.getString("dtitle"));
                // 设置描述
                if (obj.containsKey("desc"))
                    dtkGoods.setDescription(obj.getString("desc"));
                // 设置主图
                if (obj.containsKey("mainPic"))
                    dtkGoods.setMainPic(obj.getString("mainPic"));
                // 设置营销图
                if (obj.containsKey("marketingMainPic"))
                    dtkGoods.setMarketingMainPic(obj.getString("marketingMainPic"));
                // 设置实际价格
                if (obj.containsKey("actualPrice"))
                    dtkGoods.setPrice(obj.getBigDecimal("actualPrice"));
                // 设置原价
                if (obj.containsKey("originalPrice"))
                    dtkGoods.setOriginalPrice(obj.getBigDecimal("originalPrice"));
                // 设置优惠券金额
                if (obj.containsKey("couponPrice"))
                    dtkGoods.setCouponPrice(obj.getBigDecimal("couponPrice"));
                // 设置优惠券链接
                if (obj.containsKey("couponLink"))
                    dtkGoods.setCouponLink(obj.getString("couponLink"));

                try {
                    // 解析优惠券开始时间
                    if (obj.containsKey("couponStartTime")) {
                        String cStart = obj.getString("couponStartTime");
                        if (StringUtils.hasText(cStart))
                            dtkGoods.setCouponStartTime(LocalDateTime.parse(cStart, TIME_FORMATTER));
                    }

                    // 解析优惠券结束时间
                    if (obj.containsKey("couponEndTime")) {
                        String cEnd = obj.getString("couponEndTime");
                        if (StringUtils.hasText(cEnd))
                            dtkGoods.setCouponEndTime(LocalDateTime.parse(cEnd, TIME_FORMATTER));
                    }

                    // 解析活动开始时间
                    if (obj.containsKey("activityStartTime")) {
                        String aStart = obj.getString("activityStartTime");
                        if (StringUtils.hasText(aStart))
                            dtkGoods.setActivityStartTime(LocalDateTime.parse(aStart, TIME_FORMATTER));
                    }

                    // 解析活动结束时间
                    if (obj.containsKey("activityEndTime")) {
                        String aEnd = obj.getString("activityEndTime");
                        if (StringUtils.hasText(aEnd))
                            dtkGoods.setActivityEndTime(LocalDateTime.parse(aEnd, TIME_FORMATTER));
                    }
                } catch (Exception e) {
                    // 记录日期解析错误
                    log.debug("Date parse error: {}", e.getMessage());
                }

                // 设置佣金比率
                if (obj.containsKey("commissionRate"))
                    dtkGoods.setCommissionRate(obj.getBigDecimal("commissionRate"));
                // 设置月销量
                if (obj.containsKey("monthSales"))
                    dtkGoods.setSalesVolume(obj.getInteger("monthSales"));
                // 设置店铺类型
                if (obj.containsKey("shopType"))
                    dtkGoods.setShopType(obj.getInteger("shopType"));
                // 设置店铺名称
                if (obj.containsKey("shopName"))
                    dtkGoods.setShopName(obj.getString("shopName"));
                // 设置店铺等级
                if (obj.containsKey("shopLevel"))
                    dtkGoods.setShopLevel(obj.getInteger("shopLevel"));
                // 设置品牌名称
                if (obj.containsKey("brandName"))
                    dtkGoods.setBrandName(obj.getString("brandName"));
                // 设置活动类型
                if (obj.containsKey("activityType"))
                    dtkGoods.setActivityType(obj.getInteger("activityType"));

                // 添加到列表
                list.add(dtkGoods);
            }
        } catch (Exception e) {
            // 记录解析异常日志
            log.error("Parse goods response error", e);
        }
        return list;
    }
}
