package com.nei10u.tip.goods.processor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.goods.util.GoodsFieldUtils;
import com.nei10u.tip.service.HjkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 拼多多（PDD）平台商品处理器：负责接入好京客（蚂蚁星球）PDD 接口，并适配 App 的分页与字段模型。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PddGoodsProcessor {

    private final HjkApiService hjkApiService;

    /**
     * 拼多多商品列表
     */
    public JSONObject getPddGoodsList(String keyword, int pageId, int pageSize) {
        int page = Math.max(pageId, 1);
        int size = pageSize <= 0 ? 20 : pageSize;
        String resp = hjkApiService.pddGoodsList(keyword, page, size);
        return normalizeHjkListResponse(resp, page, size);
    }

    /**
     * 今日爆款（支持分页：offset/limit）
     */
    public JSONObject getPddExplosiveGoodList(int pageId, int pageSize) {
        int page = Math.max(pageId, 1);
        int size = pageSize <= 0 ? 20 : pageSize;

        int offset = (page - 1) * size;
        // 默认频道：今日爆款（1）
        String resp = hjkApiService.pddGetRecommendGoods(offset, size, 1, 0);
        return normalizeHjkListResponse(resp, page, size);
    }

    /**
     * 详情：优先 goods_id，其次 goods_sign（两者都传时，以 goods_id 为准）
     */
    public JSONObject getPddGoodsDetail(String goodsIdOrSign) {
        if (!StringUtils.hasText(goodsIdOrSign))
            return new JSONObject();

        // 尝试按数字 goods_id 走
        Map<String, Object> params;
        if (goodsIdOrSign.trim().matches("^\\d+$")) {
            params = Map.of("goods_id", goodsIdOrSign.trim(), "isunion", "0");
        } else {
            params = Map.of("goods_sign", goodsIdOrSign.trim(), "isunion", "0");
        }

        String resp = hjkApiService.pddGoodsDetail(params);
        if (!StringUtils.hasText(resp))
            return new JSONObject();

        JSONObject raw;
        try {
            raw = JSON.parseObject(resp);
        } catch (Exception e) {
            return new JSONObject().fluentPut("raw", resp);
        }

        // 常见：{status_code,message,data:{...}} 或 data 为数组/字符串
        Object dataObj = raw.get("data");
        JSONObject item = null;
        if (dataObj instanceof JSONObject jo) {
            item = jo;
        } else if (dataObj instanceof JSONArray arr && !arr.isEmpty()) {
            item = arr.getJSONObject(0);
        }
        if (item == null)
            return new JSONObject().fluentPut("raw", raw);

        JSONObject normalized = normalizePddGoodsLike(item);
        if (normalized == null)
            normalized = new JSONObject();
        normalized.put("raw", raw);
        return normalized;
    }

    /**
     * 将好京客列表响应尽可能归一化为：{list,totalNum,pageId,pageSize,raw}
     */
    private JSONObject normalizeHjkListResponse(String resp, int page, int pageSize) {
        JSONObject out = new JSONObject();
        out.put("pageId", String.valueOf(page));
        out.put("pageSize", pageSize);

        if (!StringUtils.hasText(resp)) {
            out.put("list", new JSONArray());
            out.put("totalNum", 0);
            out.put("raw", new JSONObject());
            return out;
        }

        JSONObject raw;
        try {
            raw = JSON.parseObject(resp);
        } catch (Exception e) {
            out.put("list", new JSONArray());
            out.put("totalNum", 0);
            out.put("raw", resp);
            return out;
        }

        JSONArray dataArr = null;
        Long total = null;
        Object dataObj = raw.get("data");
        if (dataObj instanceof JSONArray arr) {
            dataArr = arr;
        } else if (dataObj instanceof JSONObject jo) {
            // 兼容：goods_list / list / result.goods_list
            if (jo.containsKey("goods_list"))
                dataArr = jo.getJSONArray("goods_list");
            if (dataArr == null)
                dataArr = jo.getJSONArray("list");
            if (dataArr == null) {
                JSONObject result = jo.getJSONObject("result");
                if (result != null)
                    dataArr = result.getJSONArray("goods_list");
            }
            // 示例：{data:{total:2889, goods_list:[...]}}
            if (jo.containsKey("total")) {
                try {
                    total = jo.getLong("total");
                } catch (Exception ignore) {
                }
            }
        }
        if (dataArr == null)
            dataArr = new JSONArray();

        JSONArray list = new JSONArray();
        for (int i = 0; i < dataArr.size(); i++) {
            Object item = dataArr.get(i);
            if (!(item instanceof JSONObject jo))
                continue;
            JSONObject g = normalizePddGoodsLike(jo);
            if (g != null)
                list.add(g);
        }

        out.put("list", list);
        out.put("totalNum", (total != null && total > 0) ? total : list.size());
        out.put("raw", raw);
        return out;
    }

    /**
     * 把好京客/拼多多 item 尽可能归一化为 App 的 Goods 模型字段（多 key 兜底）。
     */
    private JSONObject normalizePddGoodsLike(JSONObject jo) {
        if (jo == null)
            return null;

        JSONObject g = new JSONObject();
        g.put("platform", "pdd");

        String goodsId = GoodsFieldUtils.firstNonBlank(jo, "goods_id", "goodsId", "itemid", "item_id", "id",
                "goods_sign");
        if (StringUtils.hasText(goodsId))
            g.put("goodsId", goodsId);

        g.put("title",
                GoodsFieldUtils.firstNonBlank(jo, "goods_name", "title", "itemtitle", "goods_title", "itemshorttitle"));
        g.put("desc", GoodsFieldUtils.firstNonBlank(jo, "goods_desc", "desc", "itemdesc", "goods_info"));

        // 主图：好京客字段为 picurl
        String pic = GoodsFieldUtils.firstNonBlank(jo, "picurl", "goods_thumbnail_url", "goods_image_url", "pict_url",
                "pic",
                "itempic", "goods_picture", "mainPic");
        if (StringUtils.hasText(pic))
            g.put("mainPic", pic);

        // 轮播图：
        // - 好京客：picurls (array)
        // - 其它：goods_gallery_urls (array/string)
        List<String> carouselPics = new ArrayList<>();
        Object hjkPics = jo.get("picurls");
        if (hjkPics instanceof JSONArray arr) {
            for (int i = 0; i < arr.size(); i++) {
                String u = arr.getString(i);
                if (StringUtils.hasText(u))
                    carouselPics.add(u.trim());
            }
        }
        Object gallery = jo.get("goods_gallery_urls");
        if (gallery instanceof JSONArray gArr) {
            for (int i = 0; i < gArr.size(); i++) {
                String u = gArr.getString(i);
                if (StringUtils.hasText(u))
                    carouselPics.add(u.trim());
            }
        } else if (gallery instanceof String s) {
            String[] parts = s.split("[,|]");
            for (String p : parts) {
                if (StringUtils.hasText(p))
                    carouselPics.add(p.trim());
            }
        }
        if (!carouselPics.isEmpty()) {
            g.put("carouselPics", carouselPics);
            if (!StringUtils.hasText(g.getString("mainPic")))
                g.put("mainPic", carouselPics.get(0));
        }

        // =========================
        // 价格/券字段（优先适配好京客）
        // 好京客示例：
        // - price: 原价
        // - price_pg: 拼购价
        // - price_after: 券后价（字符串）
        // - discount: 券金额
        // =========================
        BigDecimal original = GoodsFieldUtils.safeBigDecimal(jo.get("price"));
        BigDecimal groupPrice = GoodsFieldUtils.safeBigDecimal(jo.get("price_pg"));
        BigDecimal actual = GoodsFieldUtils.safeBigDecimal(jo.get("price_after"));
        BigDecimal coupon = GoodsFieldUtils.safeBigDecimal(jo.get("discount"));

        // 兜底：若未给券后价，尝试 groupPrice - coupon
        if (actual == null && groupPrice != null && coupon != null)
            actual = groupPrice.subtract(coupon);
        // 兜底：若券为空但给了 groupPrice 与 actual，尝试反推 coupon
        if (coupon == null && groupPrice != null && actual != null) {
            BigDecimal diff = groupPrice.subtract(actual);
            if (diff.compareTo(BigDecimal.ZERO) > 0)
                coupon = diff;
        }
        // 兜底：没有券后价就用拼购价，没有拼购价就用原价
        if (actual == null)
            actual = groupPrice != null ? groupPrice : original;

        // 兜底：若好京客原价为空，尝试官方字段
        if (original == null) {
            original = GoodsFieldUtils.parseMoneyMaybeCent(jo.get("min_normal_price"));
            if (original == null)
                original = GoodsFieldUtils.parseMoneyMaybeCent(jo.get("normal_price"));
            if (original == null)
                original = GoodsFieldUtils.safeBigDecimal(jo.get("goods_yprice"));
        }
        // 兜底：若好京客券为空，尝试官方字段
        if (coupon == null) {
            coupon = GoodsFieldUtils.parseMoneyMaybeCent(jo.get("coupon_discount"));
            if (coupon == null)
                coupon = GoodsFieldUtils.safeBigDecimal(jo.get("coupon_money"));
        }

        g.put("originalPrice", original);
        g.put("couponPrice", coupon);
        g.put("actualPrice", actual);

        // 佣金：好京客字段 commission/commissionshare
        BigDecimal commission = GoodsFieldUtils.safeBigDecimal(jo.get("commission"));
        if (commission != null)
            g.put("estimateAmount", commission);
        BigDecimal commissionRate = GoodsFieldUtils.safeBigDecimal(jo.get("commissionshare"));
        if (commissionRate == null) {
            // predict_promotion_rate 可能是“万分比/百分比”之一，兜底走转换
            commissionRate = GoodsFieldUtils.parseRatePermyriadToPercent(jo.get("predict_promotion_rate"));
        }
        if (commissionRate != null)
            g.put("commissionRate", commissionRate);

        // 销量：好京客 sales 可能是 “4.3万+”
        Integer monthSales = parseSalesLoose(jo.getString("sales"));
        if (monthSales != null)
            g.put("monthSales", monthSales);
        return g;
    }

    private static Integer parseSalesLoose(String raw) {
        if (!StringUtils.hasText(raw))
            return null;
        String s = raw.trim().replace("+", "").replace(" ", "");
        try {
            if (s.matches("^\\d+$"))
                return Integer.parseInt(s);
            if (s.contains("万")) {
                double v = Double.parseDouble(s.replace("万", ""));
                return (int) Math.round(v * 10000);
            }
            if (s.contains("千")) {
                double v = Double.parseDouble(s.replace("千", ""));
                return (int) Math.round(v * 1000);
            }
        } catch (Exception ignore) {
        }
        return null;
    }
}
