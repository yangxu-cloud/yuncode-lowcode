-- =============================================
-- Yuncode LowCode 数据库初始化脚本
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `yuncode_lowcode`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `yuncode_lowcode`;

-- =============================================
-- 1. 系统租户表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_tenant` (
    `id` BIGINT NOT NULL COMMENT '租户ID',
    `tenant_name` VARCHAR(100) NOT NULL COMMENT '租户名称',
    `tenant_code` VARCHAR(50) NOT NULL COMMENT '租户编码',
    `contact_name` VARCHAR(50) COMMENT '联系人',
    `contact_phone` VARCHAR(20) COMMENT '联系电话',
    `contact_email` VARCHAR(100) COMMENT '联系邮箱',
    `address` VARCHAR(500) COMMENT '企业地址',
    `tenant_type` TINYINT DEFAULT 0 COMMENT '租户类型（0试用 1标准 2高级 3企业）',
    `expire_time` DATETIME COMMENT '过期时间',
    `user_limit` INT DEFAULT 100 COMMENT '用户数量限制',
    `storage_limit` INT DEFAULT 10240 COMMENT '存储空间限制（MB）',
    `status` TINYINT DEFAULT 0 COMMENT '状态（0正常 1禁用）',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`),
    KEY `idx_tenant_type` (`tenant_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统租户表';

-- =============================================
-- 2. 系统用户表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL COMMENT '用户ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `avatar` VARCHAR(500) COMMENT '头像',
    `gender` TINYINT DEFAULT 0 COMMENT '性别（0男 1女 2未知）',
    `dept_id` BIGINT COMMENT '部门ID',
    `role_ids` VARCHAR(1000) COMMENT '角色ID列表（逗号分隔）',
    `status` TINYINT DEFAULT 0 COMMENT '状态（0正常 1禁用）',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- =============================================
-- 3. 登录日志表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id` BIGINT NOT NULL COMMENT '日志ID',
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `tenant_name` VARCHAR(100) COMMENT '租户名称',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) COMMENT '用户名',
    `login_time` DATETIME COMMENT '登录时间',
    `logout_time` DATETIME COMMENT '登出时间',
    `ipaddr` VARCHAR(128) COMMENT 'IP地址',
    `login_location` VARCHAR(255) COMMENT '登录地点',
    `browser` VARCHAR(50) COMMENT '浏览器',
    `os` VARCHAR(50) COMMENT '操作系统',
    `status` TINYINT DEFAULT 0 COMMENT '状态（0成功 1失败）',
    `msg` VARCHAR(500) COMMENT '提示信息',
    `cost_time` BIGINT DEFAULT 0 COMMENT '执行时长(ms)',
    `trace_id` VARCHAR(64) COMMENT '链路追踪ID',
    `span_id` VARCHAR(64) COMMENT 'Span ID',
    `parent_span_id` VARCHAR(64) COMMENT '父 Span ID',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_login_time` (`login_time`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- =============================================
-- 4. 操作日志表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id` BIGINT NOT NULL COMMENT '日志ID',
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `tenant_name` VARCHAR(100) COMMENT '租户名称',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) COMMENT '用户名',
    `module` VARCHAR(50) COMMENT '模块',
    `operation` VARCHAR(100) COMMENT '操作',
    `method` VARCHAR(200) COMMENT '方法',
    `params` TEXT COMMENT '参数',
    `ip` VARCHAR(128) COMMENT 'IP地址',
    `location` VARCHAR(255) COMMENT '位置',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `execute_time` BIGINT DEFAULT 0 COMMENT '执行时长(ms)',
    `status` TINYINT DEFAULT 0 COMMENT '状态（0成功 1失败）',
    `error_msg` TEXT COMMENT '错误信息',
    `trace_id` VARCHAR(64) COMMENT '链路追踪ID',
    `span_id` VARCHAR(64) COMMENT 'Span ID',
    `parent_span_id` VARCHAR(64) COMMENT '父 Span ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_module` (`module`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- =============================================
-- 5. 系统日志表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_system_log` (
    `id` BIGINT NOT NULL COMMENT '日志ID',
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `tenant_name` VARCHAR(100) COMMENT '租户名称',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) COMMENT '用户名',
    `log_type` VARCHAR(20) COMMENT '日志类型',
    `log_title` VARCHAR(100) COMMENT '日志标题',
    `log_content` TEXT COMMENT '日志内容',
    `request_method` VARCHAR(10) COMMENT '请求方法',
    `request_url` VARCHAR(500) COMMENT '请求URL',
    `request_params` TEXT COMMENT '请求参数',
    `ip` VARCHAR(128) COMMENT 'IP地址',
    `location` VARCHAR(255) COMMENT '位置',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `execute_time` BIGINT DEFAULT 0 COMMENT '执行时长(ms)',
    `status` TINYINT DEFAULT 0 COMMENT '状态（0成功 1失败）',
    `error_msg` TEXT COMMENT '错误信息',
    `trace_id` VARCHAR(64) COMMENT '链路追踪ID',
    `span_id` VARCHAR(64) COMMENT 'Span ID',
    `parent_span_id` VARCHAR(64) COMMENT '父 Span ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_log_type` (`log_type`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- =============================================
-- 6. 系统设置表
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
    `status` TINYINT DEFAULT 0 COMMENT '状态（0正常 1禁用）',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
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
-- 插入初始数据
-- =============================================

-- 插入默认租户
INSERT INTO `sys_tenant` (`id`, `tenant_name`, `tenant_code`, `contact_name`, `tenant_type`, `expire_time`, `status`, `remark`)
VALUES (1, '默认租户', 'default', 'System', 3, '2099-12-31 23:59:59', 0, '系统默认租户')
ON DUPLICATE KEY UPDATE `tenant_name` = VALUES(`tenant_name`);

-- 插入默认管理员用户（密码：admin123，使用 BCrypt 加密）
INSERT INTO `sys_user` (`id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `status`)
VALUES (1, 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', '系统管理员', 0)
ON DUPLICATE KEY UPDATE `password` = VALUES(`password`);

-- 插入默认基础设置
INSERT INTO `sys_settings` (`id`, `tenant_id`, `setting_group`, `setting_key`, `setting_value`, `setting_name`, `description`, `data_type`, `is_system`, `sort`, `status`) VALUES
(1, NULL, 'basic', 'appName', 'Yuncode LowCode', '应用名称', '应用的显示名称', 'string', 1, 1, 0),
(2, NULL, 'basic', 'appVersion', '1.0.0', '应用版本', '当前应用版本号', 'string', 1, 2, 0),
(3, NULL, 'basic', 'appLogo', '/logo.png', '应用Logo', '应用的Logo图片路径', 'string', 1, 3, 0),
(4, NULL, 'basic', 'appDescription', '云创低代码平台', '应用描述', '应用的简短描述', 'string', 1, 4, 0),
(5, NULL, 'basic', 'copyright', '© 2024 Yuncode. All rights reserved.', '版权信息', '版权声明信息', 'string', 1, 5, 0),
(6, NULL, 'basic', 'icp', '', '备案号', 'ICP备案号', 'string', 1, 6, 0)
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);

-- =============================================
-- 验证数据
-- =============================================

-- 查看租户
SELECT * FROM sys_tenant;

-- 查看用户
SELECT id, tenant_id, username, nickname, status FROM sys_user;

-- 查看设置
SELECT * FROM sys_settings WHERE setting_group = 'basic' ORDER BY sort;
