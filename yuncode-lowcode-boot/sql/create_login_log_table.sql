-- ========================================
-- 登录日志表 (sys_login_log)
-- 用于记录用户登录和登出行为
-- ========================================

USE yuncode_lowcode;

-- 删除旧表（如果存在）
DROP TABLE IF EXISTS `sys_login_log`;

-- 创建登录日志表
CREATE TABLE `sys_login_log` (
    `id` BIGINT NOT NULL COMMENT '日志ID',
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `tenant_name` VARCHAR(100) DEFAULT NULL COMMENT '租户名称',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `login_time` DATETIME DEFAULT NULL COMMENT '登录时间',
    `logout_time` DATETIME DEFAULT NULL COMMENT '登出时间',
    `ipaddr` VARCHAR(128) DEFAULT NULL COMMENT '登录IP地址',
    `login_location` VARCHAR(255) DEFAULT NULL COMMENT '登录地点',
    `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器类型',
    `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
    `status` TINYINT DEFAULT 0 COMMENT '状态（0成功 1失败）',
    `msg` VARCHAR(255) DEFAULT NULL COMMENT '提示消息',
    `cost_time` BIGINT DEFAULT NULL COMMENT '访问时长（毫秒）',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    `span_id` VARCHAR(64) DEFAULT NULL COMMENT 'Span ID',
    `parent_span_id` VARCHAR(64) DEFAULT NULL COMMENT '父 Span ID',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_login_time` (`login_time`),
    KEY `idx_status` (`status`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ========================================
-- 说明
-- ========================================
-- 1. 此表用于记录用户的登录和登出行为
-- 2. 支持链路追踪（MDC + SkyWalking）
-- 3. status 字段说明：
--    0: 登录成功
--    1: 登录失败
-- 4. cost_time 字段记录在线时长（毫秒），登出时更新
-- 5. 与 sys_oper_log 和 sys_system_log 的区别：
--    - sys_login_log: 登录日志，记录用户登录/登出行为
--    - sys_oper_log: 操作日志，记录用户的操作行为（增删改查等）
--    - sys_system_log: 系统运行日志，记录代码执行过程中的日志信息
-- ========================================
