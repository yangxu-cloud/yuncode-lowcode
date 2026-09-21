package com.yuncode.system.iam.service.impl;

import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.iam.entity.SysUser;
import com.yuncode.system.iam.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户缓存服务实现
 * 使用 Redis 缓存用户基本信息。
 * 缓存键格式：user:info:{tenantId}:{userId}，确保多租户隔离。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserCacheServiceImpl implements UserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_KEY_PREFIX = "user:info:";
    private static final long DEFAULT_CACHE_TIMEOUT = 1800; // 默认30分钟

    /**
     * 构建租户安全的缓存键
     */
    private String buildKey(Long userId) {
        Long tenantId = SecurityUtil.getTenantIdOrNull();
        if (tenantId != null) {
            return USER_CACHE_KEY_PREFIX + tenantId + ":" + userId;
        }
        return USER_CACHE_KEY_PREFIX + "global:" + userId;
    }

    @Override
    public void cacheUser(Long userId, SysUser user, long timeout) {
        String key = buildKey(userId);
        redisTemplate.opsForValue().set(key, user, timeout, TimeUnit.SECONDS);
    }

    @Override
    public SysUser getCachedUser(Long userId) {
        String key = buildKey(userId);
        return (SysUser) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void evictUserCache(Long userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
    }

    @Override
    public void updateUserCache(Long userId, SysUser user) {
        cacheUser(userId, user, DEFAULT_CACHE_TIMEOUT);
    }
}
