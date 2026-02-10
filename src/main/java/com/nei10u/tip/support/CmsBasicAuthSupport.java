package com.nei10u.tip.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * tip-backend -> tip-cms-backend 内部调用的 Basic Auth 统一收敛点。
 *
 * 配置项：
 * - app.cms.auth.username（默认：admin）
 * - app.cms.auth.password（默认：admin）
 */
@Component
public class CmsBasicAuthSupport {

    @Value("${app.cms.auth.username:admin}")
    private String username;

    @Value("${app.cms.auth.password:admin}")
    private String password;

    public HttpEntity<Void> authedEntity() {
        HttpHeaders headers = new HttpHeaders();
        final String u = username == null ? "" : username.trim();
        if (!u.isEmpty()) {
            headers.setBasicAuth(u, password == null ? "" : password);
        }
        return new HttpEntity<>(headers);
    }
}

