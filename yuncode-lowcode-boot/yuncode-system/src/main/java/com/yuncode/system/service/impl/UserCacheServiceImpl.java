package com.yuncode.system.service.impl;

import com.yuncode.system.entity.SysUser;
import com.yuncode.system.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户缓存服务实现
 * 使用 Redis 缓存用户基本信息
 */
@RequiredArgsConstructor
@Service
public class UserCacheServiceImpl implements UserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_KEY_PREFIX = "user:info:";
    private static final long DEFAULT_CACHE_TIMEOUT = 1800; // 默认30分钟

    @Override
    public void cacheUser(Long userId, SysUser user, long timeout) {
        String key = USER_CACHE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, user, timeout, TimeUnit.SECONDS);
    }

    @Override
    public SysUser getCachedUser(Long userId) {
        String key = USER_CACHE_KEY_PREFIX + userId;
        return (SysUser) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void evictUserCache(Long userId) {
        String key = USER_CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    @Override
    public void updateUserCache(Long userId, SysUser user) {
        // 更新缓存时使用默认过期时间
        cacheUser(userId, user, DEFAULT_CACHE_TIMEOUT);
    }
}
