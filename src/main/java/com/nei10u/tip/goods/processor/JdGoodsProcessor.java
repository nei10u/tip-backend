package com.nei10u.tip.goods.processor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nei10u.tip.goods.util.GoodsFieldUtils;
import com.nei10u.tip.service.GoodsCacheService;
import com.nei10u.tip.service.ZtkApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.nei10u.tip.service.impl.GoodsCacheServiceImpl.HOME_LIST_PAGE_ID;
import static com.nei10u.tip.service.impl.GoodsCacheServiceImpl.HOME_LIST_PAGE_SIZE;

/**
 * 京东（JD）平台商品处理器：负责接入折淘客 ZTK 的京东相关能力，并做字段归一化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdGoodsProcessor {

    private final ZtkApiService ztkApiService;
    private final GoodsCacheService goodsCacheService;

    public JSONObject getJdCurrentTrend(int cid, int pageId, int pageSize) {
        // 首页最下方：只缓存第一页（cid=0 & pageSize=20）
        if (HOME_LIST_PAGE_ID == pageId && HOME_LIST_PAGE_SIZE == pageSize && cid == 0) {
            return goodsCacheService.getOrLoadJdCurrentFirstPage();
        }
        return goodsCacheService.fetchJdCurrentTrend(cid, pageId, pageSize);
    }

    public JSONObject getJdSelectedGoodsList(String pinPaiName, String pinPai, String cid, String sort, int pageId, int pageSize) {
        String response = ztkApiService.getJdGoodsList(pinPaiName, pinPai, cid, sort, Strings.EMPTY, Strings.EMPTY, pageId, pageSize);
        return StringUtils.hasText(response) ? normalizeZtkJdSelectedLikeList(JSON.parseObject(response), pageId, pageSize) : new JSONObject();
    }

    public JSONObject getJdSelfOperatedGoodsList(String pinPaiName, String pinPai, String cid, String sort, String tj, int pageId, int pageSize) {
        String response = ztkApiService.getJdGoodsList(pinPaiName, pinPai, cid, sort, Strings.EMPTY, tj, pageId, pageSize);
        return StringUtils.hasText(response) ? normalizeZtkJdSelectedLikeList(JSON.parseObject(response), pageId, pageSize) : new JSONObject();
    }

    /**
     * 当前 tip-backend 未接入“京东30天榜单”第三方接口：先占位返回空结构。
     */
    public JSONObject getJd30DaysTrend(String pageId, String cid) {
        return new JSONObject()
                .fluentPut("list", new JSONArray())
                .fluentPut("raw", new JSONObject())
                .fluentPut("pageId", pageId)
                .fluentPut("pageSize", HOME_LIST_PAGE_SIZE);
    }

    /**
     * 京东详情：折淘客大字段接口（包含详情图），做最大兼容与字段归一化。
     */
    public JSONObject getJdGoodsDetail(String goodsId) {
        String response = ztkApiService.getJdGoodsDetails(goodsId);
        if (!StringUtils.hasText(response)) return new JSONObject();

        try {
            JSONObject item = null;
            String trimmed = response.trim();
            if (trimmed.startsWith("[")) {
                JSONArray arr = JSON.parseArray(trimmed);
                if (arr != null && !arr.isEmpty()) item = arr.getJSONObject(0);
            } else if (trimmed.startsWith("{")) {
                JSONObject obj = JSON.parseObject(trimmed);
                if (obj != null) {
                    if (obj.containsKey("jd_union_open_goods_bigfield_query_response")) {
                        JSONObject wrap = obj.getJSONObject("jd_union_open_goods_bigfield_query_response");
                        String resultStr = wrap == null ? null : wrap.getString("result");
                        if (StringUtils.hasText(resultStr)) {
                            String rs = resultStr.trim();
                            try {
                                if (rs.startsWith("{")) {
                                    obj = JSON.parseObject(rs);
                                } else if (rs.startsWith("[")) {
                                    JSONArray rArr = JSON.parseArray(rs);
                                    if (rArr != null && !rArr.isEmpty()) item = rArr.getJSONObject(0);
                                }
                            } catch (Exception ignore) {
                            }
                        }
                    }

                    if (item == null && obj != null && obj.containsKey("data") && obj.get("data") instanceof String) {
                        try {
                            String ds = obj.getString("data");
                            if (StringUtils.hasText(ds)) {
                                String dts = ds.trim();
                                if (dts.startsWith("[")) {
                                    JSONArray dArr = JSON.parseArray(dts);
                                    if (dArr != null && !dArr.isEmpty()) item = dArr.getJSONObject(0);
                                } else if (dts.startsWith("{")) {
                                    item = JSON.parseObject(dts);
                                }
                            }
                        } catch (Exception ignore) {
                        }
                    }

                    if (item == null) {
                        Object content = obj.get("content");
                        Object data = obj.get("data");
                        if (content instanceof JSONArray cArr && !cArr.isEmpty()) item = cArr.getJSONObject(0);
                        else if (data instanceof JSONArray dArr && !dArr.isEmpty()) item = dArr.getJSONObject(0);
                        else if (data instanceof JSONObject dObj) item = dObj;
                        else item = obj;
                    }
                }
            }
            if (item == null) return new JSONObject();

            JSONObject normalized = new JSONObject();
            normalized.put("platform", "jd");

            Object skuId = item.get("skuId");
            if (skuId == null) skuId = item.get("productId");
            if (skuId == null) skuId = item.get("mainSkuId");
            normalized.put("goodsId", skuId != null ? String.valueOf(skuId) : goodsId);

            String skuName = item.getString("skuName");
            if (StringUtils.hasText(skuName)) normalized.put("title", skuName);

            // 轮播图（imageInfo.imageList[].url）
            List<String> carouselPics = new ArrayList<>();
            JSONObject imageInfo = item.getJSONObject("imageInfo");
            if (imageInfo != null) {
                JSONArray imageList = imageInfo.getJSONArray("imageList");
                if (imageList != null) {
                    for (int i = 0; i < imageList.size(); i++) {
                        JSONObject img = imageList.getJSONObject(i);
                        if (img == null) continue;
                        String url = img.getString("url");
                        if (!StringUtils.hasText(url)) continue;
                        url = url.trim();
                        if (url.startsWith("//")) url = "https:" + url;
                        carouselPics.add(url);
                    }
                }
            }
            if (!carouselPics.isEmpty()) {
                normalized.put("carouselPics", carouselPics);
                normalized.put("mainPic", carouselPics.get(0));
            }

            // 详情图（detailImages 为逗号分隔字符串）
            String detailImages = item.getString("detailImages");
            if (StringUtils.hasText(detailImages)) {
                List<String> detailPics = Arrays.stream(detailImages.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .map(s -> s.startsWith("//") ? "https:" + s : s)
                        .collect(Collectors.toList());
                if (!detailPics.isEmpty()) normalized.put("detailPics", detailPics);
            }

            normalized.put("raw", item);
            return normalized;
        } catch (Exception ignore) {
            return new JSONObject();
        }
    }

    /**
     * 折淘客 JD 列表归一化为：{list:[GoodsLike...], totalNum, pageId, pageSize, raw}
     */
    private JSONObject normalizeZtkJdSelectedLikeList(JSONObject raw, int pageId, int pageSize) {
        if (raw == null) return new JSONObject();

        JSONArray content = raw.getJSONArray("content");
        if (content == null) {
            JSONObject data = raw.getJSONObject("data");
            if (data != null) content = data.getJSONArray("content");
        }
        if (content == null) content = new JSONArray();

        JSONArray list = new JSONArray();
        for (int i = 0; i < content.size(); i++) {
            Object item = content.get(i);
            if (!(item instanceof JSONObject jo)) continue;

            JSONObject g = new JSONObject();
            g.put("goodsId", jo.getString("code"));
            g.put("title", jo.getString("title"));
            g.put("desc", jo.getString("jianjie"));

            String mainPic = jo.getString("pict_url");
            if (!StringUtils.hasText(mainPic)) mainPic = jo.getString("white_image");
            g.put("mainPic", mainPic);

            g.put("actualPrice", GoodsFieldUtils.safeBigDecimal(jo.get("quanhou_jiage")));
            g.put("originalPrice", GoodsFieldUtils.safeBigDecimal(jo.get("size")));

            g.put("couponPrice", GoodsFieldUtils.safeBigDecimal(jo.get("coupon_info_money")));
            g.put("couponStartTime", jo.getString("coupon_start_time"));
            g.put("couponEndTime", jo.getString("coupon_end_time"));

            g.put("estimateAmount", GoodsFieldUtils.safeBigDecimal(jo.get("tkfee3")));
            g.put("commissionRate", GoodsFieldUtils.safeBigDecimal(jo.get("tkrate3")));

            Integer sales = GoodsFieldUtils.safeInt(jo.get("volume"));
            if (sales == null) sales = GoodsFieldUtils.safeInt(jo.get("sellCount"));
            g.put("monthSales", sales);

            g.put("shopName", GoodsFieldUtils.firstNonBlank(jo, "shop_title", "nick", "provcity"));
            g.put("haitao", GoodsFieldUtils.safeInt(jo.get("haitao")));
            g.put("platform", "jd");

            String smallImages = jo.getString("small_images");
            if (StringUtils.hasText(smallImages)) {
                g.put("imgs", smallImages.replace("|", ","));
            }

            list.add(g);
        }

        JSONObject out = new JSONObject();
        out.put("list", list);
        out.put("totalNum", list.size());
        out.put("pageId", String.valueOf(Math.max(pageId, 1)));
        out.put("pageSize", pageSize);
        out.put("raw", raw);
        return out;
    }
}

