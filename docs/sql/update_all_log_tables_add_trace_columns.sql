-- =============================================
-- 完整链路追踪功能更新脚本
-- 为所有日志表添加链路追踪字段
-- =============================================

USE yuncode_lowcode;

-- =============================================
-- 1. sys_login_log（登录日志）
-- =============================================
ALTER TABLE sys_login_log
    ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64) COMMENT '链路追踪ID' AFTER cost_time,
    ADD COLUMN IF NOT EXISTS span_id VARCHAR(64) COMMENT 'Span ID' AFTER trace_id,
    ADD COLUMN IF NOT EXISTS parent_span_id VARCHAR(64) COMMENT '父 Span ID' AFTER span_id;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_login_log_trace_id ON sys_login_log(trace_id);

-- =============================================
-- 2. sys_operation_log（操作日志）
-- =============================================
-- 检查表是否存在，如果存在则添加字段
-- ALTER TABLE sys_operation_log
--     ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64) COMMENT '链路追踪ID',
--     ADD COLUMN IF NOT EXISTS span_id VARCHAR(64) COMMENT 'Span ID',
--     ADD COLUMN IF NOT EXISTS parent_span_id VARCHAR(64) COMMENT '父 Span ID';

-- 添加索引
-- CREATE INDEX IF NOT EXISTS idx_operation_log_trace_id ON sys_operation_log(trace_id);

-- =============================================
-- 3. sys_system_log（系统日志）
-- =============================================
-- 检查表是否存在，如果存在则添加字段
-- ALTER TABLE sys_system_log
--     ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64) COMMENT '链路追踪ID',
--     ADD COLUMN IF NOT EXISTS span_id VARCHAR(64) COMMENT 'Span ID',
--     ADD COLUMN IF NOT EXISTS parent_span_id VARCHAR(64) COMMENT '父 Span ID';

-- 添加索引
-- CREATE INDEX IF NOT EXISTS idx_system_log_trace_id ON sys_system_log(trace_id);

-- =============================================
-- 验证脚本
-- =============================================

-- 查看表结构
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_COMMENT
FROM
    information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = 'yuncode_lowcode'
    AND TABLE_NAME IN ('sys_login_log', 'sys_operation_log', 'sys_system_log')
    AND COLUMN_NAME IN ('trace_id', 'span_id', 'parent_span_id')
ORDER BY
    TABLE_NAME, ORDINAL_POSITION;

-- =============================================
-- 使用说明
-- =============================================
-- 1. 执行前请备份数据库！
-- 2. 根据实际存在的表，取消注释相应的 ALTER TABLE 语句
-- 3. 如果表中已有 trace_id 字段，ADD COLUMN IF NOT EXISTS 会跳过
-- 4. 新的日志会自动填充链路追踪字段
-- 5. 历史数据的 trace_id 为 NULL，不影响查询
-- =============================================
