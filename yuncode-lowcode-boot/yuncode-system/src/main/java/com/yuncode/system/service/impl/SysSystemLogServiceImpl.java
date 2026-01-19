package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuncode.system.entity.SysSystemLog;
import com.yuncode.system.mapper.SysSystemLogMapper;
import com.yuncode.system.service.SysSystemLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 系统日志服务实现
 */
@Service
public class SysSystemLogServiceImpl extends ServiceImpl<SysSystemLogMapper, SysSystemLog> implements SysSystemLogService {

    @Override
    public Page<SysSystemLog> listLogs(Page<SysSystemLog> page, String level, String module,
                                       String message, String startTime, String endTime, String traceId) {
        LambdaQueryWrapper<SysSystemLog> wrapper = new LambdaQueryWrapper<>();

        // 按级别查询
        if (StringUtils.isNotBlank(level)) {
            wrapper.eq(SysSystemLog::getLevel, level);
        }

        // 按模块查询
        if (StringUtils.isNotBlank(module)) {
            wrapper.like(SysSystemLog::getModule, module);
        }

        // 按消息查询
        if (StringUtils.isNotBlank(message)) {
            wrapper.like(SysSystemLog::getMessage, message);
        }

        // 按链路追踪ID查询
        if (StringUtils.isNotBlank(traceId)) {
            wrapper.eq(SysSystemLog::getTraceId, traceId);
        }

        // 按时间范围查询
        if (StringUtils.isNotBlank(startTime)) {
            wrapper.ge(SysSystemLog::getCreatedAt, startTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            wrapper.le(SysSystemLog::getCreatedAt, endTime);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(SysSystemLog::getCreatedAt);

        return page(page, wrapper);
    }
}
