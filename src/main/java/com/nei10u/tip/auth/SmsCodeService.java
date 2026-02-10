package com.nei10u.tip.auth;

import com.nei10u.tip.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码服务
 *
 * 当前实现：
 * - 生成 6 位数字验证码
 * - 存入进程内存（TTL 5 分钟）
 * - 实际短信发送由外部服务对接（此处仅记录日志，便于联调）
 *
 * 注意：
 * - 该实现不依赖 Redis，适合本地/单实例环境
 * - 若后端水平扩容为多实例，验证码需要共享存储（可再接入 Redis/短信网关自校验等）
 */
@Slf4j
@Service
public class SmsCodeService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final SecureRandom RND = new SecureRandom();

    private static final class Entry {
        final String code;
        final long expiresAtMs;

        Entry(String code, long expiresAtMs) {
            this.code = code;
            this.expiresAtMs = expiresAtMs;
        }
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    private String normPhone(String phone) {
        return phone == null ? "" : phone.trim();
    }

    public void send(String phone) {
        final String p = normPhone(phone);
        if (p.isEmpty()) {
            throw new BusinessException("INVALID_PARAM", "手机号不能为空");
        }
        final String code = String.format("%06d", RND.nextInt(1_000_000));
        final long exp = System.currentTimeMillis() + TTL.toMillis();
        store.put(p, new Entry(code, exp));
        // TODO 接入真实短信平台（阿里云/腾讯云/极光等）
        log.info("[SMS] phone={}, code={} (ttl={}s)", p, code, TTL.toSeconds());
    }

    public void verifyOrThrow(String phone, String code) {
        final String p = normPhone(phone);
        if (p.isEmpty()) {
            throw new BusinessException("INVALID_PARAM", "手机号不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("INVALID_PARAM", "验证码不能为空");
        }
        final Entry entry = store.get(p);
        if (entry == null) {
            throw new BusinessException("SMS_CODE_EXPIRED", "验证码已过期");
        }
        if (entry.expiresAtMs < System.currentTimeMillis()) {
            store.remove(p);
            throw new BusinessException("SMS_CODE_EXPIRED", "验证码已过期");
        }
        if (!entry.code.equals(code.trim())) {
            throw new BusinessException("SMS_CODE_INVALID", "验证码错误");
        }
        // 一次性使用
        store.remove(p);
    }
}

