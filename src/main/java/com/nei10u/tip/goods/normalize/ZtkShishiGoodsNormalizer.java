package com.nei10u.tip.goods.normalize;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 折淘客实时人气榜（api_shishi.ashx） -> 统一 Goods(list) 结构
 */
@Component
public class ZtkShishiGoodsNormalizer implements GoodsNormalizer {
    @Override
    public GoodsNormalizeType type() {
        return GoodsNormalizeType.ZTK_SHISHI;
    }

    @Override
    public JSONObject normalize(JSONObject raw, int pageId, int pageSize) {
        JSONArray content = raw == null ? null : raw.getJSONArray("content");
        JSONArray list = new JSONArray();

        if (content != null) {
            for (int i = 0; i < content.size(); i++) {
                JSONObject it = content.getJSONObject(i);
                if (it == null) continue;

                JSONObject g = new JSONObject();
                // Goods.fromJson expects:
                // goodsId/title/desc/mainPic/actualPrice/originalPrice/couponPrice/commissionRate/monthSales/shopName/platform...
                g.put("goodsId", it.getString("tao_id"));
                g.put("title", it.getString("title"));
                g.put("desc", it.getString("jianjie"));
                g.put("mainPic", it.getString("pict_url"));

                g.put("actualPrice", toBigDecimalOrNull(it.getString("quanhou_jiage")));
                g.put("originalPrice", toBigDecimalOrNull(it.getString("size")));
                g.put("couponPrice", toBigDecimalOrNull(it.getString("coupon_info_money")));
                g.put("commissionRate", toBigDecimalOrNull(it.getString("tkrate3")));

                Integer vol = toIntOrNull(it.getString("volume"));
                if (vol != null) g.put("monthSales", vol);

                String shop = it.getString("shop_title");
                if (!StringUtils.hasText(shop)) shop = it.getString("nick");
                g.put("shopName", shop);

                g.put("couponStartTime", it.getString("coupon_start_time"));
                g.put("couponEndTime", it.getString("coupon_end_time"));
                // 该 normalizer 当前用于 GoodsCacheServiceImpl.fetchJdCurrentTrend（ZTK 实时榜单）
                g.put("platform", "jd");

                list.add(g);
            }
        }

        return new JSONObject()
                .fluentPut("list", list)
                .fluentPut("totalNum", list.size())
                .fluentPut("raw", raw)
                .fluentPut("pageId", pageId)
                .fluentPut("pageSize", pageSize);
    }

    private static BigDecimal toBigDecimalOrNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer toIntOrNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            String t = s.trim();
            if (t.contains(".")) t = t.substring(0, t.indexOf('.'));
            return Integer.parseInt(t);
        } catch (Exception e) {
            return null;
        }
    }
}


