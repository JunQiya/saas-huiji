package com.huiji.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务：内存存储 + 5 分钟过期。
 *
 * <p>当前实现仅生成 6 位数字码并回显到响应（开发/演示模式）。
 * 如接入了真实短信网关，可在此处替换为对阿里云/腾讯云/多吉云等 SDK 的调用。</p>
 */
@Slf4j
@Service
public class SmsCodeService {

    /** 有效期 5 分钟 */
    private static final long EXPIRE_SECONDS = 300L;
    /** 60 秒内同号限发一次 */
    private static final long SEND_COOLDOWN_SECONDS = 60L;

    private final SecureRandom random = new SecureRandom();

    @Value("${huiji.h5.sms-dev-mode:true}")
    private boolean devMode;

    private final Map<String, Entry> codeMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSendMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleaner;

    @PostConstruct
    public void start() {
        cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sms-code-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::cleanExpired, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void stop() {
        if (cleaner != null) cleaner.shutdownNow();
    }

    /**
     * 发送验证码，返回生成的码（dev 模式直接回显，生产模式应发到运营商并返回 null）。
     */
    public String send(String phone) {
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        long now = System.currentTimeMillis();
        Long last = lastSendMap.get(phone);
        if (last != null && now - last < SEND_COOLDOWN_SECONDS * 1000L) {
            long wait = (SEND_COOLDOWN_SECONDS * 1000L - (now - last)) / 1000L;
            throw new IllegalStateException("请 " + wait + " 秒后再试");
        }
        lastSendMap.put(phone, now);

        String code = String.format("%06d", random.nextInt(1_000_000));
        codeMap.put(phone, new Entry(code, now + EXPIRE_SECONDS * 1000L));
        log.info("[SMS] phone={} code={} devMode={}", phone, devMode ? code : "******", devMode);
        return devMode ? code : null;
    }

    /** 校验并消费：成功返回 true，失败/过期/不存在返回 false。 */
    public boolean verifyAndConsume(String phone, String code) {
        if (phone == null || code == null) return false;
        Entry e = codeMap.get(phone);
        if (e == null) return false;
        long now = System.currentTimeMillis();
        if (now > e.expireAt) {
            codeMap.remove(phone);
            return false;
        }
        if (!e.code.equals(code)) return false;
        codeMap.remove(phone);
        return true;
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        codeMap.entrySet().removeIf(en -> en.getValue().expireAt < now);
        // 冷却表也清理 1 小时之前的，避免无限增长
        long boundary = now - 3600_000L;
        lastSendMap.entrySet().removeIf(en -> en.getValue() < boundary);
    }

    private static class Entry {
        final String code;
        final long expireAt;
        Entry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
