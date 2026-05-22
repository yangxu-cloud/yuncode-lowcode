-- ========================================
-- 操作日志表 (sys_oper_log)
-- 用于记录用户的操作行为，支持审计追踪
-- ========================================

USE yuncode_lowcode;

-- 删除旧表（如果存在）
DROP TABLE IF EXISTS `sys_oper_log`;

-- 创建操作日志表
CREATE TABLE `sys_oper_log` (
  `id` BIGINT NOT NULL COMMENT '日志ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `module` VARCHAR(50) COMMENT '模块标题',
  `business_type` TINYINT COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` VARCHAR(100) COMMENT '方法名称',
  `request_method` VARCHAR(10) COMMENT '请求方式',
  `oper_name` VARCHAR(50) COMMENT '操作人员',
  `oper_url` VARCHAR(255) COMMENT '请求URL',
  `oper_ip` VARCHAR(128) COMMENT '主机地址',
  `oper_param` TEXT COMMENT '请求参数',
  `json_result` TEXT COMMENT '返回参数',
  `status` TINYINT DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` VARCHAR(2000) COMMENT '错误消息',
  `oper_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  `span_id` VARCHAR(64) DEFAULT NULL COMMENT 'Span ID',
  `parent_span_id` VARCHAR(64) DEFAULT NULL COMMENT '父 Span ID',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ========================================
-- 说明
-- ========================================
-- 1. 此表用于记录用户的操作行为
-- 2. 支持链路追踪（MDC + SkyWalking）
-- 3. business_type 字段说明：
--    0: 其它
--    1: 新增
--    2: 修改
--    3: 删除
--    4: 授权
--    5: 导出
--    6: 导入
-- 4. status 字段说明：
--    0: 正常
--    1: 异常
-- 5. 与 sys_system_log 表的区别：
--    - sys_oper_log: 操作日志，记录用户的操作行为（增删改查等）
--    - sys_system_log: 系统运行日志，记录代码执行过程中的日志信息
-- ========================================
