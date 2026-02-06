package com.nei10u.tip.goods.processor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.goods.util.GoodsFieldUtils;
import com.nei10u.tip.service.ZtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * 抖音（DY）平台处理器：
 * - 商品搜索（用于返利页列表）
 * - 商品转链（用于分享/购买）
 *
 * 当前接入：折淘客 ZTK 抖音相关接口（open_douyin_*）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DyGoodsProcessor {

    private final ZtkApiService ztkApiService;

    /**
     * 抖音商品搜索（默认仅返回可分销商品）。
     */
    public JSONObject search(String title, int pageId, int pageSize) {
        int page = Math.max(pageId, 1);
        int size = pageSize <= 0 ? 20 : Math.min(pageSize, 20);

        String resp = ztkApiService.dyProductSearch(title, page, size);
        JSONObject out = new JSONObject();
        out.put("pageId", String.valueOf(page));
        out.put("pageSize", size);

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

        // 兼容：data.products / data.data.products / products
        JSONObject data = raw.getJSONObject("data");
        JSONObject inner = data == null ? null : data.getJSONObject("data");
        JSONArray products = null;
        if (inner != null) products = inner.getJSONArray("products");
        if (products == null && data != null) products = data.getJSONArray("products");
        if (products == null) products = raw.getJSONArray("products");
        if (products == null) products = new JSONArray();

        JSONArray list = new JSONArray();
        for (int i = 0; i < products.size(); i++) {
            JSONObject p = products.getJSONObject(i);
            if (p == null) continue;

            JSONObject g = new JSONObject();
            g.put("platform", "dy");

            String goodsId = GoodsFieldUtils.firstNonBlank(p, "product_id", "productId", "id");
            g.put("goodsId", goodsId);
            g.put("title", GoodsFieldUtils.firstNonBlank(p, "title", "product_name", "name"));
            g.put("mainPic", GoodsFieldUtils.firstNonBlank(p, "cover", "img", "image", "mainPic"));

            // 抖音价格字段通常为“分”，这里尽量做自动识别：
            // - parseMoneyMaybeCent 内部会对 >= 1000 的数按“分”处理（与 PDD 兼容逻辑一致）
            BigDecimal price = GoodsFieldUtils.parseMoneyMaybeCent(p.get("price"));
            BigDecimal couponPrice = GoodsFieldUtils.parseMoneyMaybeCent(p.get("coupon_price"));
            BigDecimal cosFee = GoodsFieldUtils.parseMoneyMaybeCent(p.get("cos_fee"));
            BigDecimal cosRatio = GoodsFieldUtils.safeBigDecimal(p.get("cos_ratio"));

            if (couponPrice != null && couponPrice.compareTo(BigDecimal.ZERO) > 0) {
                g.put("actualPrice", couponPrice);
                g.put("couponPrice", price != null ? price.subtract(couponPrice) : null);
                g.put("originalPrice", price);
            } else {
                g.put("actualPrice", price);
                g.put("originalPrice", price);
                g.put("couponPrice", BigDecimal.ZERO);
            }

            if (cosFee != null) g.put("estimateAmount", cosFee);
            if (cosRatio != null) g.put("commissionRate", cosRatio);
            g.put("monthSales", GoodsFieldUtils.safeInt(p.get("sales")));

            list.add(g);
        }

        // total：尽可能从 data.total 透出，否则用 list.size()
        Long total = null;
        if (inner != null) total = inner.getLong("total");
        if (total == null && data != null) total = data.getLong("total");
        if (total == null) {
            try {
                total = raw.getLong("total");
            } catch (Exception ignore) {
            }
        }

        out.put("list", list);
        out.put("totalNum", (total != null && total > 0) ? total : list.size());
        out.put("raw", raw);
        return out;
    }

    /**
     * 视频(抖货)商品列表（折京客：api_videos.ashx）
     * <p>
     * 作为“抖音购物返利页”CMS 的视频列表数据源：按统一 goods 字段输出，供 App 复用现有组件渲染。
     */
    public JSONObject videoList(Integer cid, String saleNumStart, String sort, int pageId, int pageSize) {
        int page = Math.max(pageId, 1);
        int size = pageSize <= 0 ? 20 : Math.min(pageSize, 50);

        String resp = ztkApiService.dyVideoGoodsList(cid, saleNumStart, sort, page, size, false);

        JSONObject out = new JSONObject();
        out.put("pageId", String.valueOf(page));
        out.put("pageSize", size);

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

        JSONArray content = raw.getJSONArray("content");
        if (content == null) content = raw.getJSONArray("data");
        if (content == null) content = new JSONArray();

        JSONArray list = new JSONArray();
        for (int i = 0; i < content.size(); i++) {
            JSONObject p = content.getJSONObject(i);
            if (p == null) continue;

            JSONObject g = new JSONObject();
            // 关键：保持 platform=dy，避免 CMS/前端对“抖音页”平台判断分裂
            g.put("platform", "dy");

            // api_videos 的核心字段（参考折京客文档示例）
            g.put("goodsId", GoodsFieldUtils.firstNonBlank(p, "item_url", "tao_id", "code"));
            g.put("title", GoodsFieldUtils.firstNonBlank(p, "title", "tao_title"));
            g.put("desc", GoodsFieldUtils.firstNonBlank(p, "jianjie"));
            g.put("mainPic", GoodsFieldUtils.firstNonBlank(p, "pict_url", "white_image"));
            g.put("shopName", GoodsFieldUtils.firstNonBlank(p, "shop_title", "nick"));
            g.put("itemUrl", GoodsFieldUtils.firstNonBlank(p, "item_url"));
            g.put("videoUrl", GoodsFieldUtils.firstNonBlank(p, "zhibo_url"));

            // 价格
            BigDecimal originalPrice = GoodsFieldUtils.safeBigDecimal(p.get("size")); // 折扣价
            BigDecimal actualPrice = GoodsFieldUtils.safeBigDecimal(p.get("quanhou_jiage")); // 券后价
            BigDecimal couponAmount = GoodsFieldUtils.safeBigDecimal(p.get("coupon_info_money")); // 券面额

            if (actualPrice != null) g.put("actualPrice", actualPrice);
            if (originalPrice != null) g.put("originalPrice", originalPrice);
            if (couponAmount != null) g.put("couponPrice", couponAmount);

            // 优惠券有效期（字段名保持 snake_case，Flutter 端已兼容）
            String cStart = GoodsFieldUtils.firstNonBlank(p, "coupon_start_time");
            String cEnd = GoodsFieldUtils.firstNonBlank(p, "coupon_end_time");
            if (StringUtils.hasText(cStart)) g.put("coupon_start_time", cStart);
            if (StringUtils.hasText(cEnd)) g.put("coupon_end_time", cEnd);

            // 销量
            Integer monthSales = GoodsFieldUtils.safeInt(p.get("volume"));
            if (monthSales != null) g.put("monthSales", monthSales);

            // 佣金
            BigDecimal commissionRate = GoodsFieldUtils.safeBigDecimal(p.get("tkrate3"));
            BigDecimal estimateAmount = GoodsFieldUtils.safeBigDecimal(p.get("tkfee3"));
            if (commissionRate != null) g.put("commissionRate", commissionRate);
            if (estimateAmount != null) g.put("estimateAmount", estimateAmount);

            list.add(g);
        }

        out.put("list", list);
        out.put("totalNum", list.size());
        out.put("raw", raw);
        return out;
    }

    /**
     * 抖音商品转链：把上游字段尽量映射到 App 统一解析字段。
     *
     * App（Flutter）侧的 convertLink 解析优先读取：
     * - click_url / shortUrl / url / itemLink / tpwd
     */
    public JSONObject convert(String productUrlOrCommand, String externalInfo) {
        String input = productUrlOrCommand == null ? "" : productUrlOrCommand.trim();

        // 兼容：当 “抖音页视频列表” 直接把 item_url 作为 goodsId 透出时，
        // 这里不走抖音上游转链（会校验域名/口令），直接返回 click_url=原链接，保证前端按钮可用。
        if (StringUtils.hasText(input) && (input.startsWith("http://") || input.startsWith("https://"))) {
            String lower = input.toLowerCase(Locale.ROOT);
            boolean looksLikeDy = lower.contains("jinritemai.com")
                    || lower.contains("douyin.com")
                    || lower.contains("iesdouyin.com")
                    || lower.contains("aweme.com");
            if (!looksLikeDy) {
                JSONObject out = new JSONObject();
                out.put("click_url", input);
                out.put("raw", new JSONObject().fluentPut("passthrough", true));
                return out;
            }
        }

        // 抖音转链入参为 product_url：支持 URL/口令/短链；不保证支持纯数字 id。
        // 这里对“纯数字”做一个可回退的拼接（与官方示例 detail_url 结构一致）。
        if (StringUtils.hasText(input) && input.matches("^\\d+$")) {
            input = "https://haohuo.jinritemai.com/views/product/item2?id=" + input;
        }

        String resp = ztkApiService.dyProductConvert(input, externalInfo, true, true, false);
        if (!StringUtils.hasText(resp)) return new JSONObject();

        JSONObject raw;
        try {
            raw = JSON.parseObject(resp);
        } catch (Exception e) {
            return new JSONObject().fluentPut("raw", resp);
        }

        // 典型结构：{code,msg,data:{data:{...}}}
        JSONObject data = raw.getJSONObject("data");
        JSONObject inner = data == null ? null : data.getJSONObject("data");
        if (inner == null) inner = data;
        if (inner == null) inner = raw;

        // 尽量“对齐 TB 解析字段名”，减少前端改动：
        String clickUrl = GoodsFieldUtils.firstNonBlank(inner, "dy_zlink", "share_link");
        String tpwd = GoodsFieldUtils.firstNonBlank(inner, "dy_password");
        JSONObject couponLink = inner.getJSONObject("coupon_link");
        if (!StringUtils.hasText(tpwd) && couponLink != null) {
            tpwd = GoodsFieldUtils.firstNonBlank(couponLink, "share_command");
        }

        JSONObject out = new JSONObject();
        if (StringUtils.hasText(clickUrl)) out.put("click_url", clickUrl);
        if (StringUtils.hasText(tpwd)) out.put("tpwd", tpwd);
        out.put("raw", raw);
        return out;
    }
}

