package com.yuncode.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.system.entity.OnlineUser;
import com.yuncode.system.service.NotificationService;
import com.yuncode.system.service.OnlineUserService;
import com.yuncode.system.service.SysLoginLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 在线用户服务实现
 * 使用 Redis 存储在线用户信息
 */
@Slf4j
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SysLoginLogService loginLogService;

    @Autowired
    private NotificationService notificationService;

    public OnlineUserServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            SysLoginLogService loginLogService) {
        this.redisTemplate = redisTemplate;
        this.loginLogService = loginLogService;
    }

    private static final String ONLINE_USER_KEY_PREFIX = "online_user:";
    private static final int IDLE_THRESHOLD_MINUTES = 30;

    @Override
    public void addOnlineUser(String token, OnlineUser onlineUser) {
        onlineUser.setLoginTime(LocalDateTime.now());
        onlineUser.setLastAccessTime(LocalDateTime.now());
        onlineUser.setStatus("active");
        onlineUser.setSessionId(token);

        // 存储到 Redis，7天过期
        String key = ONLINE_USER_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, onlineUser, 7, TimeUnit.DAYS);
    }

    @Override
    public void removeOnlineUser(String token) {
        String key = ONLINE_USER_KEY_PREFIX + token;
        redisTemplate.delete(key);
    }

    @Override
    public OnlineUser getOnlineUser(String token) {
        String key = ONLINE_USER_KEY_PREFIX + token;
        return (OnlineUser) redisTemplate.opsForValue().get(key);
    }

    @Override
    public List<OnlineUser> getAllOnlineUsers() {
        try {
            log.debug("开始获取在线用户列表");
            Set<String> keys = redisTemplate.keys(ONLINE_USER_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.debug("Redis中没有在线用户数据");
                return new ArrayList<>();
            }

            log.debug("找到{}个在线用户记录", keys.size());
            List<OnlineUser> onlineUsers = new ArrayList<>();

            for (String key : keys) {
                try {
                    OnlineUser user = (OnlineUser) redisTemplate.opsForValue().get(key);
                    if (user != null && user.getSessionId() != null) {
                        // 验证 token 是否仍然有效（用户是否还在线）
                        String token = key.substring(ONLINE_USER_KEY_PREFIX.length());

                        try {
                            // 尝试通过 token 获取 loginId，如果失败说明 token 已失效
                            Object loginId = StpUtil.getLoginIdByToken(token);
                            if (loginId != null) {
                                // Token 有效，添加到在线用户列表
                                onlineUsers.add(user);
                            } else {
                                // Token 已失效，清理 Redis 中的过期数据
                                log.debug("Token 已失效，清理在线用户记录: token={}, username={}",
                                    token.substring(0, Math.min(20, token.length())) + "...", user.getUsername());
                                redisTemplate.delete(key);
                            }
                        } catch (Exception e) {
                            // Token 验证失败，说明已失效，清理 Redis 中的过期数据
                            log.debug("Token 验证失败，清理在线用户记录: token={}, error={}",
                                token.substring(0, Math.min(20, token.length())) + "...", e.getMessage());
                            redisTemplate.delete(key);
                        }
                    }
                } catch (Exception e) {
                    log.error("处理在线用户记录失败: key={}, error={}", key, e.getMessage());
                }
            }

            log.debug("有效在线用户数量: {}", onlineUsers.size());
            return onlineUsers;
        } catch (Exception e) {
            log.error("获取在线用户列表失败", e);
            // Redis连接失败时返回空列表，避免系统异常
            return new ArrayList<>();
        }
    }

    @Override
    public Page<OnlineUser> listOnlineUsers(Page<OnlineUser> page, String username, Long tenantId) {
        List<OnlineUser> allUsers = getAllOnlineUsers();

        // 过滤
        List<OnlineUser> filteredUsers = new ArrayList<>();
        for (OnlineUser user : allUsers) {
            boolean match = true;

            if (username != null && !username.isEmpty()) {
                match = user.getUsername() != null && user.getUsername().contains(username);
            }

            if (match && tenantId != null) {
                match = tenantId.equals(user.getTenantId());
            }

            if (match) {
                filteredUsers.add(user);
            }
        }

        // 分页
        int fromIndex = (int) ((page.getCurrent() - 1) * page.getSize());
        int toIndex = Math.min(fromIndex + (int) page.getSize(), filteredUsers.size());

        if (fromIndex >= filteredUsers.size()) {
            page.setRecords(new ArrayList<>());
        } else {
            page.setRecords(filteredUsers.subList(fromIndex, toIndex));
        }

        page.setTotal(filteredUsers.size());

        return page;
    }

    @Override
    public void kickOutUser(String token) {
        log.info("开始踢出用户，token: {}", token.substring(0, Math.min(20, token.length())) + "...");

        // 先获取被踢出用户的信息，用于记录日志和发送通知
        OnlineUser kickedUser = getOnlineUser(token);
        String kickedUsername = kickedUser != null ? kickedUser.getUsername() : "unknown";
        Long kickedUserId = kickedUser != null ? kickedUser.getUserId() : null;

        // 异步执行踢出操作，不阻塞管理员界面
        performKickOutAsync(token, kickedUsername, kickedUserId);

        log.info("踢出指令已发送，token: {}, username={}",
                token.substring(0, Math.min(20, token.length())) + "...", kickedUsername);
    }

    /**
     * 异步执行踢出操作
     * 包括发送通知、等待倒计时、踢出用户、关闭连接等
     */
    private void performKickOutAsync(String token, String kickedUsername, Long kickedUserId) {
        // 使用异步线程执行踢出逻辑，避免阻塞管理员界面
        new Thread(() -> {
            try {
                // 获取 token 对应的 loginId
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId != null) {
                    log.info("[异步踢出] 找到 token 对应的用户 ID: {}", loginId);

                    // 1. 先发送踢出通知（给用户5秒倒计时）
                    if (kickedUserId != null) {
                        try {
                            log.info("[异步踢出] 准备发送踢出通知: userId={}, 当前SSE连接数={}", kickedUserId, notificationService.getActiveConnections());
                            boolean isOnline = notificationService.isUserOnline(kickedUserId);
                            log.info("[异步踢出] 用户 SSE 在线状态: userId={}, isOnline={}", kickedUserId, isOnline);

                            notificationService.sendKickOutNotification(kickedUserId, "被管理员踢出", 5);
                            log.info("[异步踢出] 已发送踢出通知: userId={}, 倒计时=5秒", kickedUserId);
                        } catch (Exception e) {
                            log.error("[异步踢出] 发送踢出通知失败: userId={}, error={}", kickedUserId, e.getMessage(), e);
                            // 通知失败不影响踢出操作
                        }
                    } else {
                        log.warn("[异步踢出] 无法获取 userId，跳过发送踢出通知");
                    }

                    // 等待5秒，让用户看到通知
                    try {
                        Thread.sleep(5000);
                        log.info("[异步踢出] 倒计时结束，准备执行踢出");
                    } catch (InterruptedException e) {
                        log.warn("[异步踢出] 等待倒计时被中断");
                        Thread.currentThread().interrupt();
                    }

                    // 2. 踢出登录（使 token 失效）
                    StpUtil.kickout(loginId);
                    log.info("[异步踢出] 已踢出用户: loginId={}", loginId);

                    // 3. 关闭用户的 SSE 连接
                    if (kickedUserId != null) {
                        notificationService.closeUserConnection(kickedUserId);
                        log.info("[异步踢出] 已关闭用户 SSE 连接: userId={}", kickedUserId);
                    }

                } else {
                    log.warn("[异步踢出] 无法从 token 获取 loginId，尝试使用 StpUtil.kickoutByTokenValue");
                    StpUtil.kickoutByTokenValue(token);
                    log.info("[异步踢出] 使用 StpUtil.kickoutByTokenValue 踢出成功");
                }

                // 4. 记录被踢出用户的登出时间
                try {
                    loginLogService.updateLogoutTime(kickedUsername, LocalDateTime.now());
                    log.info("[异步踢出] 已记录用户登出时间: username={}", kickedUsername);
                } catch (Exception e) {
                    log.error("[异步踢出] 记录登出时间失败: username={}, error={}", kickedUsername, e.getMessage());
                }

                // 5. 删除在线用户记录
                removeOnlineUser(token);

                log.info("[异步踢出] 踢出流程完成，token: {}, username={}",
                        token.substring(0, Math.min(20, token.length())) + "...", kickedUsername);

            } catch (Exception e) {
                log.error("[异步踢出] 踢出用户失败: {}", e.getMessage(), e);
            }
        }, "KickOut-Thread-" + kickedUsername).start();
    }

    @Override
    public void batchKickOutUsers(List<String> tokens) {
        for (String token : tokens) {
            kickOutUser(token);
        }
    }

    @Override
    public Map<String, Object> getOnlineUserStats() {
        try {
            log.debug("开始获取在线用户统计");
            List<OnlineUser> allUsers = getAllOnlineUsers();

            int total = allUsers.size();
            int active = 0;
            int idle = 0;

            LocalDateTime threshold = LocalDateTime.now().minusMinutes(IDLE_THRESHOLD_MINUTES);

            for (OnlineUser user : allUsers) {
                if (user.getLastAccessTime() == null) {
                    continue;
                }
                if (user.getLastAccessTime().isAfter(threshold)) {
                    active++;
                } else {
                    idle++;
                    user.setStatus("idle");
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("active", active);
            stats.put("idle", idle);

            log.debug("在线用户统计: total={}, active={}, idle={}", total, active, idle);
            return stats;
        } catch (Exception e) {
            log.error("获取在线用户统计失败", e);
            // 返回默认统计数据
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", 0);
            stats.put("active", 0);
            stats.put("idle", 0);
            return stats;
        }
    }

    @Override
    public void updateLastAccessTime(String token) {
        OnlineUser user = getOnlineUser(token);
        if (user != null) {
            user.updateLastAccessTime();

            String key = ONLINE_USER_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(key, user, 7, TimeUnit.DAYS);
        }
    }

    @Override
    public void cleanIdleUsers(int idleMinutes) {
        List<OnlineUser> allUsers = getAllOnlineUsers();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(idleMinutes);

        for (OnlineUser user : allUsers) {
            if (user.getLastAccessTime().isBefore(threshold)) {
                // 踢出闲置用户
                kickOutUser(user.getSessionId());
            }
        }
    }
}
