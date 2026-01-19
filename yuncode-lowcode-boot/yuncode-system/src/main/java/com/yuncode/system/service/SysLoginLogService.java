package com.yuncode.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.common.utils.web.ServletUtils;
import com.yuncode.common.utils.web.TraceIdContext;
import com.yuncode.common.utils.web.UserAgentUtils;
import com.yuncode.system.entity.SysLoginLog;
import com.yuncode.system.enums.LoginStatus;
import com.yuncode.system.mapper.SysLoginLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLoginLogService {

    private final SysLoginLogMapper loginLogMapper;

    /**
     * 记录登录日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginLog(Long tenantId, String username, Integer status, String msg,
                               HttpServletRequest request, Long costTime) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setTenantId(tenantId);
        loginLog.setUsername(username);
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setIpaddr(ServletUtils.getClientIP(request));
        loginLog.setLoginLocation(UserAgentUtils.getLocationByIP(loginLog.getIpaddr()));

        String userAgent = ServletUtils.getUserAgent(request);
        loginLog.setBrowser(UserAgentUtils.parseBrowser(userAgent));
        loginLog.setOs(UserAgentUtils.parseOs(userAgent));

        loginLog.setStatus(status);
        loginLog.setMsg(msg);
        loginLog.setCostTime(costTime);

        // 设置链路追踪信息
        loginLog.setTraceId(TraceIdContext.getTraceId());
        loginLog.setSpanId(TraceIdContext.getSpanId());
        loginLog.setParentSpanId(TraceIdContext.getParentSpanId());

        loginLogMapper.insert(loginLog);

        log.info("登录日志记录成功: username={}, status={}, ip={}, traceId={}",
                username, status == 0 ? "成功" : "失败", loginLog.getIpaddr(), loginLog.getTraceId());
    }

    /**
     * 根据用户名和租户ID查询最新登录日志
     */
    public SysLoginLog getLatestLoginLog(String username, Long tenantId) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysLoginLog::getUsername, username)
                .eq(SysLoginLog::getTenantId, tenantId)
                .orderByDesc(SysLoginLog::getLoginTime)
                .last("LIMIT 1");
        return loginLogMapper.selectOne(wrapper);
    }

    /**
     * 根据用户名和租户ID查询登录日志列表
     */
    public List<SysLoginLog> getLoginHistory(String username, Long tenantId, Integer limit) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysLoginLog::getUsername, username)
                .eq(SysLoginLog::getTenantId, tenantId)
                .orderByDesc(SysLoginLog::getLoginTime)
                .last("LIMIT " + (limit != null ? limit : 10));
        return loginLogMapper.selectList(wrapper);
    }

    /**
     * 更新登出时间
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLogoutTime(String username, LocalDateTime logoutTime) {
        // 查找最新的未登出的登录记录
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysLoginLog::getUsername, username)
                .isNull(SysLoginLog::getLogoutTime)  // 未登出的记录
                .orderByDesc(SysLoginLog::getLoginTime)
                .last("LIMIT 1");

        SysLoginLog loginLog = loginLogMapper.selectOne(wrapper);
        if (loginLog != null) {
            loginLog.setLogoutTime(logoutTime);
            loginLogMapper.updateById(loginLog);
            log.info("更新登出时间成功: username={}, logoutTime={}", username, logoutTime);
        } else {
            log.warn("未找到未登出的登录记录: username={}", username);
        }
    }
}
