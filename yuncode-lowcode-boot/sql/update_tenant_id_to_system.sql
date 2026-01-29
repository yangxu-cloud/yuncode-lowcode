-- 将现有数据的租户ID从0更新为2（匹配system租户）
-- 执行前请确认：当前登录使用的是 system 租户（id=2）

USE yuncode_lowcode;

-- 1. 更新组织表：将 tenant_id=0 的数据更新为 2
UPDATE sys_org
SET tenant_id = 2
WHERE tenant_id = 0;

-- 2. 更新用户表：将 tenant_id=0 的数据更新为 2
UPDATE sys_user
SET tenant_id = 2
WHERE tenant_id = 0;

-- 3. 更新用户-组织关系表：将 tenant_id=0 的数据更新为 2
UPDATE sys_user_org
SET tenant_id = 2
WHERE tenant_id = 0;

-- 4. 验证更新结果
SELECT '=== 租户表数据 ===' AS info;
SELECT id, tenant_code, tenant_name FROM sys_tenant ORDER BY id;

SELECT '=== 组织数据的租户ID分布（更新后）===' AS info;
SELECT tenant_id, COUNT(*) as count FROM sys_org WHERE deleted = 0 GROUP BY tenant_id;

SELECT '=== 用户数据的租户ID分布（更新后）===' AS info;
SELECT tenant_id, COUNT(*) as count FROM sys_user WHERE deleted = 0 GROUP BY tenant_id;

SELECT '=== 用户-组织关系数据的租户ID分布（更新后）===' AS info;
SELECT tenant_id, COUNT(*) as count FROM sys_user_org GROUP BY tenant_id;
