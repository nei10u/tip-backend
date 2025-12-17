package com.nei10u.tip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.TreeMap;

/**
 * 折淘客API服务 (唯品会)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZtkApiService {

    private final RestTemplate restTemplate;

    @Value("${app.ztk.api-key:}")
    private String apiKey;

    @Value("${app.ztk.sid:}")
    private String sid;

    private static final String BASE_URL = "https://api.zhetaoke.com:10001";

    /**
     * 获取唯品会商品列表
     */
    public String getVipGoodsList(int pageId, int pageSize, String keyword) {
        String url = BASE_URL + "/api/open/vip/goods/list";

        Map<String, String> params = new TreeMap<>();
        params.put("page", String.valueOf(pageId));
        params.put("page_size", String.valueOf(pageSize));
        params.put("appkey", apiKey);
        params.put("sid", sid);
        if (keyword != null) {
            params.put("keyword", keyword);
        }

        return doRequest(url, params);
    }

    /**
     * 执行API请求
     */
    private String doRequest(String url, Map<String, String> params) {
        try {
            // 构建完整URL
            StringBuilder urlBuilder = new StringBuilder(url).append("?");
            params.forEach((key, value) -> urlBuilder.append(key).append("=").append(value).append("&"));
            String fullUrl = urlBuilder.substring(0, urlBuilder.length() - 1);

            log.info("Requesting ZTK API: {}", url);

            // 发送请求
            String response = restTemplate.getForObject(fullUrl, String.class);
            log.debug("ZTK API Response: {}", response);

            return response;
        } catch (Exception e) {
            log.error("Failed to request ZTK API: {}", url, e);
            return null;
        }
    }
}
