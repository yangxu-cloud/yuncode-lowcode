package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuncode.system.entity.SysLoginLog;
import com.yuncode.system.mapper.SysLoginLogMapper;
import com.yuncode.system.service.LoginLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录日志服务实现
 */
@Service
public class LoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements LoginLogService {

    @Override
    public IPage<SysLoginLog> listLoginLogs(Page<SysLoginLog> page,
                                            Long tenantId,
                                            String username,
                                            Integer status,
                                            LocalDateTime startTime,
                                            LocalDateTime endTime) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();

        // 租户ID过滤
        wrapper.eq(tenantId != null, SysLoginLog::getTenantId, tenantId);

        // 用户名过滤
        wrapper.like(username != null && !username.isEmpty(), SysLoginLog::getUsername, username);

        // 登录状态过滤
        wrapper.eq(status != null, SysLoginLog::getStatus, status);

        // 时间范围过滤
        if (startTime != null && endTime != null) {
            wrapper.between(SysLoginLog::getLoginTime, startTime, endTime);
        } else if (startTime != null) {
            wrapper.ge(SysLoginLog::getLoginTime, startTime);
        } else if (endTime != null) {
            wrapper.le(SysLoginLog::getLoginTime, endTime);
        }

        // 按登录时间倒序
        wrapper.orderByDesc(SysLoginLog::getLoginTime);

        return page(page, wrapper);
    }

    @Override
    public SysLoginLog getLoginLogById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public void saveLoginLog(SysLoginLog loginLog) {
        baseMapper.insert(loginLog);
    }

    @Override
    public void deleteLoginLogs(Long[] ids) {
        if (ids != null && ids.length > 0) {
            for (Long id : ids) {
                baseMapper.deleteById(id);
            }
        }
    }

    @Override
    public void cleanLoginLogs() {
        // 删除所有日志
        baseMapper.delete(null);
    }

    @Override
    public void deleteLoginLogsBefore(LocalDateTime beforeTime) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysLoginLog::getLoginTime, beforeTime);
        baseMapper.delete(wrapper);
    }

    @Override
    public void updateLogoutTime(String username, LocalDateTime logoutTime) {
        // 更新最近一次未登出的登录记录
        LambdaUpdateWrapper<SysLoginLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysLoginLog::getUsername, username)
                .isNull(SysLoginLog::getLogoutTime)
                .orderByDesc(SysLoginLog::getLoginTime)
                .last("LIMIT 1");

        wrapper.set(SysLoginLog::getLogoutTime, logoutTime);
        baseMapper.update(null, wrapper);
    }
}
