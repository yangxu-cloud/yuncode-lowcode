-- =============================================
-- 公司管理相关表结构
-- =============================================
-- 功能：多租户多公司管理，支持一个租户管理多家公司
-- 创建时间：2025-01-22
-- =============================================

USE yuncode_lowcode;

-- =============================================
-- 1. 公司信息表 (sys_company)
-- =============================================
CREATE TABLE `sys_company` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `company_name` VARCHAR(100) NOT NULL COMMENT '公司名称',
    `company_code` VARCHAR(50) NOT NULL COMMENT '公司编码',
    `company_type` TINYINT(1) DEFAULT 1 COMMENT '公司类型：1=有限公司，2=股份公司，3=个体工商户，4=其他',
    `credit_code` VARCHAR(50) COMMENT '统一社会信用代码',
    `legal_person` VARCHAR(50) COMMENT '法定代表人',
    `register_capital` DECIMAL(18,2) COMMENT '注册资本（万元）',
    `establish_date` DATE COMMENT '成立日期',
    `register_address` VARCHAR(500) COMMENT '注册地址',
    `business_address` VARCHAR(500) COMMENT '经营地址',
    `business_scope` TEXT COMMENT '经营范围',
    `contact_phone` VARCHAR(20) COMMENT '联系电话',
    `contact_email` VARCHAR(100) COMMENT '联系邮箱',
    `business_license` VARCHAR(500) COMMENT '营业执照图片URL',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    `tenant_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '租户ID',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记：0=未删除，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_company_code_tenant` (`company_code`, `tenant_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_credit_code` (`credit_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司信息表';

-- =============================================
-- 2. 为组织表添加 company_id 字段
-- =============================================
ALTER TABLE `sys_org`
ADD COLUMN `company_id` BIGINT(20) DEFAULT NULL COMMENT '所属公司ID' AFTER `id`,
ADD KEY `idx_company_id` (`company_id`);

-- =============================================
-- 3. 为用户表添加 company_id 字段
-- =============================================
ALTER TABLE `sys_user`
ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司ID' AFTER `tenant_id`,
ADD KEY `idx_company_id` (`company_id`);

-- =============================================
-- 4. 同步数据：为已有组织和用户设置 company_id
-- =============================================
-- 根据组织的 org_type=1（集团/公司）创建对应的公司记录（示例）
-- 注意：这里只是示例，实际业务需要根据具体情况调整

-- =============================================
-- 初始化数据示例
-- =============================================
-- INSERT INTO `sys_company` (`company_name`, `company_code`, `company_type`, `status`, `tenant_id`, `remark`, `create_by`)
-- VALUES
-- ('示例科技有限公司', 'DEMO001', 1, 1, 0, '演示公司', 'system'),
-- ('示例贸易有限公司', 'DEMO002', 1, 1, 0, '演示公司', 'system');

-- =============================================
-- 数据完整性约束（可选）
-- =============================================
-- 添加外键约束（如果需要强制关联）
-- ALTER TABLE `sys_org`
-- ADD CONSTRAINT `fk_org_company` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`) ON DELETE SET NULL;

-- ALTER TABLE `sys_user`
-- ADD CONSTRAINT `fk_user_company` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`) ON DELETE SET NULL;

-- =============================================
-- 查询验证
-- =============================================
-- 查看所有公司
-- SELECT * FROM sys_company;

-- 查看某租户下的公司
-- SELECT * FROM sys_company WHERE tenant_id = 0;

-- 查看某公司下的组织
-- SELECT * FROM sys_org WHERE company_id = 1;

-- 查看某公司下的用户
-- SELECT * FROM sys_user WHERE company_id = 1;

-- =============================================
-- 常用查询示例
-- =============================================

-- 1. 查询租户下的所有公司及人员数量
-- SELECT
--     c.id,
--     c.company_name,
--     c.company_code,
--     COUNT(DISTINCT u.id) as user_count
-- FROM sys_company c
-- LEFT JOIN sys_user u ON c.id = u.company_id AND u.deleted = 0
-- WHERE c.tenant_id = 0 AND c.deleted = 0
-- GROUP BY c.id;

-- 2. 查询公司及其组织架构
-- SELECT
--     c.company_name,
--     o.org_name,
--     o.org_type
-- FROM sys_company c
-- LEFT JOIN sys_org o ON c.id = o.company_id AND o.deleted = 0
-- WHERE c.tenant_id = 0 AND c.deleted = 0
-- ORDER BY c.id, o.parent_id, o.sort_order;

-- 3. 查询用户所属公司信息
-- SELECT
--     u.username,
--     u.nickname,
--     c.company_name,
--     o.org_name as dept_name
-- FROM sys_user u
-- LEFT JOIN sys_company c ON u.company_id = c.id
-- LEFT JOIN sys_org o ON u.dept_id = o.id
-- WHERE u.deleted = 0;
