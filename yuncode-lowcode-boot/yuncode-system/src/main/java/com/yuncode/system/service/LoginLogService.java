package com.yuncode.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.system.entity.SysLoginLog;

import java.time.LocalDateTime;

/**
 * 登录日志服务接口
 */
public interface LoginLogService {

    /**
     * 分页查询登录日志
     *
     * @param page          分页参数
     * @param tenantId      租户ID
     * @param username      用户名
     * @param status        登录状态
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 登录日志分页列表
     */
    IPage<SysLoginLog> listLoginLogs(Page<SysLoginLog> page,
                                     Long tenantId,
                                     String username,
                                     Integer status,
                                     LocalDateTime startTime,
                                     LocalDateTime endTime);

    /**
     * 根据ID查询登录日志
     *
     * @param id 日志ID
     * @return 登录日志
     */
    SysLoginLog getLoginLogById(Long id);

    /**
     * 保存登录日志
     *
     * @param loginLog 登录日志
     */
    void saveLoginLog(SysLoginLog loginLog);

    /**
     * 批量删除登录日志
     *
     * @param ids 日志ID列表
     */
    void deleteLoginLogs(Long[] ids);

    /**
     * 清空登录日志
     */
    void cleanLoginLogs();

    /**
     * 删除指定时间之前的日志
     *
     * @param beforeTime 时间点
     */
    void deleteLoginLogsBefore(LocalDateTime beforeTime);

    /**
     * 更新登出时间
     *
     * @param username    用户名
     * @param logoutTime  登出时间
     */
    void updateLogoutTime(String username, LocalDateTime logoutTime);
}
