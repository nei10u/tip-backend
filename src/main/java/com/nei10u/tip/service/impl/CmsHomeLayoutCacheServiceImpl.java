package com.nei10u.tip.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nei10u.tip.service.CmsHomeLayoutCacheService;
import com.nei10u.tip.vo.HomeLayoutConfigVO;
import com.nei10u.tip.vo.HomeLayoutSectionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class CmsHomeLayoutCacheServiceImpl implements CmsHomeLayoutCacheService {

    private final ObjectMapper objectMapper;
    private final RestTemplateBuilder restTemplateBuilder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.cms.base-url:http://localhost:18090}")
    private String cmsBaseUrl;

    private final AtomicReference<HomeLayoutConfigVO> cacheRef = new AtomicReference<>();

    private RestTemplate restTemplate() {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public HomeLayoutConfigVO getCachedOrRefresh() {
        HomeLayoutConfigVO cached = cacheRef.get();
        if (cached != null && cached.getSections() != null) return cached;
        try {
            HomeLayoutConfigVO db = refreshFromDb();
            if (db.getSections() != null && !db.getSections().isEmpty()) return db;
        } catch (Exception ignored) {
            // ignore and fallback
        }
        try {
            return refreshFromCms();
        } catch (Exception e) {
            // 兜底：返回空结构，客户端可继续用本地缓存
            return HomeLayoutConfigVO.of(Collections.emptyList());
        }
    }

    @Override
    public HomeLayoutConfigVO refreshFromCms() {
        final String url = cmsBaseUrl.replaceAll("/+$", "") + "/api/cms/home/layout";
        final String raw = restTemplate().getForObject(url, String.class);
        final List<HomeLayoutSectionVO> sections = parseCmsLayout(raw);
        HomeLayoutConfigVO vo = HomeLayoutConfigVO.of(sections);
        cacheRef.set(vo);
        return vo;
    }

    @Override
    public HomeLayoutConfigVO refreshFromDb() {
        final String sql = """
                select sort_order, type, config_json
                from cms.home_page_section
                order by sort_order asc, id asc
                """;
        List<HomeLayoutSectionVO> sections = jdbcTemplate.query(sql, (rs, rowNum) -> {
            HomeLayoutSectionVO vo = new HomeLayoutSectionVO();
            vo.setSortOrder(rs.getInt("sort_order"));
            vo.setType(rs.getString("type"));
            vo.setConfig(readJsonToMap(rs.getString("config_json")));
            return vo;
        });
        HomeLayoutConfigVO vo = HomeLayoutConfigVO.of(sections);
        cacheRef.set(vo);
        return vo;
    }

    private Map<String, Object> readJsonToMap(String json) {
        final String s = (json == null || json.isBlank()) ? "{}" : json;
        try {
            return objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<HomeLayoutSectionVO> parseCmsLayout(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) return Collections.emptyList();
            List<HomeLayoutSectionVO> out = new ArrayList<>();
            for (JsonNode n : data) {
                HomeLayoutSectionVO vo = new HomeLayoutSectionVO();
                vo.setSortOrder(n.hasNonNull("sortOrder") ? n.get("sortOrder").asInt() : 0);
                vo.setType(n.hasNonNull("type") ? n.get("type").asText() : "");
                JsonNode cfg = n.get("config");
                Map<String, Object> cfgMap = cfg != null && cfg.isObject()
                        ? objectMapper.convertValue(cfg, new TypeReference<Map<String, Object>>() {})
                        : Map.of();
                vo.setConfig(cfgMap);
                out.add(vo);
            }
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}

