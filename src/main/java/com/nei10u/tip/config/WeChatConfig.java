package com.nei10u.tip.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置
 */
@Configuration
public class WeChatConfig {

    @Value("${app.wechat.miniapp.appid:}")
    private String appid;

    @Value("${app.wechat.miniapp.secret:}")
    private String secret;

    @Value("${app.wechat.miniapp.token:}")
    private String token;

    @Value("${app.wechat.miniapp.aes-key:}")
    private String aesKey;

    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(appid);
        config.setSecret(secret);
        config.setToken(token);
        config.setAesKey(aesKey);

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
