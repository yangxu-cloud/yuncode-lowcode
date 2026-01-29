-- 诊断用户组织关系问题
-- 执行此查询来查看用户和组织的实际状态

-- 替换这两个值为实际的用户ID和组织ID
-- SET @userId = 1;  -- 替换为实际的用户ID
-- SET @orgId = 26;  -- 替换为实际的组织ID

-- 1. 查看用户信息
SELECT
    id,
    username,
    nickname,
    real_name,
    tenant_id,
    company_id,
    dept_id,
    role_code,
    status,
    deleted
FROM sys_user
WHERE username = 'zs1';  -- 替换为实际的用户名

-- 2. 查看组织信息
SELECT
    id,
    org_name,
    org_code,
    org_type,
    is_company,
    company_id,
    tenant_id,
    parent_id,
    status,
    deleted
FROM sys_org
WHERE id = 26;  -- 替换为实际的组织ID

-- 3. 查看用户组织关系是否存在
SELECT
    id,
    user_id,
    org_id,
    is_leader,
    is_main_dept,
    tenant_id,
    deleted
FROM sys_user_org
WHERE user_id = (SELECT id FROM sys_user WHERE username = 'zs1')  -- 替换为实际的用户名
  AND org_id = 26;  -- 替换为实际的组织ID

-- 4. 查看该用户的所有组织关系
SELECT
    uo.id,
    uo.user_id,
    uo.org_id,
    uo.is_leader,
    uo.is_main_dept,
    uo.tenant_id,
    o.org_name,
    o.org_type,
    o.is_company
FROM sys_user_org uo
LEFT JOIN sys_org o ON uo.org_id = o.id
WHERE uo.user_id = (SELECT id FROM sys_user WHERE username = 'zs1')  -- 替换为实际的用户名
  AND uo.deleted = 0;

-- 5. 检查是否有重复的用户组织关系（相同 user_id, org_id, tenant_id）
SELECT
    user_id,
    org_id,
    tenant_id,
    COUNT(*) as count
FROM sys_user_org
WHERE deleted = 0
GROUP BY user_id, org_id, tenant_id
HAVING COUNT(*) > 1;
