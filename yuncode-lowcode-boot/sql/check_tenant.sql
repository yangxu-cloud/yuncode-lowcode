-- 查看当前租户数据
SELECT '=== 当前租户表数据 ===' AS info;
SELECT id, tenant_code, tenant_name, status FROM sys_tenant ORDER BY id;

-- 查看当前组织数据的租户ID分布
SELECT '=== 组织数据的租户ID分布 ===' AS info;
SELECT tenant_id, COUNT(*) as count FROM sys_org WHERE deleted = 0 GROUP BY tenant_id;

-- 查看当前用户数据的租户ID分布
SELECT '=== 用户数据的租户ID分布 ===' AS info;
SELECT tenant_id, COUNT(*) as count FROM sys_user WHERE deleted = 0 GROUP BY tenant_id;
