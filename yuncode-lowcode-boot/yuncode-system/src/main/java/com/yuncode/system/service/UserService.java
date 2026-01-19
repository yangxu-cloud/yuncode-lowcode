package com.yuncode.system.service;

import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务扩展
 * 处理用户缓存相关的业务逻辑
 */
@Service
public class UserService {

    @Autowired
    private com.yuncode.system.mapper.SysUserMapper sysUserMapper;

    @Autowired
    private UserCacheService userCacheService;

    /**
     * 更新用户信息并清除缓存
     *
     * @param user 用户信息
     * @return 更新结果
     */
    public Result<Void> updateUserWithCacheEvict(SysUser user) {
        // 更新数据库
        int result = sysUserMapper.updateById(user);

        if (result > 0) {
            // 清除缓存，下次查询时会重新加载最新数据
            userCacheService.evictUserCache(user.getId());
            return Result.success();
        } else {
            return Result.error("更新用户信息失败");
        }
    }

    /**
     * 获取用户信息（带缓存）
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public SysUser getUserByIdWithCache(Long userId) {
        // 先从缓存获取
        SysUser user = userCacheService.getCachedUser(userId);

        if (user == null) {
            // 缓存不存在，从数据库查询
            user = sysUserMapper.selectById(userId);

            if (user != null) {
                // 缓存用户信息
                userCacheService.cacheUser(userId, user, 1800);
            }
        }

        return user;
    }
}
