package com.nei10u.tip.ordersync.tb;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * 淘宝 Open Platform 传统签名（MD5）：
 * sign = MD5( secret + concat(sorted(k+v)) + secret ).toUpperCase()
 */
public final class TbSignUtil {
    private TbSignUtil() {}

    public static String signMd5(Map<String, String> params, String secret) {
        if (secret == null) secret = "";
        TreeMap<String, String> sorted = new TreeMap<>();
        if (params != null) sorted.putAll(params);

        StringBuilder sb = new StringBuilder(secret);
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank()) continue;
            String v = e.getValue();
            if (v == null) continue;
            sb.append(e.getKey()).append(v);
        }
        sb.append(secret);
        return md5HexUpper(sb.toString());
    }

    private static String md5HexUpper(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString().toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 sign failed", e);
        }
    }
}


