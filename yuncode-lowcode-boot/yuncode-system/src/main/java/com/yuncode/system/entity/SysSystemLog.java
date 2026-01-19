package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统日志实体
 */
@Data
@TableName("sys_system_log")
public class SysSystemLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 日志级别：TRACE, DEBUG, INFO, WARN, ERROR
     */
    private String level;

    /**
     * 模块
     */
    private String module;

    /**
     * 日志消息
     */
    private String message;

    /**
     * 异常信息
     */
    private String exception;

    /**
     * 堆栈跟踪
     */
    private String stackTrace;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * Span ID
     */
    private String spanId;

    /**
     * 父 Span ID
     */
    private String parentSpanId;

    /**
     * 自定义标签（JSON格式）
     */
    private String tags;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;
}
