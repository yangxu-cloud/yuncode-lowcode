-- =============================================
-- 为 sys_user 表添加 is_leader 字段
-- =============================================
-- 用途：标识用户是否是其主部门的负责人
-- 设计说明：
--   1. sys_user.dept_id + is_leader - 主部门及是否主部门领导
--   2. sys_user_org - 兼职部门关系（user_id + org_id + is_leader）
-- =============================================

USE yuncode_lowcode;

-- 添加 is_leader 字段
ALTER TABLE `sys_user`
ADD COLUMN `is_leader` TINYINT(1) DEFAULT 0 COMMENT '是否主部门领导：0-否 1-是'
AFTER `dept_id`;

-- 添加索引以提升查询性能
ALTER TABLE `sys_user`
ADD INDEX `idx_dept_leader` (`dept_id`, `is_leader`);

-- =============================================
-- 数据迁移说明（可选）
-- =============================================
-- 如果需要将 sys_user_org 中的领导关系同步到 sys_user.is_leader
-- 可以执行以下 SQL：

-- 将主部门领导关系同步到 sys_user 表
-- UPDATE sys_user u
-- INNER JOIN sys_user_org uo ON u.id = uo.user_id AND u.dept_id = uo.org_id
-- SET u.is_leader = 1
-- WHERE uo.is_leader = 1;

-- =============================================
-- 验证字段是否添加成功
-- =============================================
SELECT
    id,
    username,
    nickname,
    dept_id,
    is_leader,
    role_ids
FROM sys_user
LIMIT 5;
