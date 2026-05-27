package com.yuncode.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录尝试限制服务 — 基于 Redis 计数器的暴力破解防护
 *
 * 连续登录失败超过阈值后锁定账号，锁定期间拒绝登录请求。
 * 锁定时间可通过系统设置中的 loginLockDuration 配置。
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final String ATTEMPT_KEY_PREFIX = "login:attempts:";
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final long DEFAULT_LOCK_MINUTES = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    public LoginAttemptService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查登录是否被锁定
     *
     * @param username 用户名
     * @return true 表示已锁定，拒绝登录
     */
    public boolean isLocked(String username) {
        String key = buildKey(username);
        Integer attempts = (Integer) redisTemplate.opsForValue().get(key);
        return attempts != null && attempts >= DEFAULT_MAX_ATTEMPTS;
    }

    /**
     * 登录失败 — 增加失败计数
     *
     * @param username 用户名
     */
    public void loginFailed(String username) {
        String key = buildKey(username);
        Integer attempts = (Integer) redisTemplate.opsForValue().get(key);
        if (attempts == null) {
            redisTemplate.opsForValue().set(key, 1, DEFAULT_LOCK_MINUTES, TimeUnit.MINUTES);
        } else {
            redisTemplate.opsForValue().set(key, attempts + 1, DEFAULT_LOCK_MINUTES, TimeUnit.MINUTES);
        }
        log.warn("登录失败累计: username={}, 第 {} 次失败, 连续 {} 次将锁定", username, attempts != null ? attempts + 1 : 1, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * 登录成功 — 清除失败计数
     *
     * @param username 用户名
     */
    public void loginSucceeded(String username) {
        String key = buildKey(username);
        redisTemplate.delete(key);
    }

    /**
     * 获取剩余锁定时间（秒）
     *
     * @param username 用户名
     * @return 剩余秒数，未锁定时返回 0
     */
    public long getRemainingLockTime(String username) {
        String key = buildKey(username);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        Integer attempts = (Integer) redisTemplate.opsForValue().get(key);
        if (attempts != null && attempts >= DEFAULT_MAX_ATTEMPTS && ttl != null) {
            return ttl;
        }
        return 0;
    }

    private String buildKey(String username) {
        return ATTEMPT_KEY_PREFIX + username;
    }
}
