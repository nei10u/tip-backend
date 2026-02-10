package com.nei10u.tip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cms.allowed-origins:https://tip-cms.zeabur.app,http://localhost:5174}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // 允许凭证（如 Cookie）
        config.setAllowCredentials(true);
        // 使用 allowedOriginPatterns 替代 allowedOrigins 以支持通配符和凭证共存
        origins.forEach(config::addAllowedOriginPattern);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        // 统一对所有接口生效
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/tip-backend/**", config);

        return new CorsFilter(source);
    }
}