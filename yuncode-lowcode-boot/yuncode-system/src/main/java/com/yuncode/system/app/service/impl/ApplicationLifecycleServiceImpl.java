package com.yuncode.system.app.service.impl;

import com.yuncode.common.event.AppLifecycleEvent;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.app.entity.SysApplication;
import com.yuncode.system.app.mapper.SysApplicationMapper;
import com.yuncode.system.app.service.ApplicationLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 应用生命周期管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationLifecycleServiceImpl implements ApplicationLifecycleService {

    private final SysApplicationMapper applicationMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        if (application.getStatus() != null && application.getStatus() == 1) {
            throw new RuntimeException("应用已在运行中");
        }

        application.setStatus(1);
        application.setStartTime(LocalDateTime.now());
        application.setStopTime(null);

        boolean result = applicationMapper.updateById(application) > 0;
        if (result) {
            eventPublisher.publishEvent(new AppLifecycleEvent(id, application.getAppId(), "start"));
            log.info("应用启动事件已发布: appId={}", application.getAppId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        if (application.getStatus() != null && application.getStatus() == 0) {
            throw new RuntimeException("应用未运行");
        }

        application.setStatus(0);
        application.setStopTime(LocalDateTime.now());

        boolean result = applicationMapper.updateById(application) > 0;
        if (result) {
            eventPublisher.publishEvent(new AppLifecycleEvent(id, application.getAppId(), "stop"));
            log.info("应用停止事件已发布: appId={}", application.getAppId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restartApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        // 先停止
        if (application.getStatus() != null && application.getStatus() == 1) {
            application.setStatus(0);
            application.setStopTime(LocalDateTime.now());
            applicationMapper.updateById(application);
        }

        // 再启动
        application.setStatus(1);
        application.setStartTime(LocalDateTime.now());
        application.setStopTime(null);
        boolean result = applicationMapper.updateById(application) > 0;

        if (result) {
            eventPublisher.publishEvent(new AppLifecycleEvent(id, application.getAppId(), "restart"));
            log.info("应用重启事件已发布: appId={}", application.getAppId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        application.setStatus(0);
        boolean result = applicationMapper.updateById(application) > 0;
        if (result) {
            log.info("应用已恢复: id={}", id);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean installApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        eventPublisher.publishEvent(new AppLifecycleEvent(id, application.getAppId(), "start"));
        log.info("应用安装事件已发布: appId={}", application.getAppId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean uninstallApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        eventPublisher.publishEvent(new AppLifecycleEvent(id, application.getAppId(), "uninstall"));
        log.info("应用卸载事件已发布: appId={}", application.getAppId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean upgradeApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        // 更新版本号
        String currentVersion = application.getVersion();
        if (currentVersion != null) {
            String[] parts = currentVersion.split("\\.");
            if (parts.length >= 3) {
                int patch = Integer.parseInt(parts[2]) + 1;
                application.setVersion(parts[0] + "." + parts[1] + "." + patch);
            }
        }

        boolean result = applicationMapper.updateById(application) > 0;
        if (result) {
            eventPublisher.publishEvent(new AppLifecycleEvent(id, application.getAppId(), "start"));
            log.info("应用升级事件已发布: appId={}, version={}", application.getAppId(), application.getVersion());
        }
        return result;
    }
}
