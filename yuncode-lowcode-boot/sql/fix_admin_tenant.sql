-- 修复 admin 用户的租户ID
-- 问题：之前运行的脚本将 admin 用户的 tenant_id 从 0 改成了 2
-- 解决：将 admin 用户的 tenant_id 恢复为 0（系统租户）

-- 1. 查看 admin 用户当前的租户ID
SELECT id, username, tenant_id, role_code FROM sys_user WHERE username = 'admin';

-- 2. 修复 admin 用户的租户ID
UPDATE sys_user
SET tenant_id = 0, role_code = 'PLATFORM_ADMIN'
WHERE username = 'admin';

-- 3. 验证修复结果
SELECT id, username, tenant_id, role_code FROM sys_user WHERE username = 'admin';

-- 4. 检查系统租户（tenant_id=0）的数据量
SELECT '系统租户用户数' as description, COUNT(*) as count
FROM sys_user
WHERE tenant_id = 0;
