-- 更新数据的租户ID：将 tenant_id=0 的数据更新为 tenant_id=2
-- 执行前请确认：当前登录用户的租户ID是 2

-- 1. 更新组织表
UPDATE sys_org SET tenant_id = 2 WHERE tenant_id = 0;

-- 2. 更新用户表
UPDATE sys_user SET tenant_id = 2 WHERE tenant_id = 0;

-- 3. 验证更新结果
SELECT '组织数据' as table_name, COUNT(*) as count, tenant_id FROM sys_org GROUP BY tenant_id;
SELECT '用户数据' as table_name, COUNT(*) as count, tenant_id FROM sys_user GROUP BY tenant_id;
