-- =============================================
-- 功能验证脚本：添加公司即创建租户
-- 日期: 2025-01-28
-- 说明: 验证组织驱动的多租户架构功能
-- =============================================

-- =============================================
-- 验证步骤 1：检查数据库表结构
-- =============================================

-- 1.1 检查 sys_user 表是否有 role_code 字段
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'yuncode_lowcode'
AND TABLE_NAME = 'sys_user'
AND COLUMN_NAME = 'role_code';

-- 1.2 检查 sys_org 表是否有 tenant_code 字段
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'yuncode_lowcode'
AND TABLE_NAME = 'sys_org'
AND COLUMN_NAME = 'tenant_code';

-- 1.3 检查 sys_tenant 表是否有 org_id 字段
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'yuncode_lowcode'
AND TABLE_NAME = 'sys_tenant'
AND COLUMN_NAME = 'org_id';

-- =============================================
-- 验证步骤 2：检查现有数据
-- =============================================

-- 2.1 查看所有用户及其角色
SELECT
    id,
    username,
    real_name,
    role_code,
    tenant_id,
    status
FROM sys_user
WHERE deleted = 0
ORDER BY id;

-- 2.2 查看组织结构
SELECT
    id,
    org_name,
    org_code,
    tenant_code,
    org_type,
    tenant_id,
    parent_id
FROM sys_org
WHERE deleted = 0
ORDER BY parent_id, sort_order;

-- 2.3 查看租户列表
SELECT
    id,
    org_id,
    tenant_name,
    tenant_code,
    tenant_type,
    user_limit,
    storage_limit,
    status
FROM sys_tenant
WHERE deleted = 0;

-- =============================================
-- 验证步骤 3：测试添加公司（租户）
-- =============================================

-- 说明：此步骤需要通过前端界面测试
-- 1. 使用 admin 账号登录（role_code = PLATFORM_ADMIN）
-- 2. 进入组织管理页面
-- 3. 在根节点下点击"添加子组织"
-- 4. 组织类型选择"公司"
-- 5. 填写信息：
--    - 组织名称：测试公司A
--    - 组织编码：TEST001
--    - 租户编码：test_company_a
--    - 租户类型：标准版（1）
--    - 用户数限制：50
--    - 存储空间限制：5120 MB
-- 6. 点击保存
-- 7. 执行下面的验证SQL查看结果

-- 验证3.1：检查是否创建了租户记录
SELECT * FROM sys_tenant WHERE tenant_code = 'test_company_a';

-- 验证3.2：检查是否创建了组织记录
SELECT * FROM sys_org WHERE tenant_code = 'test_company_a';

-- 验证3.3：检查组织和租户的关联关系
SELECT
    o.id as org_id,
    o.org_name,
    o.tenant_code as org_tenant_code,
    o.tenant_id as org_tenant_id,
    t.id as tenant_id,
    t.tenant_code as tenant_tenant_code,
    t.org_id as tenant_org_id
FROM sys_org o
LEFT JOIN sys_tenant t ON o.tenant_id = t.id
WHERE o.tenant_code = 'test_company_a';

-- =============================================
-- 验证步骤 4：测试添加部门（继承租户）
-- =============================================

-- 说明：此步骤需要通过前端界面测试
-- 1. 在刚创建的"测试公司A"下点击"添加子组织"
-- 2. 组织类型选择"部门"
-- 3. 填写信息：
--    - 组织名称：研发部
--    - 组织编码：RD001
-- 4. 点击保存
-- 5. 执行下面的验证SQL

-- 验证4.1：检查部门是否继承了租户信息
SELECT
    id,
    org_name,
    org_code,
    tenant_code,
    tenant_id,
    org_type,
    parent_id
FROM sys_org
WHERE org_code = 'RD001';

-- 预期结果：
-- - tenant_code 应该与父公司相同（test_company_a）
-- - tenant_id 应该与父公司相同
-- - org_type = 2（部门）

-- =============================================
-- 验证步骤 5：测试用户登录
-- =============================================

-- 说明：此步骤需要通过前端界面测试
-- 1. 在"测试公司A"下创建一个测试用户
-- 2. 使用租户编码 + 用户名 + 密码登录
-- 3. 验证登录成功后，用户的 tenant_id 是否正确

-- 5.1 查看测试用户信息
SELECT
    id,
    username,
    real_name,
    role_code,
    tenant_id
FROM sys_user
WHERE username = 'test_user';

-- 预期结果：
-- - tenant_id 应该等于"测试公司A"的租户ID
-- - role_code 应该是 NORMAL 或 TENANT_ADMIN

-- =============================================
-- 验证步骤 6：测试权限控制
-- =============================================

-- 6.1 测试平台管理员权限
-- 使用 admin 账号登录，应该可以添加公司（创建租户）

-- 6.2 测试普通用户权限
-- 创建一个普通用户（role_code = NORMAL），尝试添加公司
-- 预期结果：应该被拒绝，提示"需要平台管理员权限"

-- =============================================
-- 常见问题排查
-- =============================================

-- 问题1：添加公司后没有创建租户记录
-- 排查SQL：
SELECT * FROM sys_tenant ORDER BY id DESC LIMIT 5;
SELECT * FROM sys_org WHERE org_type = 1 ORDER BY id DESC LIMIT 5;

-- 问题2：部门没有继承租户信息
-- 排查SQL：
SELECT
    o.id,
    o.org_name,
    o.tenant_code,
    o.tenant_id,
    p.org_name as parent_name,
    p.tenant_code as parent_tenant_code,
    p.tenant_id as parent_tenant_id
FROM sys_org o
LEFT JOIN sys_org p ON o.parent_id = p.id
WHERE o.org_type = 2;

-- 问题3：用户登录时找不到租户
-- 排查SQL：
-- 查看该租户编码是否存在
SELECT * FROM sys_org WHERE tenant_code = 'your_tenant_code';
SELECT * FROM sys_tenant WHERE tenant_code = 'your_tenant_code';

-- =============================================
-- 清理测试数据（如需要）
-- =============================================

-- 删除测试组织（级联删除子组织和用户关联）
-- 注意：需要先删除子组织，再删除父组织
-- DELETE FROM sys_user_org WHERE org_id IN (SELECT id FROM sys_org WHERE tenant_code = 'test_company_a');
-- DELETE FROM sys_org WHERE tenant_code = 'test_company_a';
-- DELETE FROM sys_tenant WHERE tenant_code = 'test_company_a';

-- =============================================
-- 验证完成检查清单
-- =============================================

-- ✅ 数据库表结构检查
--   □ sys_user.role_code 字段存在
--   □ sys_org.tenant_code 字段存在
--   □ sys_tenant.org_id 字段存在

-- ✅ 功能测试
--   □ 可以添加公司（创建租户）
--   □ 公司节点有正确的 tenant_code
--   □ sys_org 和 sys_tenant 正确关联
--   □ 可以添加部门（继承租户信息）
--   □ 部门的 tenant_code 和 tenant_id 与父公司一致

-- ✅ 权限测试
--   □ admin 用户（PLATFORM_ADMIN）可以添加公司
--   □ 普通用户（NORMAL）不能添加公司

-- ✅ 登录测试
--   □ 可以使用租户编码登录
--   □ 登录后 tenant_id 正确存储在 Session
--   □ 登录后 roleCode 正确存储在前端

-- =============================================
