package com.nei10u.tip.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Enumeration;

/**
 * tip-cms 后台管理接口代理：
 * <p>
 * 目的：让 CMS 前端可以把 CMS_API_BASE 指向 tip-backend（例如 http://localhost:8080），
 * 由 tip-backend 统一转发到实际的 tip-cms-backend（app.cms.base-url）。
 * <p>
 * 解决场景：
 * - 只暴露 8080 端口时，/api/cms/pages 保存/发布无法落库（404/连接失败）；
 * - 发布按钮仅刷新 tip-backend 缓存，不会修改 cms.page.status；
 * - 本代理转发 /api/cms/** 给 tip-cms-backend，确保 cms.page / cms.page_section 真正更新。
 */
@RestController
@RequiredArgsConstructor
public class CmsAdminProxyController {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${app.cms.base-url:http://localhost:18090}")
    private String cmsBaseUrl;

    private RestTemplate restTemplate() {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
    }

    @RequestMapping(value = "/api/cms/**")
    public ResponseEntity<String> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) String body) {
        final String base = cmsBaseUrl == null ? "" : cmsBaseUrl.replaceAll("/+$", "");
        final String uri = request.getRequestURI(); // already includes /api/cms/...
        final String qs = request.getQueryString();
        final String targetUrl = base + uri + (qs == null || qs.isBlank() ? "" : "?" + qs);

        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("unsupported_method");
        }

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            if (name == null) continue;
            String lower = name.toLowerCase();
            // 避免 Host/Content-Length 之类的转发副作用
            if (lower.equals("host") || lower.equals("content-length")) continue;
            Enumeration<String> values = request.getHeaders(name);
            while (values != null && values.hasMoreElements()) {
                headers.add(name, values.nextElement());
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate().exchange(targetUrl, method, entity, String.class);

        // 回传必要响应头（避免 CORS/缓存头丢失）；同时避免把目标 Host 等无关 header 回传
        HttpHeaders outHeaders = new HttpHeaders();
        resp.getHeaders().forEach((k, v) -> {
            if (k == null) return;
            String lower = k.toLowerCase();
            if (lower.equals("transfer-encoding") || lower.equals("content-length")) return;
            outHeaders.put(k, v);
        });
        return new ResponseEntity<>(resp.getBody(), outHeaders, resp.getStatusCode());
    }
}

