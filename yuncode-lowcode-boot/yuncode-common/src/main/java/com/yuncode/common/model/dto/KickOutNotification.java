package com.yuncode.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 踢出通知DTO
 * 当用户被管理员踢出时，前端会收到此通知
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KickOutNotification {

    /**
     * 通知类型：kick_out
     */
    private String type;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 倒计时（秒）
     */
    private Integer countdown;

    /**
     * 踢出原因
     */
    private String reason;

    /**
     * 操作时间戳
     */
    private Long timestamp;

    public KickOutNotification(String message, Integer countdown, String reason) {
        this.type = "kick_out";
        this.message = message;
        this.countdown = countdown;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }
}
