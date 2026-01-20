package com.yuncode.system.controller;

import com.yuncode.common.exception.ErrorCode;
import com.yuncode.common.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
     * Sa-Token 会自动从 URL 参数 'token' 中读取并验证 token（已在 SaTokenConfig 中配置）
     *
     * @return SseEmitter
     */
    @GetMapping(produces = "text/event-stream")
    @Operation(summary = "建立 SSE 连接", description = "建立 SSE 长连接，接收实时通知消息")
    public SseEmitter connect() {
        log.info("========================================");
        log.info("收到 SSE 连接请求");

        // Sa-Token 拦截器已经验证过 token，直接从上下文获取用户 ID
        Long userId = UserContextUtil.getUserId();

        if (userId == null) {
            log.warn("❌ 未登录用户尝试建立 SSE 连接");
            throw new com.yuncode.common.exception.BaseException(ErrorCode.UNAUTHORIZED);
        }

        log.info("✅ 用户身份验证成功: userId={}", userId);
        log.info("当前 SSE 连接数: {}", notificationService.getActiveConnections());

        SseEmitter emitter = notificationService.createUserNotification(userId);

        log.info("✅ SSE 连接创建成功: userId={}, 当前连接数={}", userId, notificationService.getActiveConnections());
        log.info("========================================");

        return emitter;
    }
}
