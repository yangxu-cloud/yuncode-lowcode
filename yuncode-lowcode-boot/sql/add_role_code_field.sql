-- =============================================
-- 添加用户角色编码字段
-- 日期: 2025-01-28
-- 说明: 实现三级权限体系（平台管理员、租户管理员、普通用户）
-- =============================================

-- 1. 添加 role_code 字段
ALTER TABLE sys_user
ADD COLUMN role_code VARCHAR(50) COMMENT '角色编码：PLATFORM_ADMIN(平台管理员)/TENANT_ADMIN(租户管理员)/NORMAL(普通用户)' AFTER role_ids;

-- 2. 为现有用户设置默认角色（普通用户）
UPDATE sys_user
SET role_code = 'NORMAL'
WHERE role_code IS NULL;

-- 3. 设置平台管理员（根据实际用户名调整）
-- 将 admin 用户设置为平台管理员
UPDATE sys_user
SET role_code = 'PLATFORM_ADMIN',
    tenant_id = 0
WHERE username = 'admin';

-- 4. 为 role_code 字段添加索引（提高查询性能）
CREATE INDEX idx_role_code ON sys_user(role_code);

-- =============================================
-- 验证脚本
-- =============================================

-- 查看所有用户的角色编码
SELECT
    id,
    username,
    real_name,
    role_code,
    tenant_id,
    status
FROM sys_user
WHERE deleted = 0;

-- 查看角色分布
SELECT
    role_code,
    COUNT(*) as user_count
FROM sys_user
WHERE deleted = 0
GROUP BY role_code;

-- =============================================
-- 角色编码说明
-- =============================================
-- PLATFORM_ADMIN: 平台管理员
--   - 可以添加公司（创建租户）
--   - 可以查看所有租户数据
--   - tenant_id = 0 或 NULL
--   - 数量：1-3个（Yuncode 内部人员）
--
-- TENANT_ADMIN: 租户管理员
--   - 可以管理本租户的组织、用户、角色
--   - 不能添加公司（创建租户）
--   - tenant_id = 具体租户ID
--   - 数量：每个租户 1-N 个
--
-- NORMAL: 普通用户
--   - 根据角色权限访问功能
--   - tenant_id = 具体租户ID
-- =============================================
