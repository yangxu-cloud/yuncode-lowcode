-- ========================================
-- 系统日志表 (sys_system_log)
-- 用于记录系统运行时的日志信息，支持链路追踪
-- ========================================

USE yuncode_lowcode;

-- 删除旧表（如果存在）
DROP TABLE IF EXISTS `sys_system_log`;

-- 创建系统日志表
CREATE TABLE `sys_system_log` (
    `id` BIGINT NOT NULL COMMENT '日志ID',
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `tenant_name` VARCHAR(100) DEFAULT NULL COMMENT '租户名称',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `level` VARCHAR(20) NOT NULL COMMENT '日志级别：TRACE, DEBUG, INFO, WARN, ERROR',
    `module` VARCHAR(100) DEFAULT NULL COMMENT '模块',
    `message` TEXT COMMENT '日志消息',
    `exception` TEXT COMMENT '异常信息',
    `stack_trace` TEXT COMMENT '堆栈跟踪',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    `span_id` VARCHAR(64) DEFAULT NULL COMMENT 'Span ID',
    `parent_span_id` VARCHAR(64) DEFAULT NULL COMMENT '父 Span ID',
    `tags` TEXT COMMENT '自定义标签（JSON格式）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_level` (`level`),
    KEY `idx_module` (`module`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- ========================================
-- 说明
-- ========================================
-- 1. 此表用于记录系统运行时的日志信息
-- 2. 支持链路追踪（MDC + SkyWalking）
-- 3. 包含 trace_id, span_id, parent_span_id 字段用于分布式追踪
-- 4. level 字段支持：TRACE, DEBUG, INFO, WARN, ERROR
-- 5. 与 sys_oper_log 表的区别：
--    - sys_system_log: 系统运行日志，记录代码执行过程中的日志信息
--    - sys_oper_log: 操作日志，记录用户的操作行为（增删改查等）
-- ========================================
