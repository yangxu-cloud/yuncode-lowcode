-- 组织服务相关表结构

-- 1. 组织表 (sys_org)
CREATE TABLE `sys_org` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `org_name` VARCHAR(100) NOT NULL COMMENT '组织名称',
    `org_code` VARCHAR(50) NOT NULL COMMENT '组织编码',
    `parent_id` BIGINT(20) DEFAULT 0 COMMENT '父组织ID，0表示根节点',
    `org_type` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '组织类型：1=集团/公司，2=部门',
    `is_company` TINYINT(1) DEFAULT 1 COMMENT '是否公司：0=否，1=是',
    `sort_order` INT(11) DEFAULT 0 COMMENT '排序号',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    `tenant_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '租户ID',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记：0=未删除，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_org_code_tenant` (`org_code`, `tenant_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_org_type` (`org_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织表';

-- 2. 用户组织关联表 (sys_user_org)
CREATE TABLE `sys_user_org` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `org_id` BIGINT(20) NOT NULL COMMENT '组织ID',
    `is_leader` TINYINT(1) DEFAULT 0 COMMENT '是否负责人：0=否，1-是',
    `is_main_dept` TINYINT(1) DEFAULT 0 COMMENT '是否主部门：0-否，1-是（与sys_user.dept_id对应）',
    `tenant_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '租户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记：0=未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_org` (`user_id`, `org_id`, `tenant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_org_id` (`org_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_user_main_dept` (`user_id`, `is_main_dept`),
    KEY `idx_org_main_dept` (`org_id`, `is_main_dept`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组织关联表';

-- 初始化数据：默认组织架构
INSERT INTO `sys_org` (`id`, `org_name`, `org_code`, `parent_id`, `org_type`, `is_company`, `sort_order`, `status`, `tenant_id`, `remark`, `create_by`)
VALUES (1, '组织架构', 'ROOT', 0, 1, 1, 0, 1, 0, '系统根组织', 'system');

-- =============================================
-- 表结构升级脚本（用于已存在的表）
-- =============================================

-- 为已存在的 sys_user_org 表添加 is_main_dept 字段
ALTER TABLE `sys_user_org`
ADD COLUMN `is_main_dept` TINYINT(1) DEFAULT 0 COMMENT '是否主部门：0-否，1-是（与sys_user.dept_id对应）' AFTER `is_leader`;

-- 添加索引提升查询性能
ALTER TABLE `sys_user_org`
ADD INDEX `idx_user_main_dept` (`user_id`, `is_main_dept`),
ADD INDEX `idx_org_main_dept` (`org_id`, `is_main_dept`);

-- 同步 sys_user.dept_id 到 sys_user_org.is_main_dept
-- 将 sys_user 中 dept_id 对应的 sys_user_org 记录标记为主部门
UPDATE `sys_user_org` uo
INNER JOIN `sys_user` u ON uo.user_id = u.id AND uo.org_id = u.dept_id
SET uo.is_main_dept = 1
WHERE u.deleted = 0 AND uo.deleted = 0;
