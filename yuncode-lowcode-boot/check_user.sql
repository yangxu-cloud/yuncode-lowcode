-- 检查用户记录
SELECT id, username, real_name, tenant_id, deleted, status
FROM sys_user
WHERE id = 2016895884431990785;

-- 检查角色-用户关联记录
SELECT * FROM sys_role_user
WHERE role_id = 3 AND user_id = 2016895884431990785;

-- 检查所有 sys_role_user 中的用户
SELECT ru.role_id, ru.user_id, u.username, u.real_name, u.tenant_id, u.deleted
FROM sys_role_user ru
LEFT JOIN sys_user u ON ru.user_id = u.id
WHERE ru.role_id = 3;

-- 检查当前租户的所有用户
SELECT id, username, real_name, tenant_id, status
FROM sys_user
WHERE tenant_id = 2 AND deleted = 0
LIMIT 10;
