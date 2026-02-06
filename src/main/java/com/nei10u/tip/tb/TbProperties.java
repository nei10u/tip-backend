package com.nei10u.tip.tb;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 淘宝开放平台配置（与 application.yml: app.tb.* 对齐）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.tb")
public class TbProperties {

    /**
     * API 网关：<a href="https://eco.taobao.com/router/rest">...</a>
     */
    private String gateway;

    /**
     * 淘宝开放平台 appKey（yml 中为 app-key）
     */
    private String appKey;

    /**
     * 淘宝开放平台 appSecret（yml 中为 app-secret）
     */
    private String appSecret;

    /**
     * 可选：部分接口需要的 session
     */
    private String session;

    private Oauth oauth = new Oauth();

    @Data
    public static class Oauth {
        /**
         * 授权地址：https://oauth.taobao.com/authorize
         */
        private String authorizeUrl = "https://oauth.taobao.com/authorize";

        /**
         * 回调地址（必须在淘宝开放平台配置白名单）
         */
        private String redirectUri;

        /**
         * 渠道邀请码（与 legacy 对齐）
         */
        private String inviterCode;

        /**
         * 备注（与 legacy 对齐）
         */
        private String note;

        /**
         * infoType（通常为 1）
         */
        private Long infoType = 1L;

        /**
         * state 有效期（秒）
         */
        private long stateTtlSeconds = 600;

        /**
         * 授权结果有效期（秒）
         */
        private long resultTtlSeconds = 1800;
    }
}


