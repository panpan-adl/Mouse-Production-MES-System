package com.mouse.mes.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class FaceChallengeService {
    private static final Duration TTL = Duration.ofSeconds(45);
    private static final String PREFIX = "mes:face:challenge:";
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);
    private final StringRedisTemplate redis;

    public FaceChallengeService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public UUID issue(Long enrollmentUserId) {
        UUID challenge = UUID.randomUUID();
        redis.opsForValue().set(PREFIX + challenge, scope(enrollmentUserId), TTL);
        return challenge;
    }

    public void consume(UUID challenge, Long enrollmentUserId) {
        if (challenge == null || !Long.valueOf(1).equals(
                redis.execute(CONSUME, List.of(PREFIX + challenge), scope(enrollmentUserId)))) {
            throw new IllegalArgumentException("人脸验证请求无效或已过期，请重试");
        }
    }

    private String scope(Long enrollmentUserId) {
        return enrollmentUserId == null ? "login" : "enroll:" + enrollmentUserId;
    }
}
