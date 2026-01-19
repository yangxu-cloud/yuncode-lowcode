package com.yuncode.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.system.entity.OnlineUser;

import java.util.List;
import java.util.Map;

/**
 * 在线用户服务接口
 */
public interface OnlineUserService {

    /**
     * 添加在线用户
     *
     * @param token Token值
     * @param onlineUser 在线用户信息
     */
    void addOnlineUser(String token, OnlineUser onlineUser);

    /**
     * 移除在线用户
     *
     * @param token Token值
     */
    void removeOnlineUser(String token);

    /**
     * 获取在线用户
     *
     * @param token Token值
     * @return 在线用户信息
     */
    OnlineUser getOnlineUser(String token);

    /**
     * 获取所有在线用户
     *
     * @return 在线用户列表
     */
    List<OnlineUser> getAllOnlineUsers();

    /**
     * 分页查询在线用户
     *
     * @param page 分页参数
     * @param username 用户名
     * @param tenantId 租户ID
     * @return 分页结果
     */
    Page<OnlineUser> listOnlineUsers(Page<OnlineUser> page, String username, Long tenantId);

    /**
     * 踢出用户
     *
     * @param token Token值
     */
    void kickOutUser(String token);

    /**
     * 批量踢出用户
     *
     * @param tokens Token列表
     */
    void batchKickOutUsers(List<String> tokens);

    /**
     * 获取在线用户统计
     *
     * @return 统计信息 {total: 总数, active: 活跃数, idle: 闲置数}
     */
    Map<String, Object> getOnlineUserStats();

    /**
     * 更新用户最后访问时间
     *
     * @param token Token值
     */
    void updateLastAccessTime(String token);

    /**
     * 清理闲置用户（超过指定时间未活动）
     *
     * @param idleMinutes 闲置分钟数
     */
    void cleanIdleUsers(int idleMinutes);
}
