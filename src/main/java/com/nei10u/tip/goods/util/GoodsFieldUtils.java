package com.nei10u.tip.goods.util;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 商品字段通用解析工具：
 * - 兼容第三方接口“字段命名不稳定 / 类型不稳定（数字/字符串）”
 * - 仅做轻量兜底，不引入业务语义
 */
public final class GoodsFieldUtils {

    private GoodsFieldUtils() {
    }

    public static BigDecimal safeBigDecimal(Object v) {
        if (v == null) return null;
        try {
            String s = v.toString().trim();
            if (s.isEmpty()) return null;
            return new BigDecimal(s);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static Integer safeInt(Object v) {
        if (v == null) return null;
        try {
            String s = v.toString().trim();
            if (s.isEmpty()) return null;
            // sales_tip 可能是“1.2万+”之类，直接 parseInt 会失败；这里只做纯数字解析
            if (!s.matches("^-?\\d+$")) return null;
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static String firstNonBlank(JSONObject jo, String... keys) {
        if (jo == null || keys == null) return "";
        for (String k : keys) {
            String s = jo.getString(k);
            if (StringUtils.hasText(s)) return s.trim();
        }
        return "";
    }

    /**
     * 解析金额：兼容“分”为单位的整数（常见 >= 1000）。
     */
    public static BigDecimal parseMoneyMaybeCent(Object v) {
        BigDecimal bd = safeBigDecimal(v);
        if (bd == null) return null;
        if (bd.compareTo(new BigDecimal("1000")) >= 0) {
            return bd.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return bd;
    }

    /**
     * 解析佣金率：兼容 promotion_rate 的“万分比”到“百分比”。
     * - 1000 => 10 (%)
     */
    public static BigDecimal parseRatePermyriadToPercent(Object v) {
        BigDecimal bd = safeBigDecimal(v);
        if (bd == null) return null;
        if (bd.compareTo(BigDecimal.ZERO) <= 0) return bd;
        if (bd.compareTo(new BigDecimal("100")) > 0) {
            return bd.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        }
        return bd;
    }
}

