package com.nei10u.tip.goods.processor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.goods.normalize.GoodsNormalizeRegistry;
import com.nei10u.tip.goods.normalize.GoodsNormalizeType;
import com.nei10u.tip.service.HjkApiService;
import com.nei10u.tip.service.ZtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 唯品会（VIP）平台商品处理器：
 * - 优先接入好京客（蚂蚁星球）vip/goodsquery，并归一化为 App 可消费结构
 * - 如 HJK 调用失败，兜底走折淘客 ZTK（保持兼容）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipGoodsProcessor {

    private final HjkApiService hjkApiService;
    private final GoodsNormalizeRegistry goodsNormalizeRegistry;

    public JSONObject getVipHighCommissionGoodsList(int channelType, int sourceType, int pageId, int pageSize) {
        int page = Math.max(pageId, 1);
        int size = pageSize <= 0 ? 20 : pageSize;

        // 1) 优先：HJK 唯品会
        try {
            String resp = hjkApiService.vipChannelGoodsQuery(channelType, sourceType, page, size);
            if (StringUtils.hasText(resp)) {
                JSONObject raw = JSON.parseObject(resp);
                return goodsNormalizeRegistry.get(GoodsNormalizeType.HJK_VIP).normalize(raw, page, size);
            }
        } catch (Exception e) {
            log.warn("HJK VIP goodsquery failed, fallback to ZTK. page={}, size={}", page, size, e);
        }
        return new JSONObject().fluentPut("list", new JSONArray()).fluentPut("totalNum", 0)
                .fluentPut("pageId", String.valueOf(page)).fluentPut("pageSize", size)
                .fluentPut("raw", new JSONObject());
    }

    public JSONObject getVipExplosiveGoodsList(int channelType, int sourceType, int pageId, int pageSize) {
        int page = Math.max(pageId, 1);
        int size = pageSize <= 0 ? 20 : pageSize;
        // 1) 优先：HJK 唯品会
        try {
            String resp = hjkApiService.vipChannelGoodsQuery(channelType, sourceType, page, size);
            if (StringUtils.hasText(resp)) {
                JSONObject raw = JSON.parseObject(resp);
                return goodsNormalizeRegistry.get(GoodsNormalizeType.HJK_VIP).normalize(raw, page, size);
            }
        } catch (Exception e) {
            log.warn("HJK VIP goodsquery failed, fallback to ZTK. page={}, size={}", page, size, e);
        }
        return new JSONObject().fluentPut("list", new JSONArray()).fluentPut("totalNum", 0)
                .fluentPut("pageId", String.valueOf(page)).fluentPut("pageSize", size)
                .fluentPut("raw", new JSONObject());
    }

    /**
     * ZTK 唯品会列表 -> 统一 {list,totalNum,pageId,pageSize,raw}
     * <p>
     * ZTK 返回一般为：
     * - { "content": [ ... ] } 或 { "data": { "content": [ ... ] } }
     */
    private JSONObject normalizeZtkVipList(JSONObject raw, int pageId, int pageSize) {
        com.alibaba.fastjson2.JSONArray content = raw == null ? null : raw.getJSONArray("content");
        if (content == null && raw != null) {
            JSONObject data = raw.getJSONObject("data");
            if (data != null)
                content = data.getJSONArray("content");
        }
        if (content == null)
            content = new com.alibaba.fastjson2.JSONArray();

        com.alibaba.fastjson2.JSONArray list = new com.alibaba.fastjson2.JSONArray();
        for (int i = 0; i < content.size(); i++) {
            JSONObject it = content.getJSONObject(i);
            if (it == null)
                continue;

            JSONObject g = new JSONObject();
            g.put("platform", "vip");

            g.put("goodsId", it.getString("tao_id"));
            g.put("title", it.getString("title"));
            g.put("desc", it.getString("jianjie"));
            g.put("mainPic", it.getString("pict_url"));

            g.put("actualPrice", it.getBigDecimal("quanhou_jiage"));
            g.put("originalPrice", it.getBigDecimal("size"));
            g.put("couponPrice", it.getBigDecimal("coupon_info_money"));

            g.put("commissionRate", it.getBigDecimal("tkrate3"));
            g.put("estimateAmount", it.getBigDecimal("tkfee3"));
            g.put("monthSales", it.getInteger("volume"));

            String shop = it.getString("shop_title");
            if (!StringUtils.hasText(shop))
                shop = it.getString("nick");
            g.put("shopName", shop);

            g.put("couponStartTime", it.getString("coupon_start_time"));
            g.put("couponEndTime", it.getString("coupon_end_time"));

            list.add(g);
        }

        return new JSONObject()
                .fluentPut("list", list)
                .fluentPut("totalNum", list.size())
                .fluentPut("raw", raw)
                .fluentPut("pageId", String.valueOf(pageId))
                .fluentPut("pageSize", pageSize);
    }

}
