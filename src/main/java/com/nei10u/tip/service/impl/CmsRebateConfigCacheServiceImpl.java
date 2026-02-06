package com.nei10u.tip.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nei10u.tip.service.CmsRebateConfigCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CmsRebateConfigCacheServiceImpl implements CmsRebateConfigCacheService {

    private final ObjectMapper objectMapper;
    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${app.cms.base-url:http://localhost:18090}")
    private String cmsBaseUrl;

    @Value("${app.cms.rebate-platform-codes:tb,jd,pdd,dy}")
    private String rebatePlatformCodes;

    private final Map<String, JSONObject> pageCache = new ConcurrentHashMap<>();

    private RestTemplate restTemplate() {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public JSONObject getPublishedPage(String platformCode) {
        final String code = safeCode(platformCode);
        if (code.isEmpty()) return new JSONObject();
        JSONObject cached = pageCache.get(code);
        if (cached != null) return cached;
        try {
            return refreshPublishedPage(code);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public JSONObject refreshPublishedPage(String platformCode) {
        final String code = safeCode(platformCode);
        if (code.isEmpty()) return new JSONObject();

        final String url = cmsBaseUrl.replaceAll("/+$", "") + "/api/cms/rebate/pages/" + code;
        final String raw = restTemplate().getForObject(url, String.class);
        JSONObject data = extractData(raw);
        pageCache.put(code, data);
        return data;
    }

    @Override
    public void refreshAll() {
        List<String> codes = Arrays.stream(rebatePlatformCodes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        for (String c : codes) {
            try {
                refreshPublishedPage(c);
            } catch (Exception ignored) {
            }
        }
    }

    private String safeCode(String platformCode) {
        return platformCode == null ? "" : platformCode.trim().toLowerCase();
    }

    /**
     * tip-cms 返回结构：{ code: 200, message: "success", data: {...} }
     * 这里仅提取 data 并转为 fastjson2 JSONObject 便于直接透传。
     */
    private JSONObject extractData(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return new JSONObject();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) return new JSONObject();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(data, Map.class);
            return new JSONObject(map);
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}

