package com.nei10u.tip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 好京客 / 蚂蚁星球 OpenAPI（PDD 模块）
 * <p>
 * 文档来源：`<a href="https://www.haojingke.com/open/doc.html?id=159">...</a>` 的
 * doclist/onedoc 数据源。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HjkApiService {

    private final RestTemplate restTemplate;

    @Value("${app.hjk.api-key:}")
    private String apiKey;

    private static final String BASE_URL_PDD = "http://api-gw.haojingke.com/index.php/v1/api/pdd";
    private static final String BASE_URL_VIP = "http://api-gw.haojingke.com/index.php/v1/api/vip";

    /**
     * 商品列表（doc id=144）
     */
    public String pddGoodsList(Map<String, Object> params) {
        return doPostForm(BASE_URL_PDD + "/goodslist", params);
    }

    /**
     * 商品列表（便捷重载）：支持 keyword + page/page_size。
     */
    public String pddGoodsList(String keyword, int pageId, int pageSize) {
        Map<String, Object> params = new TreeMap<>();
        params.put("page", Math.max(pageId, 1));
        params.put("page_size", pageSize <= 0 ? 20 : pageSize);
        if (keyword != null && !keyword.trim().isEmpty()) {
            params.put("keyword", keyword.trim());
        }
        return pddGoodsList(params);
    }

    /**
     * 商品详情（doc id=146）
     */
    public String pddGoodsDetail(Map<String, Object> params) {
        return doPostForm(BASE_URL_PDD + "/goodsdetail", params);
    }

    /**
     * 分类列表（doc id=147）
     */
    public String pddCats(Integer parentCatId) {
        return doPostForm(BASE_URL_PDD + "/cats", Map.of("parent_cat_id", parentCatId == null ? 0 : parentCatId));
    }

    /**
     * 运营频道商品（doc id=159）
     * - channel_type: 0=1.9包邮, 1=今日爆款(默认), 2=品牌清仓
     */
    public String pddGetRecommendGoods(Integer offset, Integer limit, Integer channelType, Integer isUnion) {
        Map<String, Object> params = new TreeMap<>();
        if (offset != null)
            params.put("offset", offset);
        if (limit != null)
            params.put("limit", limit);
        if (channelType != null)
            params.put("channel_type", channelType);
        if (isUnion != null)
            params.put("isunion", isUnion);
        return doPostForm(BASE_URL_PDD + "/getrecommendgoods", params);
    }

    /**
     * 唯品会：商品查询（goodsquery）
     * <p>
     * 文档示例（以公开页为准）：
     * -
     * <a href="http://api-gw.haojingke.com/index.php/v1/api/vip/goodslist">...</a>
     * <p>
     * 关键参数：
     * - pageindex：第几页（默认 1）
     * - pagesize：每页条数（默认 20）
     * - keyword：关键词（多数场景必填；这里做可选兜底）
     */
    private String vipGoodsQuery(Map<String, Object> params) {
        params.put("openId", "default_open_id");
        params.put("realCall", true);
        return doPostForm(BASE_URL_VIP + "/goodslist", params);
    }

    public String vipGoodsQuery(int pageId, int pageSize) {
        Map<String, Object> params = new TreeMap<>();
        params.put("pageindex", Math.max(pageId, 1));
        params.put("pagesize", pageSize <= 0 ? 20 : pageSize);
        return vipGoodsQuery(params);
    }

    public String vipChannelGoodsQuery(int channelType, int sourceType, int pageId, int pageSize) {
        Map<String, Object> params = new TreeMap<>();
        params.put("channelType", channelType);
        params.put("sourceType", sourceType);
        params.put("pageindex", Math.max(pageId, 1));
        params.put("pagesize", pageSize <= 0 ? 20 : pageSize);

        return vipGoodsQuery(params);
    }

    private String doPostForm(String url, Map<String, Object> params) {
        try {
            // 统一把 apikey 附带到 query（实际测试：无 apikey 会返回 status_code=1）
            String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("apikey", apiKey == null ? "" : apiKey)
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            if (params != null) {
                params.forEach((k, v) -> {
                    if (v == null)
                        return;
                    String s = String.valueOf(v).trim();
                    if (s.isEmpty())
                        return;
                    form.add(k, s);
                });
            }

            HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);
            return restTemplate.postForObject(fullUrl, req, String.class);
        } catch (Exception e) {
            log.error("HJK request failed: {}", url, e);
            return null;
        }
    }
}
