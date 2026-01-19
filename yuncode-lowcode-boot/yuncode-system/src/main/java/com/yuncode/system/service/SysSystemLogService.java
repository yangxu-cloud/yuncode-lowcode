package com.yuncode.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yuncode.system.entity.SysSystemLog;

/**
 * 系统日志服务接口
 */
public interface SysSystemLogService extends IService<SysSystemLog> {

    /**
     * 分页查询系统日志
     *
     * @param page 分页参数
     * @param level 日志级别
     * @param module 模块
     * @param message 消息
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param traceId 链路追踪ID
     * @return 分页结果
     */
    Page<SysSystemLog> listLogs(Page<SysSystemLog> page, String level, String module,
                                String message, String startTime, String endTime, String traceId);
}
