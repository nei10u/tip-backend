package com.nei10u.tip.ordersync.tb;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 淘宝开放平台 HTTP Client（不依赖 taobao-sdk）。
 * <p>
 * 参考 legacy：gateway 固定为 <a href="https://eco.taobao.com/router/rest">...</a>
 */
@Service
@RequiredArgsConstructor
public class TbOpenAPIClient {

    private final RestTemplate restTemplate;

    @Value("${app.tb.gateway:https://eco.taobao.com/router/rest}")
    private String gateway;

    @Value("${app.tb.app-key:}")
    private String appKey;

    @Value("${app.tb.app-secret:}")
    private String appSecret;

    /** 有些 API 需要 session，若不需要可留空 */
    @Value("${app.tb.session:}")
    private String session;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String execute(String method, Map<String, String> bizParams) {
        if (!StringUtils.hasText(method)) {
            throw new IllegalArgumentException("method is required");
        }
        Map<String, String> params = new HashMap<>();
        params.put("method", method);
        params.put("app_key", appKey);
        params.put("format", "json");
        params.put("v", "2.0");
        params.put("sign_method", "md5");
        params.put("timestamp", LocalDateTime.now().format(TS_FMT));
        if (StringUtils.hasText(session)) {
            params.put("session", session);
        }
        if (bizParams != null) {
            params.putAll(bizParams);
        }

        String sign = TbSignUtil.signMd5(params, appSecret);
        params.put("sign", sign);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);

        return restTemplate.postForObject(gateway, form, String.class);
    }

    public String tbkOrderDetailsGet(Map<String, String> bizParams) {
        return execute("taobao.tbk.order.details.get", bizParams);
    }
}


