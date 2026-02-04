-- 系统租户（tenant_id=2）默认角色初始化脚本
-- 说明：系统租户的默认角色数据，包括角色分类和基础角色

-- 清理旧数据（可选，首次初始化时注释掉）
-- DELETE FROM sys_role WHERE tenant_id = 2;

-- 1. 创建"用户"角色分类
INSERT INTO sys_role (id, tenant_id, parent_id, role_name, role_code, role_type, description, sort_order, status, deleted, create_time, update_time, create_by, update_by)
VALUES (1, 2, 0, '用户', 'user_category', 1, NULL, 1, 0, 0, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    update_time = NOW();

-- 2. 创建"普通用户"角色
INSERT INTO sys_role (id, tenant_id, parent_id, role_name, role_code, role_type, description, sort_order, status, deleted, create_time, update_time, create_by, update_by)
VALUES (2, 2, 1, '普通用户', 'common_user', 2, '系统普通用户，拥有基本权限', 1, 0, 0, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    update_time = NOW();

-- 3. 创建"系统管理员"角色
INSERT INTO sys_role (id, tenant_id, parent_id, role_name, role_code, role_type, description, sort_order, status, deleted, create_time, update_time, create_by, update_by)
VALUES (3, 2, 1, '系统管理员', 'system_admin', 2, '系统管理员，拥有所有权限', 2, 0, 0, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    update_time = NOW();

-- 4. 创建"租户管理员"角色
INSERT INTO sys_role (id, tenant_id, parent_id, role_name, role_code, role_type, description, sort_order, status, deleted, create_time, update_time, create_by, update_by)
VALUES (4, 2, 1, '租户管理员', 'tenant_admin', 2, '租户管理员，管理租户内的用户和角色', 3, 0, 0, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    update_time = NOW();

-- 查询验证
SELECT id, tenant_id, parent_id, role_name, role_code, role_type, sort_order, status
FROM sys_role
WHERE tenant_id = 2
ORDER BY sort_order;
