package com.yuncode.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.system.entity.SysSettings;
import com.yuncode.system.mapper.SysSettingsMapper;
import com.yuncode.system.vo.SettingsVO;
import com.yuncode.system.vo.SystemInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统设置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysSettingsService {

    private final SysSettingsMapper settingsMapper;

    /**
     * 获取基础设置
     */
    public SettingsVO getBasicSettings() {
        SettingsVO settingsVO = new SettingsVO();

        // 查询所有基础设置
        LambdaQueryWrapper<SysSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysSettings::getSettingGroup, "basic")
                .eq(SysSettings::getStatus, 0)
                .orderByAsc(SysSettings::getSort);

        List<SysSettings> settingsList = settingsMapper.selectList(wrapper);

        // 填充设置值
        for (SysSettings setting : settingsList) {
            String key = setting.getSettingKey();
            String value = setting.getSettingValue();

            switch (key) {
                case "systemName", "appName" -> settingsVO.setSystemName(value);
                case "appVersion" -> settingsVO.setAppVersion(value);
                case "systemLogo", "appLogo" -> settingsVO.setSystemLogo(value);
                case "systemDescription", "appDescription" -> settingsVO.setSystemDescription(value);
                case "copyright" -> settingsVO.setCopyright(value);
                case "icp" -> settingsVO.setIcp(value);
                case "systemUrl" -> settingsVO.setSystemUrl(value);
                case "language" -> settingsVO.setLanguage(value);
                case "timezone" -> settingsVO.setTimezone(value);
                case "dateFormat" -> settingsVO.setDateFormat(value);
                case "timeFormat" -> settingsVO.setTimeFormat(value);
            }
        }

        // 如果没有数据，设置默认值
        if (settingsVO.getSystemName() == null) {
            settingsVO.setSystemName("Yuncode LowCode");
        }
        if (settingsVO.getAppVersion() == null) {
            settingsVO.setAppVersion("1.0.0");
        }
        if (settingsVO.getSystemDescription() == null) {
            settingsVO.setSystemDescription("云创低代码平台");
        }
        if (settingsVO.getCopyright() == null) {
            settingsVO.setCopyright("© 2024 Yuncode. All rights reserved.");
        }

        log.info("获取基础设置: systemName={}", settingsVO.getSystemName());

        return settingsVO;
    }

    /**
     * 更新基础设置
     */
    public void updateBasicSettings(SettingsVO settingsVO) {
        log.info("更新基础设置: systemName={}", settingsVO.getSystemName());

        // 定义基本设置的键值映射
        Map<String, String> settingsMap = new HashMap<>();
        if (settingsVO.getSystemName() != null) settingsMap.put("systemName", settingsVO.getSystemName());
        if (settingsVO.getAppVersion() != null) settingsMap.put("appVersion", settingsVO.getAppVersion());
        if (settingsVO.getSystemLogo() != null) settingsMap.put("systemLogo", settingsVO.getSystemLogo());
        if (settingsVO.getSystemUrl() != null) settingsMap.put("systemUrl", settingsVO.getSystemUrl());
        if (settingsVO.getSystemDescription() != null) settingsMap.put("systemDescription", settingsVO.getSystemDescription());
        if (settingsVO.getCopyright() != null) settingsMap.put("copyright", settingsVO.getCopyright());
        if (settingsVO.getIcp() != null) settingsMap.put("icp", settingsVO.getIcp());
        if (settingsVO.getLanguage() != null) settingsMap.put("language", settingsVO.getLanguage());
        if (settingsVO.getTimezone() != null) settingsMap.put("timezone", settingsVO.getTimezone());
        if (settingsVO.getDateFormat() != null) settingsMap.put("dateFormat", settingsVO.getDateFormat());
        if (settingsVO.getTimeFormat() != null) settingsMap.put("timeFormat", settingsVO.getTimeFormat());

        // 保存或更新每个设置
        for (Map.Entry<String, String> entry : settingsMap.entrySet()) {
            saveOrUpdateSetting("basic", entry.getKey(), entry.getValue());
        }

        log.info("基础设置更新完成");
    }

    /**
     * 保存或更新单个设置
     */
    private void saveOrUpdateSetting(String group, String key, String value) {
        // 查询是否已存在
        LambdaQueryWrapper<SysSettings> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysSettings::getSettingGroup, group)
                .eq(SysSettings::getSettingKey, key);

        SysSettings existingSetting = settingsMapper.selectOne(queryWrapper);

        if (existingSetting != null) {
            // 更新
            existingSetting.setSettingValue(value);
            settingsMapper.updateById(existingSetting);
            log.debug("更新设置: group={}, key={}, value={}", group, key, value);
        } else {
            // 新增
            SysSettings newSetting = new SysSettings();
            newSetting.setSettingGroup(group);
            newSetting.setSettingKey(key);
            newSetting.setSettingValue(value);
            newSetting.setSettingName(key); // 可以根据key设置更友好的名称
            newSetting.setDataType("string");
            newSetting.setIsSystem(0);
            newSetting.setStatus(0);
            settingsMapper.insert(newSetting);
            log.debug("新增设置: group={}, key={}, value={}", group, key, value);
        }
    }

    /**
     * 根据分组获取设置
     */
    public Map<String, String> getSettingsByGroup(String group) {
        LambdaQueryWrapper<SysSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysSettings::getSettingGroup, group)
                .eq(SysSettings::getStatus, 0);

        List<SysSettings> settingsList = settingsMapper.selectList(wrapper);

        Map<String, String> settings = new HashMap<>();
        for (SysSettings setting : settingsList) {
            settings.put(setting.getSettingKey(), setting.getSettingValue());
        }

        return settings;
    }

    /**
     * 根据键获取设置值
     */
    public String getSettingValue(String group, String key) {
        LambdaQueryWrapper<SysSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysSettings::getSettingGroup, group)
                .eq(SysSettings::getSettingKey, key)
                .eq(SysSettings::getStatus, 0)
                .last("LIMIT 1");

        SysSettings setting = settingsMapper.selectOne(wrapper);
        return setting != null ? setting.getSettingValue() : null;
    }

    /**
     * 获取安全设置
     */
    public SettingsVO getSecuritySettings() {
        SettingsVO settingsVO = new SettingsVO();

        // 查询所有安全设置
        LambdaQueryWrapper<SysSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysSettings::getSettingGroup, "security")
                .eq(SysSettings::getStatus, 0)
                .orderByAsc(SysSettings::getSort);

        List<SysSettings> settingsList = settingsMapper.selectList(wrapper);

        // 填充设置值
        for (SysSettings setting : settingsList) {
            String key = setting.getSettingKey();
            String value = setting.getSettingValue();

            switch (key) {
                // 密码策略
                case "passwordMinLength" -> settingsVO.setPasswordMinLength(value != null ? Integer.parseInt(value) : 8);
                case "passwordRequireUppercase" -> settingsVO.setPasswordRequireUppercase(Boolean.parseBoolean(value));
                case "passwordRequireLowercase" -> settingsVO.setPasswordRequireLowercase(Boolean.parseBoolean(value));
                case "passwordRequireNumber" -> settingsVO.setPasswordRequireNumber(Boolean.parseBoolean(value));
                case "passwordRequireSpecial" -> settingsVO.setPasswordRequireSpecial(Boolean.parseBoolean(value));
                case "passwordExpireDays" -> settingsVO.setPasswordExpireDays(value != null ? Integer.parseInt(value) : 90);
                // 登录策略
                case "loginMaxAttempts" -> settingsVO.setLoginMaxAttempts(value != null ? Integer.parseInt(value) : 5);
                case "loginLockDuration" -> settingsVO.setLoginLockDuration(value != null ? Integer.parseInt(value) : 30);
                case "loginSessionTimeout" -> settingsVO.setLoginSessionTimeout(value != null ? Integer.parseInt(value) : 30);
                case "loginEnableCaptcha" -> settingsVO.setLoginEnableCaptcha(Boolean.parseBoolean(value));
            }
        }

        // 设置默认值
        if (settingsVO.getPasswordMinLength() == null) {
            settingsVO.setPasswordMinLength(8);
        }
        if (settingsVO.getPasswordRequireUppercase() == null) {
            settingsVO.setPasswordRequireUppercase(true);
        }
        if (settingsVO.getPasswordRequireLowercase() == null) {
            settingsVO.setPasswordRequireLowercase(true);
        }
        if (settingsVO.getPasswordRequireNumber() == null) {
            settingsVO.setPasswordRequireNumber(true);
        }
        if (settingsVO.getPasswordRequireSpecial() == null) {
            settingsVO.setPasswordRequireSpecial(false);
        }
        if (settingsVO.getPasswordExpireDays() == null) {
            settingsVO.setPasswordExpireDays(90);
        }
        if (settingsVO.getLoginMaxAttempts() == null) {
            settingsVO.setLoginMaxAttempts(5);
        }
        if (settingsVO.getLoginLockDuration() == null) {
            settingsVO.setLoginLockDuration(30);
        }
        if (settingsVO.getLoginSessionTimeout() == null) {
            settingsVO.setLoginSessionTimeout(30);
        }
        if (settingsVO.getLoginEnableCaptcha() == null) {
            settingsVO.setLoginEnableCaptcha(false);
        }

        log.info("获取安全设置");

        return settingsVO;
    }

    /**
     * 更新安全设置
     */
    public void updateSecuritySettings(SettingsVO settingsVO) {
        log.info("更新安全设置");

        // 定义安全设置的键值映射
        Map<String, String> settingsMap = new HashMap<>();

        // 密码策略
        if (settingsVO.getPasswordMinLength() != null) {
            settingsMap.put("passwordMinLength", String.valueOf(settingsVO.getPasswordMinLength()));
        }
        if (settingsVO.getPasswordRequireUppercase() != null) {
            settingsMap.put("passwordRequireUppercase", String.valueOf(settingsVO.getPasswordRequireUppercase()));
        }
        if (settingsVO.getPasswordRequireLowercase() != null) {
            settingsMap.put("passwordRequireLowercase", String.valueOf(settingsVO.getPasswordRequireLowercase()));
        }
        if (settingsVO.getPasswordRequireNumber() != null) {
            settingsMap.put("passwordRequireNumber", String.valueOf(settingsVO.getPasswordRequireNumber()));
        }
        if (settingsVO.getPasswordRequireSpecial() != null) {
            settingsMap.put("passwordRequireSpecial", String.valueOf(settingsVO.getPasswordRequireSpecial()));
        }
        if (settingsVO.getPasswordExpireDays() != null) {
            settingsMap.put("passwordExpireDays", String.valueOf(settingsVO.getPasswordExpireDays()));
        }

        // 登录策略
        if (settingsVO.getLoginMaxAttempts() != null) {
            settingsMap.put("loginMaxAttempts", String.valueOf(settingsVO.getLoginMaxAttempts()));
        }
        if (settingsVO.getLoginLockDuration() != null) {
            settingsMap.put("loginLockDuration", String.valueOf(settingsVO.getLoginLockDuration()));
        }
        if (settingsVO.getLoginSessionTimeout() != null) {
            settingsMap.put("loginSessionTimeout", String.valueOf(settingsVO.getLoginSessionTimeout()));
        }
        if (settingsVO.getLoginEnableCaptcha() != null) {
            settingsMap.put("loginEnableCaptcha", String.valueOf(settingsVO.getLoginEnableCaptcha()));
        }

        // 保存或更新每个设置
        for (Map.Entry<String, String> entry : settingsMap.entrySet()) {
            saveOrUpdateSetting("security", entry.getKey(), entry.getValue());
        }

        log.info("安全设置更新完成");
    }

    /**
     * 获取系统信息
     */
    public SystemInfoVO getSystemInfo() {
        SystemInfoVO systemInfo = new SystemInfoVO();

        try {
            // 获取系统属性
            java.lang.management.RuntimeMXBean runtimeMXBean = java.lang.management.ManagementFactory.getRuntimeMXBean();

            // 系统名称
            String systemName = getSettingValue("basic", "systemName");
            if (systemName == null) {
                systemName = getSettingValue("basic", "appName");
            }
            systemInfo.setName(systemName != null ? systemName : "Yuncode LowCode");

            // 系统版本
            String appVersion = getSettingValue("basic", "appVersion");
            systemInfo.setVersion(appVersion != null ? appVersion : "1.0.0");

            // 运行环境
            String env = System.getProperty("spring.profiles.active", "dev");
            systemInfo.setEnv(env);

            // 框架
            systemInfo.setFramework("Spring Boot");

            // Java版本
            systemInfo.setJavaVersion(System.getProperty("java.version"));

            // 启动时间
            if (runtimeMXBean.getUptime() > 0) {
                long startTime = runtimeMXBean.getStartTime();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                systemInfo.setStartTime(sdf.format(new java.util.Date(startTime)));
            }

            // 运行时长
            long uptime = runtimeMXBean.getUptime();
            long days = uptime / (24 * 60 * 60 * 1000);
            long hours = (uptime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
            long minutes = (uptime % (60 * 60 * 1000)) / (60 * 1000);
            systemInfo.setUptime(days + "天 " + hours + "小时 " + minutes + "分钟");

            // 服务器IP
            try {
                java.net.InetAddress inetAddress = java.net.InetAddress.getLocalHost();
                systemInfo.setServerIp(inetAddress.getHostAddress());
            } catch (Exception e) {
                systemInfo.setServerIp("localhost");
            }

            // 操作系统
            systemInfo.setOs(System.getProperty("os.name"));

            // 系统架构
            systemInfo.setArch(System.getProperty("os.arch"));

        } catch (Exception e) {
            log.error("获取系统信息失败", e);
        }

        log.info("获取系统信息");

        return systemInfo;
    }
}
