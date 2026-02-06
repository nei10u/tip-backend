package com.nei10u.tip.ordersync.util;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 订单同步数据解析工具类。
 * <p>
 * 提供 JSON 数据安全提取、类型转换和日期解析等通用功能，
 * 避免在业务代码中充斥 try-catch 和判空逻辑。
 * </p>
 */
public final class OrderSyncParseUtil {
    private OrderSyncParseUtil() {
    }

    /**
     * 尝试从 JSON 对象中获取第一个非空的字符串值。
     * 支持传入多个备选 key，按顺序查找。
     *
     * @param obj  JSON 对象
     * @param keys 备选键名列表
     * @return 找到的第一个非空字符串，如果都为空则返回 null
     */
    public static String firstNonBlank(JSONObject obj, String... keys) {
        if (obj == null || keys == null)
            return null;
        for (String k : keys) {
            if (!StringUtils.hasText(k))
                continue;
            String v = obj.getString(k);
            if (StringUtils.hasText(v))
                return v;
        }
        return null;
    }

    /**
     * 尝试从 JSON 对象中获取第一个有效的整数（不为 null）。
     * 支持传入多个备选 key，按顺序查找。
     *
     * @param obj  JSON 对象
     * @param keys 备选键名列表
     * @return 找到的第一个整数，否则返回 null
     */
    public static Integer firstPositiveInteger(JSONObject obj, String... keys) {
        if (obj == null || keys == null)
            return null;
        for (String k : keys) {
            try {
                if (!StringUtils.hasText(k))
                    continue;
                // 尝试直接获取 Integer
                Integer val = obj.getInteger(k);
                if (val != null)
                    return val;
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    /**
     * 尝试从 JSON 对象中获取第一个有效（大于0）的浮点数值。
     * 支持传入多个备选 key，按顺序查找。
     *
     * @param obj  JSON 对象
     * @param keys 备选键名列表
     * @return 找到的第一个大于0的 Double 值，否则返回 null
     */
    public static Double firstPositiveDouble(JSONObject obj, String... keys) {
        if (obj == null || keys == null)
            return null;
        for (String k : keys) {
            try {
                if (!StringUtils.hasText(k))
                    continue;
                Object raw = obj.get(k);
                if (raw == null)
                    continue;
                double v;
                if (raw instanceof Number) {
                    v = ((Number) raw).doubleValue();
                } else {
                    String s = raw.toString();
                    if (!StringUtils.hasText(s))
                        continue;
                    v = Double.parseDouble(s);
                }
                if (v > 0)
                    return v;
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    /**
     * 尝试从 JSON 对象中获取第一个有效（>=0）的浮点数值。
     * <p>
     * 适用于“回退佣金/退款金额”这类字段：0 也是有意义的合法值。
     * </p>
     */
    public static Double firstNonNegativeDouble(JSONObject obj, String... keys) {
        if (obj == null || keys == null)
            return null;
        for (String k : keys) {
            try {
                if (!StringUtils.hasText(k))
                    continue;
                Object raw = obj.get(k);
                if (raw == null)
                    continue;
                double v;
                if (raw instanceof Number) {
                    v = ((Number) raw).doubleValue();
                } else {
                    String s = raw.toString();
                    if (!StringUtils.hasText(s))
                        continue;
                    v = Double.parseDouble(s);
                }
                if (v >= 0)
                    return v;
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    /**
     * 安全解析日期时间字符串。
     * <p>
     * 优先尝试 standard pattern (yyyy-MM-dd HH:mm:ss)，
     * 失败后尝试 ISO 格式。
     * </p>
     *
     * @param raw 日期时间字符串
     * @return LocalDateTime 对象，解析失败返回 null
     */
    public static LocalDateTime parseDateTime(String raw) {
        if (!StringUtils.hasText(raw))
            return null;
        // 常见格式：yyyy-MM-dd HH:mm:ss
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignore) {
        }
        // ISO
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * LocalDateTime 转 java.util.Date。
     * 使用系统默认时区。
     *
     * @param t LocalDateTime
     * @return Date
     */
    public static Date toDate(LocalDateTime t) {
        if (t == null)
            return null;
        return Date.from(t.atZone(ZoneId.systemDefault()).toInstant());
    }
}
