package com.yuncode.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yuncode.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 通知控制器
 * 提供 SSE 连接端点，前端可以通过此接口接收实时通知
 */
@Slf4j
@RestController
@RequestMapping("/user/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "SSE 实时通知接口")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    private final com.yuncode.system.service.NotificationService notificationService;

    /**
     * 建立 SSE 连接
     * 前端调用此接口后，会保持长连接，可以接收服务器推送的实时通知
     *
     * 注意：EventSource 不支持自定义请求头，所以 token 通过 URL 参数传递
     * 同时传递 sessionId 用于标识业务会话
     *
     * @param token Sa-Token JWT（用于身份验证）
     * @param sessionId 业务会话ID（用于 SSE 连接管理）
     * @return SseEmitter
     */
    @GetMapping(produces = "text/event-stream")
    @Operation(summary = "建立 SSE 连接", description = "建立 SSE 长连接，接收实时通知消息")
    public SseEmitter connect(
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        log.info("========================================");
        log.info("收到 SSE 连接请求");

        // 手动验证 token
        if (token == null || token.isEmpty()) {
            log.warn("❌ 未提供 token");
            throw new com.yuncode.common.exception.BaseException(ErrorCode.UNAUTHORIZED);
        }

        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("❌ 未提供 sessionId");
            throw new com.yuncode.common.exception.BaseException(ErrorCode.UNAUTHORIZED);
        }

        try {
            // 使用 Sa-Token 验证 token 并获取登录 ID
            Object loginId = StpUtil.getLoginIdByToken(token);

            if (loginId == null) {
                log.warn("❌ token 无效或已过期");
                throw new com.yuncode.common.exception.BaseException(ErrorCode.UNAUTHORIZED);
            }

            Long userId = Long.valueOf(loginId.toString());

            log.info("✅ 用户身份验证成功: userId={}, sessionId={}", userId, sessionId);
            log.info("当前 SSE 连接数: {}", notificationService.getActiveConnections());

            SseEmitter emitter = notificationService.createUserNotification(userId, sessionId);

            log.info("✅ SSE 连接创建成功: userId={}, sessionId={}, 当前连接数={}", userId, sessionId, notificationService.getActiveConnections());
            log.info("========================================");

            return emitter;

        } catch (Exception e) {
            log.error("❌ SSE 连接失败", e);
            throw new com.yuncode.common.exception.BaseException(ErrorCode.UNAUTHORIZED);
        }
    }
}
