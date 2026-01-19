package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuncode.system.entity.SysOperLog;
import com.yuncode.system.mapper.SysOperLogMapper;
import com.yuncode.system.service.OperLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志服务实现
 */
@Service
public class OperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements OperLogService {

    @Override
    public IPage<SysOperLog> listOperLogs(Page<SysOperLog> page,
                                          Long tenantId,
                                          String module,
                                          Integer businessType,
                                          String operName,
                                          Integer status,
                                          LocalDateTime startTime,
                                          LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();

        // 租户ID过滤
        wrapper.eq(tenantId != null, SysOperLog::getTenantId, tenantId);

        // 模块过滤
        wrapper.like(module != null && !module.isEmpty(), SysOperLog::getModule, module);

        // 业务类型过滤
        wrapper.eq(businessType != null, SysOperLog::getBusinessType, businessType);

        // 操作人员过滤
        wrapper.like(operName != null && !operName.isEmpty(), SysOperLog::getOperName, operName);

        // 操作状态过滤
        wrapper.eq(status != null, SysOperLog::getStatus, status);

        // 时间范围过滤
        if (startTime != null && endTime != null) {
            wrapper.between(SysOperLog::getOperTime, startTime, endTime);
        } else if (startTime != null) {
            wrapper.ge(SysOperLog::getOperTime, startTime);
        } else if (endTime != null) {
            wrapper.le(SysOperLog::getOperTime, endTime);
        }

        // 按操作时间倒序
        wrapper.orderByDesc(SysOperLog::getOperTime);

        return page(page, wrapper);
    }

    @Override
    public SysOperLog getOperLogById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public void saveOperLog(SysOperLog operLog) {
        baseMapper.insert(operLog);
    }

    @Override
    public void deleteOperLogs(Long[] ids) {
        if (ids != null && ids.length > 0) {
            for (Long id : ids) {
                baseMapper.deleteById(id);
            }
        }
    }

    @Override
    public void cleanOperLogs() {
        // 删除所有日志
        baseMapper.delete(null);
    }

    @Override
    public void deleteOperLogsBefore(LocalDateTime beforeTime) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysOperLog::getOperTime, beforeTime);
        baseMapper.delete(wrapper);
    }
}
