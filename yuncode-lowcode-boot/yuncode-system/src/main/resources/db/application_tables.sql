-- 应用管理表结构
-- 创建时间：2025-01-30

-- ============================================
-- 应用表
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
-- 应用日志表
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