-- =============================================
-- 初始化系统租户（ID=2）
-- =============================================
-- 问题：系统期望有 ID=2 的系统租户用于平台管理员登录
-- 解决：创建系统租户记录
-- =============================================

-- 1. 检查当前租户表中的数据
SELECT '当前租户表数据' as description, id, tenant_name, tenant_code, status, deleted
FROM sys_tenant
ORDER BY id;

-- 2. 检查 ID=2 的租户是否存在
SELECT '检查ID=2租户是否存在' as description,
       COUNT(*) as count,
       MAX(CASE WHEN id = 2 AND deleted = 0 THEN 1 ELSE 0 END) as exists_active
FROM sys_tenant;

-- 3. 创建系统租户（ID=2）- 如果不存在则创建
INSERT INTO sys_tenant (
    id,
    org_id,
    tenant_name,
    tenant_code,
    contact_name,
    tenant_type,
    expire_time,
    user_limit,
    storage_limit,
    status,
    deleted,
    remark,
    create_time,
    update_time,
    create_by,
    update_by
)
SELECT
    2 as id,
    1 as org_id,  -- 关联根组织
    '系统租户' as tenant_name,
    'system' as tenant_code,
    'System Administrator' as contact_name,
    3 as tenant_type,  -- 企业版
    '2099-12-31 23:59:59' as expire_time,
    9999 as user_limit,
    102400 as storage_limit,  -- 100GB
    0 as status,  -- 启用
    0 as deleted,  -- 未删除
    '系统内置租户，用于平台管理员' as remark,
    NOW() as create_time,
    NOW() as update_time,
    'system' as create_by,
    'system' as update_by
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_tenant WHERE id = 2 AND deleted = 0
);

-- 4. 验证系统租户是否创建成功
SELECT '验证系统租户' as description, id, tenant_name, tenant_code, status, deleted
FROM sys_tenant
WHERE id = 2;

-- 5. 检查 admin 用户的租户ID
SELECT 'admin用户信息' as description,
       id, username, nickname, tenant_id, role_code, status, deleted
FROM sys_user
WHERE username = 'admin';

-- 6. 更新 admin 用户，确保属于系统租户（tenant_id=2）
UPDATE sys_user
SET tenant_id = 2,
    update_time = NOW(),
    update_by = 'system'
WHERE username = 'admin'
  AND (tenant_id IS NULL OR tenant_id != 2);

-- 7. 更新根组织（id=1），确保关联系统租户（tenant_id=2）
UPDATE sys_org
SET tenant_id = 2,
    update_time = NOW(),
    update_by = 'system'
WHERE id = 1
  AND (tenant_id IS NULL OR tenant_id != 2);

-- 8. 最终验证
SELECT '=== 最终验证 ===' as description;

SELECT '系统租户' as data_type, id, tenant_name, tenant_code, status
FROM sys_tenant
WHERE id = 2 AND deleted = 0;

SELECT 'admin用户' as data_type, id, username, tenant_id, role_code
FROM sys_user
WHERE username = 'admin';

SELECT '根组织' as data_type, id, org_name, tenant_id
FROM sys_org
WHERE id = 1 AND deleted = 0;
