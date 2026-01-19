package com.yuncode.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.system.entity.SysSettings;
import com.yuncode.system.mapper.SysSettingsMapper;
import com.yuncode.system.vo.SettingsVO;
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
                case "appName" -> settingsVO.setAppName(value);
                case "appVersion" -> settingsVO.setAppVersion(value);
                case "appLogo" -> settingsVO.setAppLogo(value);
                case "appDescription" -> settingsVO.setAppDescription(value);
                case "copyright" -> settingsVO.setCopyright(value);
                case "icp" -> settingsVO.setIcp(value);
            }
        }

        // 如果没有数据，设置默认值
        if (settingsVO.getAppName() == null) {
            settingsVO.setAppName("Yuncode LowCode");
        }
        if (settingsVO.getAppVersion() == null) {
            settingsVO.setAppVersion("1.0.0");
        }
        if (settingsVO.getAppDescription() == null) {
            settingsVO.setAppDescription("云创低代码平台");
        }
        if (settingsVO.getCopyright() == null) {
            settingsVO.setCopyright("© 2024 Yuncode. All rights reserved.");
        }

        log.info("获取基础设置: appName={}", settingsVO.getAppName());

        return settingsVO;
    }

    /**
     * 更新基础设置
     */
    public void updateBasicSettings(SettingsVO settingsVO) {
        // TODO: 实现更新逻辑
        log.info("更新基础设置: {}", settingsVO);
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
}
