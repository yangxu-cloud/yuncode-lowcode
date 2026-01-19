package com.yuncode.system.dto;

import lombok.Data;

/**
 * 操作日志查询 DTO
 */
@Data
public class OperationLogQueryDTO {

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 20;

    /**
     * 用户名
     */
    private String username;

    /**
     * 模块
     */
    private String module;

    /**
     * 操作
     */
    private String operation;

    /**
     * 状态
     */
    private String status;

    /**
     * 链路追踪ID
     */
    private String traceId;
}
