package com.nei10u.tip.goods.normalize;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.goods.util.GoodsFieldUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 好京客：唯品会商品查询（vip/goodsquery）-> 统一 Goods(list) 结构
 *
 * 典型返回：
 * {
 *   "data": { "goodsInfoList": [...], "total": 44 },
 *   "message": "success",
 *   "status_code": 200
 * }
 */
@Component
public class HjkVipGoodsNormalizer implements GoodsNormalizer {
    @Override
    public GoodsNormalizeType type() {
        return GoodsNormalizeType.HJK_VIP;
    }

    @Override
    public JSONObject normalize(JSONObject raw, int pageId, int pageSize) {
        JSONObject out = new JSONObject();
        out.put("pageId", String.valueOf(Math.max(pageId, 1)));
        out.put("pageSize", pageSize <= 0 ? 20 : pageSize);

        if (raw == null) {
            out.put("list", new JSONArray());
            out.put("totalNum", 0);
            out.put("raw", new JSONObject());
            return out;
        }

        JSONObject data = raw.getJSONObject("data");
        if (data == null) {
            out.put("list", new JSONArray());
            out.put("totalNum", 0);
            out.put("raw", raw);
            return out;
        }

        JSONArray goodsInfoList = data.getJSONArray("goodsInfoList");
        if (goodsInfoList == null) goodsInfoList = new JSONArray();

        JSONArray list = new JSONArray();
        for (int i = 0; i < goodsInfoList.size(); i++) {
            JSONObject it = goodsInfoList.getJSONObject(i);
            if (it == null) continue;

            JSONObject g = new JSONObject();
            g.put("platform", "vip");
            g.put("goodsId", GoodsFieldUtils.firstNonBlank(it, "goodsId", "goods_id", "id"));
            g.put("title", GoodsFieldUtils.firstNonBlank(it, "goodsName", "title", "goods_name"));

            // 图片：thumb 优先，其次 mainPicture
            String pic = GoodsFieldUtils.firstNonBlank(it, "goodsThumbUrl", "goodsMainPicture", "goods_thumb_url", "mainPic");
            if (StringUtils.hasText(pic)) g.put("mainPic", pic);

            // 价格：marketPrice/vipPrice + couponInfo.fav（若存在）
            BigDecimal marketPrice = GoodsFieldUtils.safeBigDecimal(it.get("marketPrice"));
            BigDecimal vipPrice = GoodsFieldUtils.safeBigDecimal(it.get("vipPrice"));

            BigDecimal coupon = null;
            JSONObject couponInfo = it.getJSONObject("couponInfo");
            if (couponInfo != null) {
                // 文档示例：fav = 红包/券金额
                coupon = GoodsFieldUtils.safeBigDecimal(couponInfo.get("fav"));
                if (coupon != null) g.put("couponPrice", coupon);

                // 时间：优先 useBeginTime/useEndTime（用户实际可用窗口）
                Object start = couponInfo.get("useBeginTime");
                Object end = couponInfo.get("useEndTime");
                if (start != null) g.put("couponStartTime", start);
                if (end != null) g.put("couponEndTime", end);
            }

            BigDecimal actual = vipPrice;
            if (actual != null && coupon != null) {
                actual = actual.subtract(coupon);
                if (actual.compareTo(BigDecimal.ZERO) < 0) actual = BigDecimal.ZERO;
            }

            if (marketPrice != null) g.put("originalPrice", marketPrice);
            if (actual != null) g.put("actualPrice", actual);
            if (vipPrice != null && actual == null) g.put("actualPrice", vipPrice);

            // 佣金
            BigDecimal commissionRate = GoodsFieldUtils.safeBigDecimal(it.get("commissionRate"));
            BigDecimal commission = GoodsFieldUtils.safeBigDecimal(it.get("commission"));
            if (commissionRate != null) g.put("commissionRate", commissionRate);
            if (commission != null) g.put("estimateAmount", commission);

            // 店铺名称
            JSONObject storeInfo = it.getJSONObject("storeInfo");
            String shopName = storeInfo == null ? null : storeInfo.getString("storeName");
            if (!StringUtils.hasText(shopName)) shopName = it.getString("brandName");
            if (StringUtils.hasText(shopName)) g.put("shopName", shopName);

            // 海淘标识
            Integer haitao = GoodsFieldUtils.safeInt(it.get("haiTao"));
            if (haitao != null) g.put("haitao", haitao);

            // 额外字段（不影响 App 解析）
            String destUrl = it.getString("destUrl");
            if (StringUtils.hasText(destUrl)) g.put("destUrl", destUrl);

            list.add(g);
        }

        Long total = null;
        try {
            total = data.getLong("total");
        } catch (Exception ignore) {
        }

        out.put("list", list);
        out.put("totalNum", (total != null && total > 0) ? total : list.size());
        out.put("raw", raw);
        return out;
    }
}

