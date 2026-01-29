-- =============================================
-- 组织架构与租户关联升级脚本
-- 日期: 2025-01-28
-- 说明: 实现"添加公司即创建租户"的架构设计
-- =============================================

-- 1. 为 sys_org 表添加租户相关字段
ALTER TABLE sys_org
ADD COLUMN tenant_code VARCHAR(50) COMMENT '租户编码（仅公司节点orgType=1时有值，用于登录租户识别）' AFTER org_type;

-- 2. 为 tenant_code 字段添加索引（提高登录时的查询速度）
CREATE INDEX idx_tenant_code ON sys_org(tenant_code);

-- 3. 为现有数据设置租户编码（根据实际数据调整）
-- 将现有公司节点的 org_code 同步到 tenant_code
UPDATE sys_org
SET tenant_code = org_code
WHERE org_type = 1 AND tenant_code IS NULL;

-- 4. 添加 sys_tenant 表的 org_id 字段（关联组织ID）
-- 检查字段是否存在，不存在则添加
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'yuncode_lowcode'
    AND TABLE_NAME = 'sys_tenant'
    AND COLUMN_NAME = 'org_id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sys_tenant ADD COLUMN org_id BIGINT COMMENT ''关联的组织ID（sys_org.id）'' AFTER id',
    'SELECT ''Column org_id already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 同步现有数据：将现有的 sys_tenant 与 sys_org 关联
-- 注意：需要根据实际数据调整关联逻辑
-- UPDATE sys_tenant t
-- INNER JOIN sys_org o ON t.tenant_code = t.tenant_code
-- SET t.org_id = o.id
-- WHERE t.org_id IS NULL;

-- =============================================
-- 验证脚本
-- =============================================

-- 查看所有公司节点（orgType=1）
SELECT
    id,
    org_name,
    org_code,
    tenant_code,
    org_type,
    tenant_id
FROM sys_org
WHERE org_type = 1 AND deleted = 0;

-- 查看租户编码分布
SELECT
    tenant_code,
    COUNT(*) as org_count
FROM sys_org
WHERE org_type = 1 AND deleted = 0
GROUP BY tenant_code;

-- =============================================
-- 字段说明
-- =============================================
-- sys_org.tenant_code:
--   - 仅在公司节点（orgType=1）时有值
--   - 用于用户登录时的租户识别
--   - 全局唯一，格式如：tencent, alibaba, bytedance
--
-- sys_org.org_type:
--   - 0: 根节点（系统级）
--   - 1: 公司节点（租户）
--   - 2: 部门节点
--
-- sys_org.tenant_id:
--   - 冗余字段，存储所属租户ID
--   - 公司节点自己创建租户时，tenant_id 指向自己创建的租户
--   - 部门节点继承父节点的 tenant_id
--
-- sys_tenant.org_id:
--   - 关联的组织ID（sys_org.id）
--   - 用于租户与组织的双向关联
-- =============================================
