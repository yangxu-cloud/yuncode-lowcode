-- 添加 company_id 字段到 sys_org 表
-- 用于组织结构继承：部门继承公司的 company_id

-- 1. 添加 company_id 字段
ALTER TABLE `sys_org`
ADD COLUMN `company_id` BIGINT(20) DEFAULT NULL COMMENT '所属公司ID（部门继承公司ID时使用）' AFTER `parent_id`;

-- 2. 添加索引以提升查询性能
ALTER TABLE `sys_org`
ADD INDEX `idx_company_id` (`company_id`);

-- 3. 验证字段是否添加成功
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'sys_org'
  AND COLUMN_NAME = 'company_id';
