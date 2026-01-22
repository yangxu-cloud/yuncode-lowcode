package com.yuncode.system.service;

import com.yuncode.common.model.dto.KickOutNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * SSE 通知服务
 * 管理用户 SSE 连接和消息推送
 */
@Slf4j
@Service
public class NotificationService {

    /**
     * 存储用户的 SSE 连接
     * Key: userId-sessionId, Value: SseEmitter
     * 使用 userId+sessionId 作为 key，支持同一用户多个会话同时在线
     */
    private final Map<String, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    /**
     * 定时任务线程池，用于发送倒计时更新
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 为用户创建 SSE 连接
     *
     * @param userId 用户ID
     * @param sessionId 业务会话ID
     * @return SseEmitter
     */
    public SseEmitter createUserNotification(Long userId, String sessionId) {
        // 30分钟超时
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 使用 userId+sessionId 作为 key，支持同一用户多个会话同时在线
        String userKey = userId + "-" + sessionId;

        // 不再移除旧连接，允许同一用户多个会话
        // 保存新连接
        userEmitters.put(userKey, emitter);

        // 设置连接完成和超时回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成: userId={}, sessionId={}", userId, sessionId);
            userEmitters.remove(userKey);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 连接超时: userId={}, sessionId={}", userId, sessionId);
            userEmitters.remove(userKey);
            emitter.complete();
        });

        emitter.onError((ex) -> {
            log.error("SSE 连接错误: userId={}, sessionId={}", userId, sessionId, ex);
            userEmitters.remove(userKey);
        });

        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 连接已建立"));
        } catch (IOException e) {
            log.error("发送连接成功消息失败: userId={}, sessionId={}", userId, sessionId, e);
        }

        log.info("创建 SSE 连接成功: userId={}, sessionId={}, 当前连接数={}", userId, sessionId, userEmitters.size());
        return emitter;
    }

    /**
     * 发送踢出通知（带倒计时）
     *
     * @param userId    用户ID
     * @param sessionId 业务会话ID（精确指定要通知的会话）
     * @param reason    踢出原因
     * @param seconds   倒计时秒数
     */
    public void sendKickOutNotification(Long userId, String sessionId, String reason, int seconds) {
        // 使用 userId+sessionId 作为 key，精确发送通知到指定会话
        String userKey = userId + "-" + sessionId;
        SseEmitter emitter = userEmitters.get(userKey);

        if (emitter == null) {
            log.warn("用户 SSE 连接不存在，无法发送踢出通知: userId={}, sessionId={}", userId, sessionId);
            return;
        }

        log.info("开始发送踢出通知: userId={}, sessionId={}, reason={}, countdown={}秒", userId, sessionId, reason, seconds);

        // 发送初始踢出通知
        KickOutNotification notification = new KickOutNotification(
                "您已被管理员强制下线",
                seconds,
                reason
        );

        try {
            emitter.send(SseEmitter.event()
                    .name("kick_out")
                    .data(notification));
            log.info("踢出通知已发送: userId={}, sessionId={}, countdown={}秒", userId, sessionId, seconds);
        } catch (IOException e) {
            log.error("发送踢出通知失败: userId={}, sessionId={}", userId, sessionId, e);
            return;
        }

        // 启动倒计时任务，每秒更新一次
        final int[] countdown = {seconds};
        scheduler.scheduleAtFixedRate(() -> {
            countdown[0]--;

            if (countdown[0] <= 0) {
                // 倒计时结束，发送最后一条消息
                try {
                    emitter.send(SseEmitter.event()
                            .name("kick_out_final")
                            .data("倒计时结束，即将退出"));
                } catch (IOException e) {
                    log.error("发送倒计时结束消息失败: userId={}, sessionId={}", userId, sessionId, e);
                }
                // 完成连接
                emitter.complete();
                log.info("踢出倒计时结束，连接已关闭: userId={}, sessionId={}", userId, sessionId);
            } else if (countdown[0] <= 5) {
                // 最后5秒，每秒发送一次更新
                try {
                    KickOutNotification update = new KickOutNotification(
                            "您已被管理员强制下线",
                            countdown[0],
                            reason
                    );
                    emitter.send(SseEmitter.event()
                            .name("kick_out_update")
                            .data(update));
                } catch (IOException e) {
                    log.error("发送倒计时更新失败: userId={}, sessionId={}, countdown={}", userId, sessionId, countdown[0], e);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 发送普通通知消息
     *
     * @param userId   用户ID
     * @param message  消息内容
     */
    public void sendMessage(Long userId, String message) {
        String userIdKey = userId.toString();
        SseEmitter emitter = userEmitters.get(userIdKey);

        if (emitter == null) {
            log.warn("用户 SSE 连接不存在: userId={}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(message));
            log.info("消息已发送: userId={}, message={}", userId, message);
        } catch (IOException e) {
            log.error("发送消息失败: userId={}", userId, e);
        }
    }

    /**
     * 关闭指定用户的 SSE 连接
     *
     * @param userId    用户ID
     * @param sessionId 业务会话ID（精确指定要关闭的会话）
     */
    public void closeUserConnection(Long userId, String sessionId) {
        String userKey = userId + "-" + sessionId;
        SseEmitter emitter = userEmitters.remove(userKey);

        if (emitter != null) {
            try {
                emitter.complete();
                log.info("SSE 连接已关闭: userId={}, sessionId={}", userId, sessionId);
            } catch (Exception e) {
                log.error("关闭 SSE 连接失败: userId={}, sessionId={}", userId, sessionId, e);
            }
        }
    }

    /**
     * 获取当前在线 SSE 连接数
     *
     * @return 连接数
     */
    public int getActiveConnections() {
        return userEmitters.size();
    }

    /**
     * 检查用户是否在线（有 SSE 连接）
     *
     * @param userId 用户ID
     * @return true=在线, false=离线
     */
    public boolean isUserOnline(Long userId) {
        // 检查是否有该用户的任意 SSE 连接
        return userEmitters.keySet().stream()
                .anyMatch(key -> key.startsWith(userId + "-"));
    }

    /**
     * 获取用户的所有 SSE 连接数
     *
     * @param userId 用户ID
     * @return 连接数
     */
    public int getUserConnections(Long userId) {
        String prefix = userId + "-";
        return (int) userEmitters.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .count();
    }

    /**
     * 获取所有连接的详细信息（调试用）
     *
     * @return 连接信息列表
     */
    public Map<String, String> getConnectionDetails() {
        Map<String, String> details = new ConcurrentHashMap<>();
        userEmitters.forEach((key, emitter) -> {
            String[] parts = key.split("-", 2);
            if (parts.length == 2) {
                String userId = parts[0];
                String sessionId = parts[1];
                String sessionIdPreview = sessionId.length() > 20 ? sessionId.substring(0, 20) + "..." : sessionId;
                details.put(userId, sessionIdPreview);
            }
        });
        return details;
    }
}
