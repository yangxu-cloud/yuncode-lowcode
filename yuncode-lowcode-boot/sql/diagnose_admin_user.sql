-- 诊断 admin 用户登录问题
-- 执行此查询来查看 admin 用户的实际状态

-- 1. 查看 admin 用户的完整信息
SELECT
    id,
    username,
    password,
    tenant_id,
    role_code,
    status,
    deleted
FROM sys_user
WHERE username = 'admin';

-- 2. 查看系统租户（id=0）的信息
SELECT id, tenant_code, tenant_name, status
FROM sys_tenant
WHERE id = 0;

-- 3. 查看数据库中存在哪些租户
SELECT id, tenant_code, tenant_name, status
FROM sys_tenant
ORDER BY id;

-- 4. 查看每个租户下有多少用户
SELECT tenant_id, COUNT(*) as user_count
FROM sys_user
WHERE deleted = 0
GROUP BY tenant_id
ORDER BY tenant_id;
