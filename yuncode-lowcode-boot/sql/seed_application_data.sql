-- ========================================
-- 应用表结构创建 + 种子数据
-- 创建日期: 2026-05-12
-- 说明: 先建表（如未创建），再为系统租户（ID=2）插入示例应用数据
-- ========================================

USE yuncode_lowcode;

-- ============================================
-- 1. 创建应用表（IF NOT EXISTS，可安全重复执行）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id` VARCHAR(200) NOT NULL COMMENT '应用ID（com.xxx.xxx格式）',
    `app_name` VARCHAR(200) NOT NULL COMMENT '应用名称',
    `app_icon` VARCHAR(500) DEFAULT NULL COMMENT '应用图标URL',
    `app_description` TEXT DEFAULT NULL COMMENT '应用描述',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '运行状态(0-未运行,1-运行中,2-已停止,3-异常)',
    `version` VARCHAR(50) DEFAULT NULL COMMENT '版本号',
    `start_time` DATETIME DEFAULT NULL COMMENT '启动时间',
    `stop_time` DATETIME DEFAULT NULL COMMENT '停止时间',
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id_tenant` (`app_id`, `tenant_id`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用表';

-- ============================================
-- 2. 创建应用日志表（IF NOT EXISTS）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_application_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id` BIGINT NOT NULL COMMENT '应用ID',
    `operation_type` TINYINT NOT NULL COMMENT '操作类型(0-安装,1-启动,2-停止,3-卸载,4-升级)',
    `operation_content` TEXT DEFAULT NULL COMMENT '操作内容',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0-成功,1-失败)',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用日志表';

-- ============================================
-- 3. 插入种子数据
-- ============================================

-- 清空旧数据
DELETE FROM `sys_application_log` WHERE `app_id` IN (SELECT `id` FROM `sys_application` WHERE `tenant_id` = 2);
DELETE FROM `sys_application` WHERE `tenant_id` = 2;

-- 插入示例应用
INSERT INTO `sys_application` (`app_id`, `app_name`, `app_icon`, `app_description`, `status`, `version`, `tenant_id`, `create_time`, `update_time`)
VALUES
('com.yuncode.demo.workflow', '工作流引擎', 'Monitor', '可视化工作流引擎，支持拖拽式流程设计、审批流配置、流程监控与统计分析。', 1, '2.1.0', 2, NOW(), NOW()),
('com.yuncode.demo.report', '智能报表系统', 'Document', '企业级智能报表系统，支持多数据源接入、可视化图表配置、定时报表推送。', 0, '1.3.0', 2, NOW(), NOW()),
('com.yuncode.demo.data-center', '数据中心', 'Management', '统一数据管理中心，提供数据集成、数据治理、数据服务能力。', 2, '3.0.0', 2, NOW(), NOW()),
('com.yuncode.demo.message', '消息中心', 'ChatDotSquare', '多渠道消息推送中心，支持站内信、邮件、短信、企业微信等方式。', 1, '1.5.2', 2, NOW(), NOW()),
('com.yuncode.demo.file', '文件存储服务', 'Folder', '分布式文件存储服务，支持本地存储、OSS、MinIO等多种存储方式。', 1, '2.0.1', 2, NOW(), NOW()),
('com.yuncode.demo.notice', '公告管理', 'Bell', '企业公告管理平台，支持公告发布、审核、撤回及阅读统计功能。', 0, '1.0.0', 2, NOW(), NOW());

-- 验证插入结果
SELECT COUNT(*) AS '应用数量' FROM `sys_application` WHERE `tenant_id` = 2;
SELECT `app_name`, `status`, `version` FROM `sys_application` WHERE `tenant_id` = 2;
