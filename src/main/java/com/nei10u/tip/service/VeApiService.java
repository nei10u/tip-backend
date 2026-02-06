package com.nei10u.tip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * VEAPI 转链实现（对齐 legacy TbGoodsData.convertVe）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VeApiService {

    private final RestTemplate restTemplate;

    @Value("${app.veapi.key:}")
    private String veKey;

    @Value("${app.veapi.account-id:}")
    private String accountId;

    @Value("${app.veapi.promotion-type:2}")
    private String promotionType;

    @Value("${app.veapi.detail:2}")
    private String detail;

    private static final String URL = "http://api.veapi.cn/tbk/generalconvert";

    public String generalConvert(String para, String relationId) {
        if (!StringUtils.hasText(veKey)) {
            return null; // 未配置则不启用
        }
        if (!StringUtils.hasText(para)) {
            return null;
        }

        try {
            Map<String, String> params = new TreeMap<>();
            params.put("vekey", veKey);
            params.put("para", URLEncoder.encode(para, StandardCharsets.UTF_8));
            if (StringUtils.hasText(relationId)) {
                params.put("relation_id", relationId);
            }
            if (StringUtils.hasText(accountId)) {
                params.put("account_id", accountId);
            }
            if (StringUtils.hasText(promotionType)) {
                params.put("promotion_type", promotionType);
            }
            if (StringUtils.hasText(detail)) {
                params.put("detail", detail);
            }

            // legacy 行为：根据输入形态选择 material_dto / item_dto，并固定 required_link_type
            params.put("globaltpwd", "1");
            String required = "coupon_supered_long_url,cps_supered_long_url,coupon_supered_short_url,coupon_supered_short_tpwd,cps_supered_short_url,cps_supered_short_tpwd";
            params.put("required_link_type", required);

            if (para.contains("https://") || para.contains("http://")) {
                params.put("material_dto", "{\"material_id\":\"" + para + "\"}");
            } else if (para.matches("^[A-Za-z0-9]+-[A-Za-z0-9]+$")) {
                params.put("item_dto", "{\"item_id\":\"" + para + "\"}");
            }

            String fullUrl = buildUrl(URL, params);
            log.info("Requesting VEAPI Full URL: {}", fullUrl);
            return restTemplate.getForObject(fullUrl, String.class);
        } catch (Exception e) {
            log.error("Failed to request VEAPI generalconvert", e);
            return null;
        }
    }

    private static String buildUrl(String base, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(base).append("?");
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        return sb.substring(0, sb.length() - 1);
    }
}



