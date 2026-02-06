package com.nei10u.tip.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.goods.normalize.GoodsNormalizeRegistry;
import com.nei10u.tip.goods.normalize.GoodsNormalizeType;
import com.nei10u.tip.service.DtkApiService;
import com.nei10u.tip.service.GoodsCacheService;
import com.nei10u.tip.service.ZtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsCacheServiceImpl implements GoodsCacheService {

    private final DtkApiService dtkApiService;
    private final ZtkApiService ztkApiService;

    private final GoodsNormalizeRegistry goodsNormalizeRegistry;

    private final Object tbTrendCacheLock = new Object();
    private volatile CacheEntry tbTrendFirstPageCache;

    private final Object jdCurrentCacheLock = new Object();
    private volatile CacheEntry jdCurrentFirstPageCache;

    // ==========================
    // 首页最下方列表：本地缓存（全用户共享）
    // - 只缓存第一页
    // - TTL 2分钟
    // - 提前1分钟刷新（每1分钟刷新一次，尽量保持“永远存在”）
    // ==========================
    private static final long HOME_LIST_TTL_MS = 2 * 60 * 1000L;
    private static final long HOME_LIST_REFRESH_MS = 60 * 1000L;
    public static final int HOME_LIST_PAGE_ID = 1;
    public static final int HOME_LIST_PAGE_SIZE = 20;
    public static final String HOME_TB_TREND_TYPE = "1";

    private static final class CacheEntry {
        final JSONObject value;
        final long fetchedAtMs;

        CacheEntry(JSONObject value, long fetchedAtMs) {
            this.value = value;
            this.fetchedAtMs = fetchedAtMs;
        }
    }

    /**
     * 首页底部列表缓存：防止“首次拉取失败/空列表”污染缓存。
     *
     * 约定：只有当列表非空时才允许写入缓存；缓存命中但为空时，视为 miss，继续回源请求。
     */
    private static boolean isEffectivelyEmptyList(JSONObject raw) {
        if (raw == null) return true;

        try {
            // 1) 标准结构：{data:{list:[...]}}
            JSONObject data = raw.getJSONObject("data");
            if (data != null) {
                JSONArray list = data.getJSONArray("list");
                if (list != null) return list.isEmpty();
            }

            // 2) 归一化结构：{list:[...]}
            JSONArray list = raw.getJSONArray("list");
            if (list != null) return list.isEmpty();
        } catch (Exception ignore) {
            // ignore
        }
        // 无 list 字段也视为“空”
        return true;
    }

    @Override
    public JSONObject getOrLoadTbTrendFirstPage() {
        final long now = System.currentTimeMillis();
        CacheEntry e = tbTrendFirstPageCache;
        if (e != null && (now - e.fetchedAtMs) < HOME_LIST_TTL_MS && !isEffectivelyEmptyList(e.value)) {
            return e.value;
        }
        synchronized (tbTrendCacheLock) {
            e = tbTrendFirstPageCache;
            if (e != null && (now - e.fetchedAtMs) < HOME_LIST_TTL_MS && !isEffectivelyEmptyList(e.value)) {
                return e.value;
            }
            JSONObject fresh = fetchTbTrend(HOME_TB_TREND_TYPE, "", HOME_LIST_PAGE_ID, HOME_LIST_PAGE_SIZE);
            if (!isEffectivelyEmptyList(fresh)) {
                tbTrendFirstPageCache = new CacheEntry(fresh, now);
                return fresh;
            }
            // refresh 失败/空：返回旧值（仅当旧值非空），否则直接返回 fresh（空结构）
            return (e != null && !isEffectivelyEmptyList(e.value)) ? e.value : (fresh == null ? new JSONObject() : fresh);
        }
    }

    public JSONObject getOrLoadJdCurrentFirstPage() {
        final long now = System.currentTimeMillis();
        CacheEntry e = jdCurrentFirstPageCache;
        if (e != null && (now - e.fetchedAtMs) < HOME_LIST_TTL_MS && !isEffectivelyEmptyList(e.value)) {
            return e.value;
        }
        synchronized (jdCurrentCacheLock) {
            e = jdCurrentFirstPageCache;
            if (e != null && (now - e.fetchedAtMs) < HOME_LIST_TTL_MS && !isEffectivelyEmptyList(e.value)) {
                return e.value;
            }
            JSONObject fresh = fetchJdCurrentTrend(0, HOME_LIST_PAGE_ID, HOME_LIST_PAGE_SIZE);
            if (!isEffectivelyEmptyList(fresh)) {
                jdCurrentFirstPageCache = new CacheEntry(fresh, now);
                return fresh;
            }
            return (e != null && !isEffectivelyEmptyList(e.value))
                    ? e.value
                    : (fresh == null ? new JSONObject().fluentPut("list", new JSONArray()) : fresh);
        }
    }

    /**
     * 后台刷新：每分钟刷新一次（提前1分钟更新，理论上缓存永远存在）
     */
    @Scheduled(initialDelay = 10_000L, fixedDelay = HOME_LIST_REFRESH_MS)
    public void refreshHomeBottomListCache() {
        final long now = System.currentTimeMillis();

        CacheEntry tb = tbTrendFirstPageCache;
        if (tb == null || (now - tb.fetchedAtMs) >= HOME_LIST_REFRESH_MS) {
            try {
                JSONObject fresh = fetchTbTrend(HOME_TB_TREND_TYPE, "", HOME_LIST_PAGE_ID, HOME_LIST_PAGE_SIZE);
                if (!isEffectivelyEmptyList(fresh)) {
                    tbTrendFirstPageCache = new CacheEntry(fresh, now);
                }
            } catch (Exception ignore) {
            }
        }

        CacheEntry jd = jdCurrentFirstPageCache;
        if (jd == null || (now - jd.fetchedAtMs) >= HOME_LIST_REFRESH_MS) {
            try {
                JSONObject fresh = fetchJdCurrentTrend(0, HOME_LIST_PAGE_ID, HOME_LIST_PAGE_SIZE);
                if (!isEffectivelyEmptyList(fresh)) {
                    jdCurrentFirstPageCache = new CacheEntry(fresh, now);
                }
            } catch (Exception ignore) {
            }
        }
    }

    public JSONObject fetchTbTrend(String type, String cid, int pageId, int pageSize) {
        String response = dtkApiService.getRankingList(type, cid, pageId, pageSize);
        if (!StringUtils.hasText(response)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(response);
        } catch (Exception ignore) {
            return new JSONObject();
        }
    }

    public JSONObject fetchJdCurrentTrend(int cid, int pageId, int pageSize) {
        String response = ztkApiService.getRealTimeHotList("new", cid, pageId, pageSize);
        if (!StringUtils.hasText(response)) {
            return new JSONObject()
                    .fluentPut("list", new JSONArray())
                    .fluentPut("raw", new JSONObject())
                    .fluentPut("pageId", pageId)
                    .fluentPut("pageSize", pageSize);
        }

        JSONObject raw;
        try {
            raw = JSON.parseObject(response);
        } catch (Exception e) {
            return new JSONObject()
                    .fluentPut("list", new JSONArray())
                    .fluentPut("raw", new JSONObject())
                    .fluentPut("pageId", pageId)
                    .fluentPut("pageSize", pageSize);
        }

        return goodsNormalizeRegistry
                .get(GoodsNormalizeType.ZTK_SHISHI)
                .normalize(raw, pageId, pageSize);
    }

}
