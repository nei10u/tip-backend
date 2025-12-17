package com.nei10u.tip.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 微信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatService {

    private final WxMaService wxMaService;

    /**
     * 微信登录 (code2session)
     */
    public WxMaJscode2SessionResult login(String code) {
        try {
            return wxMaService.getUserService().getSessionInfo(code);
        } catch (Exception e) {
            log.error("微信登录失败: code={}", code, e);
            throw new RuntimeException("微信登录失败", e);
        }
    }
}
