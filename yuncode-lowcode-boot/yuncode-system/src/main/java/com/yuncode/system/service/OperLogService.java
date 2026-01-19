package com.yuncode.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.system.entity.SysOperLog;

import java.time.LocalDateTime;

/**
 * 操作日志服务接口
 */
public interface OperLogService {

    /**
     * 分页查询操作日志
     *
     * @param page          分页参数
     * @param tenantId      租户ID
     * @param module        模块名称
     * @param businessType  业务类型
     * @param operName      操作人员
     * @param status        操作状态
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 操作日志分页列表
     */
    IPage<SysOperLog> listOperLogs(Page<SysOperLog> page,
                                   Long tenantId,
                                   String module,
                                   Integer businessType,
                                   String operName,
                                   Integer status,
                                   LocalDateTime startTime,
                                   LocalDateTime endTime);

    /**
     * 根据ID查询操作日志
     *
     * @param id 日志ID
     * @return 操作日志
     */
    SysOperLog getOperLogById(Long id);

    /**
     * 保存操作日志
     *
     * @param operLog 操作日志
     */
    void saveOperLog(SysOperLog operLog);

    /**
     * 批量删除操作日志
     *
     * @param ids 日志ID列表
     */
    void deleteOperLogs(Long[] ids);

    /**
     * 清空操作日志
     */
    void cleanOperLogs();

    /**
     * 删除指定时间之前的日志
     *
     * @param beforeTime 时间点
     */
    void deleteOperLogsBefore(LocalDateTime beforeTime);
}
