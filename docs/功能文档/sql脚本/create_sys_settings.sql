-- =============================================
-- 系统设置表
-- =============================================

CREATE TABLE IF NOT EXISTS `sys_settings` (
    `id` BIGINT NOT NULL COMMENT '设置ID',
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `setting_group` VARCHAR(50) NOT NULL COMMENT '设置分组',
    `setting_key` VARCHAR(100) NOT NULL COMMENT '设置键',
    `setting_value` TEXT COMMENT '设置值',
    `setting_name` VARCHAR(100) COMMENT '设置名称',
    `description` VARCHAR(500) COMMENT '设置描述',
    `data_type` VARCHAR(20) DEFAULT 'string' COMMENT '数据类型',
    `is_system` TINYINT DEFAULT 0 COMMENT '是否系统设置',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 0 COMMENT '状态',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_key` (`setting_group`, `setting_key`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_setting_group` (`setting_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置表';

-- =============================================
-- 插入默认基础设置
-- =============================================

INSERT INTO `sys_settings` (`id`, `tenant_id`, `setting_group`, `setting_key`, `setting_value`, `setting_name`, `description`, `data_type`, `is_system`, `sort`, `status`) VALUES
(1, NULL, 'basic', 'appName', 'Yuncode LowCode', '应用名称', '应用的显示名称', 'string', 1, 1, 0),
(2, NULL, 'basic', 'appVersion', '1.0.0', '应用版本', '当前应用版本号', 'string', 1, 2, 0),
(3, NULL, 'basic', 'appLogo', '/logo.png', '应用Logo', '应用的Logo图片路径', 'string', 1, 3, 0),
(4, NULL, 'basic', 'appDescription', '云创低代码平台', '应用描述', '应用的简短描述', 'string', 1, 4, 0),
(5, NULL, 'basic', 'copyright', '© 2024 Yuncode. All rights reserved.', '版权信息', '版权声明信息', 'string', 1, 5, 0),
(6, NULL, 'basic', 'icp', '', '备案号', 'ICP备案号', 'string', 1, 6, 0);

-- =============================================
-- 验证数据
-- =============================================

SELECT * FROM sys_settings WHERE setting_group = 'basic' ORDER BY sort;
