package com.nei10u.tip.controller;

import com.nei10u.tip.mapper.PromotionClickMapper;
import com.nei10u.tip.mapper.PromotionInfoMapper;
import com.nei10u.tip.model.PromotionClick;
import com.nei10u.tip.model.PromotionInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Date;

/**
 * 推广分享跳转与点击埋点。
 * <p>
 * - 前端分享时使用 redirectUrl，让后端记录点击，再 302 跳转到联盟 promotionUrl。
 */
@Slf4j
@Controller
@RequestMapping("/api/promo")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionInfoMapper promotionInfoMapper;
    private final PromotionClickMapper promotionClickMapper;

    @GetMapping("/redirect/{infoId}")
    public RedirectView redirect(
            @PathVariable Long infoId,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request
    ) {
        PromotionInfo info = promotionInfoMapper.selectById(infoId);
        String target = (info == null) ? null : info.getPromotionUrl();

        // 记录点击
        try {
            PromotionClick click = new PromotionClick();
            click.setInfoId(infoId);
            click.setUserId(userId);
            click.setClickTime(new Date());
            click.setIp(getClientIp(request));
            promotionClickMapper.insert(click);
        } catch (Exception e) {
            log.warn("Insert promotion_click failed: infoId={}", infoId, e);
        }

        RedirectView rv = new RedirectView();
        rv.setContextRelative(false);
        rv.setExposeModelAttributes(false);
        rv.setPropagateQueryParams(false);
        rv.setUrl(StringUtils.hasText(target) ? target : "/");
        return rv;
    }

    private static String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            // 取第一个
            int idx = xff.indexOf(',');
            return (idx > 0 ? xff.substring(0, idx) : xff).trim();
        }
        String xrip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xrip)) return xrip.trim();
        return request.getRemoteAddr();
    }
}


