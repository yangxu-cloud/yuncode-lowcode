-- =============================================
-- 链路追踪功能更新脚本
-- 为 sys_login_log 表添加链路追踪字段
-- =============================================

-- 使用数据库
USE yuncode_lowcode;

-- 1. 为 sys_login_log 表添加链路追踪字段
ALTER TABLE sys_login_log
    ADD COLUMN trace_id VARCHAR(64) COMMENT '链路追踪ID' AFTER cost_time,
    ADD COLUMN span_id VARCHAR(64) COMMENT 'Span ID' AFTER trace_id,
    ADD COLUMN parent_span_id VARCHAR(64) COMMENT '父 Span ID' AFTER span_id;

-- 2. 为链路追踪字段添加索引，提高查询性能
CREATE INDEX idx_trace_id ON sys_login_log(trace_id);

-- 3. 添加注释
ALTER TABLE sys_login_log
    MODIFY COLUMN trace_id VARCHAR(64) COMMENT '链路追踪ID（MDC + SkyWalking）',
    MODIFY COLUMN span_id VARCHAR(64) COMMENT 'Span ID（用于分布式追踪）',
    MODIFY COLUMN parent_span_id VARCHAR(64) COMMENT '父 Span ID（用于调用链分析）';

-- =============================================
-- 验证脚本
-- =============================================

-- 查看表结构
DESC sys_login_log;

-- 查看索引
SHOW INDEX FROM sys_login_log;

-- =============================================
-- 注意事项
-- =============================================
-- 1. 执行前请备份数据库
-- 2. 如果表中已有数据，trace_id 字段将为 NULL
-- 3. 新的登录日志会自动填充 trace_id、span_id、parent_span_id
-- 4. 可选：为历史数据生成 trace_id（根据业务需求决定是否执行）
-- =============================================

-- 可选：为历史数据生成 trace_id（取消注释以下 SQL 以执行）
-- UPDATE sys_login_log
-- SET trace_id = CONCAT('HIST-', LPAD(id, 20, '0')),
--     span_id = CONCAT('SPAN-', LPAD(id, 20, '0'))
-- WHERE trace_id IS NULL;
