-- 修复 admin 用户的租户ID
-- 确保 admin 用户属于系统租户（tenant_id=2）

-- 1. 查看 admin 用户当前的状态
SELECT
    id,
    username,
    tenant_id,
    role_code,
    status,
    deleted
FROM sys_user
WHERE username = 'admin';

-- 2. 查看 admin 用户应该属于哪个租户（系统租户）
SELECT
    id,
    tenant_code,
    tenant_name,
    status
FROM sys_tenant
WHERE id = 2;

-- 3. 修复 admin 用户：确保属于系统租户（tenant_id=2），并设置为平台管理员
UPDATE sys_user
SET tenant_id = 2,
    role_code = 'PLATFORM_ADMIN'
WHERE username = 'admin';

-- 4. 验证修复结果
SELECT
    'admin用户信息' as description,
    id,
    username,
    tenant_id,
    role_code,
    status
FROM sys_user
WHERE username = 'admin';

-- 5. 验证系统租户下有哪些用户
SELECT
    '系统租户用户列表' as description,
    id,
    username,
    tenant_id,
    role_code,
    status
FROM sys_user
WHERE tenant_id = 2 AND deleted = 0;
