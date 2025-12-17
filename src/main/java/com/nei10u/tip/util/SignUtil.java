package com.nei10u.tip.util;

import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名工具类
 */
@Slf4j
public class SignUtil {

    /**
     * 生成MD5签名
     * 
     * @param params    参数Map
     * @param appSecret 应用密钥
     * @return 签名字符串
     */
    public static String generateMd5Sign(Map<String, String> params, String appSecret) {
        try {
            // 1. 参数排序
            Map<String, String> sortedParams = new TreeMap<>(params);

            // 2. 拼接字符串
            StringBuilder sb = new StringBuilder();
            sortedParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    sb.append(key).append(value);
                }
            });
            sb.append(appSecret);

            // 3. MD5加密
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            // 4. 转十六进制
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            log.error("Failed to generate sign", e);
            throw new RuntimeException("签名生成失败", e);
        }
    }
}
