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
     * Key: userId, Value: SseEmitter
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
     * @return SseEmitter
     */
    public SseEmitter createUserNotification(Long userId) {
        // 30分钟超时
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        String userIdKey = userId.toString();

        // 移除旧连接（如果存在）
        SseEmitter oldEmitter = userEmitters.get(userIdKey);
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (Exception e) {
                log.warn("关闭旧 SSE 连接失败: userId={}", userId, e);
            }
        }

        // 保存新连接
        userEmitters.put(userIdKey, emitter);

        // 设置连接完成和超时回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成: userId={}", userId);
            userEmitters.remove(userIdKey);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 连接超时: userId={}", userId);
            userEmitters.remove(userIdKey);
            emitter.complete();
        });

        emitter.onError((ex) -> {
            log.error("SSE 连接错误: userId={}", userId, ex);
            userEmitters.remove(userIdKey);
        });

        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 连接已建立"));
        } catch (IOException e) {
            log.error("发送连接成功消息失败: userId={}", userId, e);
        }

        log.info("创建 SSE 连接成功: userId={}, 当前连接数={}", userId, userEmitters.size());
        return emitter;
    }

    /**
     * 发送踢出通知（带倒计时）
     *
     * @param userId  用户ID
     * @param reason  踢出原因
     * @param seconds 倒计时秒数
     */
    public void sendKickOutNotification(Long userId, String reason, int seconds) {
        String userIdKey = userId.toString();
        SseEmitter emitter = userEmitters.get(userIdKey);

        if (emitter == null) {
            log.warn("用户 SSE 连接不存在，无法发送踢出通知: userId={}", userId);
            return;
        }

        log.info("开始发送踢出通知: userId={}, reason={}, countdown={}秒", userId, reason, seconds);

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
            log.info("踢出通知已发送: userId={}, countdown={}秒", userId, seconds);
        } catch (IOException e) {
            log.error("发送踢出通知失败: userId={}", userId, e);
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
                    log.error("发送倒计时结束消息失败: userId={}", userId, e);
                }
                // 完成连接
                emitter.complete();
                log.info("踢出倒计时结束，连接已关闭: userId={}", userId);
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
                    log.error("发送倒计时更新失败: userId={}, countdown={}", userId, countdown[0], e);
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
     * @param userId 用户ID
     */
    public void closeUserConnection(Long userId) {
        String userIdKey = userId.toString();
        SseEmitter emitter = userEmitters.remove(userIdKey);

        if (emitter != null) {
            try {
                emitter.complete();
                log.info("SSE 连接已关闭: userId={}", userId);
            } catch (Exception e) {
                log.error("关闭 SSE 连接失败: userId={}", userId, e);
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
        return userEmitters.containsKey(userId.toString());
    }
}
