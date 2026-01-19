package com.yuncode.system.service;

import com.yuncode.system.entity.SysUser;

/**
 * 用户缓存服务接口
 */
public interface UserCacheService {

    /**
     * 缓存用户信息
     *
     * @param userId 用户ID
     * @param user 用户信息
     * @param timeout 过期时间（秒）
     */
    void cacheUser(Long userId, SysUser user, long timeout);

    /**
     * 获取缓存的用户信息
     *
     * @param userId 用户ID
     * @return 用户信息，如果不存在返回 null
     */
    SysUser getCachedUser(Long userId);

    /**
     * 删除用户缓存
     *
     * @param userId 用户ID
     */
    void evictUserCache(Long userId);

    /**
     * 更新用户缓存
     *
     * @param userId 用户ID
     * @param user 用户信息
     */
    void updateUserCache(Long userId, SysUser user);
}
