package com.yuncode.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.entity.SysUser;
import com.yuncode.system.mapper.SysUserOrgMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务扩展
 * 处理用户缓存相关的业务逻辑
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final com.yuncode.system.mapper.SysUserMapper sysUserMapper;
    private final UserCacheService userCacheService;
    private final SysUserOrgMapper sysUserOrgMapper;

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(SysUser user) {
        // 获取目标租户ID：优先使用 user 对象中的 tenantId，否则使用 session 中的 tenantId
        Long targetTenantId = user.getTenantId();

        // 如果 user 对象中没有 tenantId，从 session 获取当前租户ID
        if (targetTenantId == null) {
            try {
                targetTenantId = StpUtil.getSession().get("tenantId", 0L);
            } catch (Exception e) {
                throw new RuntimeException("无法获取租户ID，请指定用户所属租户");
            }
            if (targetTenantId == null || targetTenantId == 0) {
                throw new RuntimeException("无法获取租户ID，请指定用户所属租户");
            }
            // 设置到 user 对象，以便后续插入时使用
            user.setTenantId(targetTenantId);
        }

        // 检查用户名是否已存在（在目标租户中）
        // 使用 selectByUsernameAndTenantId 方法，忽略租户插件，手动指定租户ID
        // 这样可以确保：不同租户可以有相同的用户名，但同一租户内用户名唯一
        SysUser existingUser = sysUserMapper.selectByUsernameAndTenantId(user.getUsername(), targetTenantId);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 设置密码（如果没有设置密码，使用默认密码）
        String plainPassword = (user.getPassword() == null || user.getPassword().isEmpty())
            ? "123456"  // 默认密码
            : user.getPassword();

        // 加密密码
        user.setPassword(BCrypt.hashpw(plainPassword));

        // 设置默认状态
        if (user.getStatus() == null) {
            user.setStatus(0); // 0-正常
        }

        // 设置逻辑删除标识（必须设置为0，否则查询时会被过滤）
        if (user.getDeleted() == null) {
            user.setDeleted(0); // 0-未删除
        }

        // 保存用户（多租户插件会自动填充 tenant_id）
        sysUserMapper.insert(user);

        // 缓存用户信息
        userCacheService.cacheUser(user.getId(), user, 1800);

        return user.getId();
    }

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    public SysUser getUserByUsername(String username) {
        // 多租户插件会自动添加 tenant_id 条件
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return sysUserMapper.selectOne(wrapper);
    }

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        // 检查用户是否存在（使用 IgnoreTenant 绕过多租户限制）
        SysUser user = sysUserMapper.selectByIdIgnoreTenant(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新状态
        user.setStatus(status);
        sysUserMapper.updateByIdIgnoreTenant(user);

        // 清除缓存
        userCacheService.evictUserCache(userId);
    }

    /**
     * 更新用户基本信息
     *
     * @param user 用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(SysUser user) {
        if (user.getId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        // 检查用户是否存在（使用 IgnoreTenant 绕过多租户限制）
        SysUser existingUser = sysUserMapper.selectByIdIgnoreTenant(user.getId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 只更新允许修改的字段
        SysUser updateUser = new SysUser();
        updateUser.setId(user.getId());
        if (user.getNickname() != null) {
            updateUser.setNickname(user.getNickname());
        }
        if (user.getRealName() != null) {
            updateUser.setRealName(user.getRealName());
        }
        if (user.getEmail() != null) {
            updateUser.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            updateUser.setPhone(user.getPhone());
        }
        if (user.getGender() != null) {
            updateUser.setGender(user.getGender());
        }
        if (user.getAvatar() != null) {
            updateUser.setAvatar(user.getAvatar());
        }

        // 更新用户信息（使用 IgnoreTenant 绕过多租户限制）
        sysUserMapper.updateByIdIgnoreTenant(updateUser);

        // 清除缓存
        userCacheService.evictUserCache(user.getId());
    }

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
