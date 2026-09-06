package com.LinkedKnowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build();

    @Value("${sms.cooldown-seconds:60}")
    private int cooldownSeconds;
    @Value("${sms.ttl-seconds:300}")
    private int ttlSeconds;
    @Value("${sms.max-fails:5}")
    private int maxFails;
    @Value("${sms.webhook-url:}")
    private String webhookUrl;
    @Value("${sms.dev-echo:true}")
    private boolean devEcho;

    public record SendResult(int cooldownSeconds, String devCode) {}

    public String normalizePhone(String raw) {
        if (raw == null) return "";
        String p = raw.trim().replaceAll("\\s+", "");
        if (p.startsWith("+86")) p = p.substring(3);
        if (p.startsWith("86") && p.length() == 13) p = p.substring(2);
        return p;
    }

    public boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    public SendResult sendCode(String rawPhone, String clientIp) {
        String phone = normalizePhone(rawPhone);
        if (!isValidPhone(phone)) {
            throw new RuntimeException("手机号格式不正确");
        }
        String cdKey = "sms:cd:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cdKey))) {
            Long remain = redisTemplate.getExpire(cdKey, TimeUnit.SECONDS);
            throw new RuntimeException("发送过于频繁，请" + Math.max(1, remain == null ? cooldownSeconds : remain) + "秒后再试");
        }
        if (clientIp != null && !clientIp.isBlank()) {
            String ipKey = "sms:ip:" + clientIp;
            Long n = redisTemplate.opsForValue().increment(ipKey);
            if (n != null && n == 1) {
                redisTemplate.expire(ipKey, Duration.ofHours(1));
            }
            if (n != null && n > 20) {
                throw new RuntimeException("该网络发送次数过多，请稍后再试");
            }
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set("sms:code:" + phone, code, Duration.ofSeconds(ttlSeconds));
        redisTemplate.opsForValue().set(cdKey, "1", Duration.ofSeconds(cooldownSeconds));
        redisTemplate.delete("sms:fail:" + phone);
        dispatch(phone, code);
        return new SendResult(cooldownSeconds, devEcho ? code : null);
    }

    public void verifyOrThrow(String rawPhone, String code) {
        String phone = normalizePhone(rawPhone);
        if (!isValidPhone(phone)) {
            throw new RuntimeException("手机号格式不正确");
        }
        if (code == null || !code.trim().matches("^\\d{4,8}$")) {
            throw new RuntimeException("验证码不正确");
        }
        String failKey = "sms:fail:" + phone;
        String stored = redisTemplate.opsForValue().get("sms:code:" + phone);
        if (stored == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!stored.equals(code.trim())) {
            Long fails = redisTemplate.opsForValue().increment(failKey);
            if (fails != null && fails == 1) {
                redisTemplate.expire(failKey, Duration.ofMinutes(15));
            }
            if (fails != null && fails >= maxFails) {
                redisTemplate.delete("sms:code:" + phone);
                throw new RuntimeException("验证码错误次数过多，请重新获取");
            }
            throw new RuntimeException("验证码不正确");
        }
        redisTemplate.delete("sms:code:" + phone);
        redisTemplate.delete(failKey);
    }

    private void dispatch(String phone, String code) {
        log.info("[SMS] phone={} code={}", phone, code);
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String body = "{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}";
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(body, JSON))
                .header("Content-Type", "application/json")
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("[SMS] webhook {} → {}", webhookUrl, response.code());
            }
        } catch (Exception e) {
            log.warn("[SMS] webhook failed: {}", e.getMessage());
        }
    }
}
