package com.nei10u.tip.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nei10u.tip.service.CmsRebateConfigCacheService;
import com.nei10u.tip.support.CmsBasicAuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CmsRebateConfigCacheServiceImpl implements CmsRebateConfigCacheService {

    private final ObjectMapper objectMapper;
    private final RestTemplateBuilder restTemplateBuilder;
    private final CmsBasicAuthSupport cmsBasicAuthSupport;

    @Value("${app.cms.base-url:http://localhost:18090}")
    private String cmsBaseUrl;

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
        HttpEntity<Void> entity = cmsBasicAuthSupport.authedEntity();

        final ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class);
        final String raw = response.getBody();
        
        JSONObject data = extractData(raw);
        pageCache.put(code, data);
        return data;
    }

    @Override
    public void refreshAll() {
        // 1. 从 tip-cms 动态获取所有已启用的平台列表
        List<String> codes;
        try {
            final String url = cmsBaseUrl.replaceAll("/+$", "") + "/api/cms/platforms?status=1";
            HttpEntity<Void> entity = cmsBasicAuthSupport.authedEntity();

            final ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class);
            final String raw = response.getBody();

            JSONArray root = JSON.parseArray(raw);
            if (root != null) {
                codes = new ArrayList<>();
                for (Object object : root) {
                    JSONObject node = JSON.parseObject(String.valueOf(object));
                    String codeNode = node.getString("code");
                    if (codeNode != null) {
                        codes.add(codeNode.trim().toLowerCase());
                    }
                }
            } else {
                // 降级：仅刷新当前已缓存的平台（避免再通过配置写死）
                codes = new ArrayList<>(pageCache.keySet());
            }
        } catch (Exception e) {
            // 降级：仅刷新当前已缓存的平台（避免再通过配置写死）
            codes = new ArrayList<>(pageCache.keySet());
        }

        // 2. 遍历刷新每个平台的配置
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
    private JSONObject extractData(String rawData) {
        if (rawData == null || rawData.isBlank()) return new JSONObject();
        try {
            JSONObject jsonData = JSON.parseObject(rawData);
            return jsonData.getJSONObject("data");
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}

