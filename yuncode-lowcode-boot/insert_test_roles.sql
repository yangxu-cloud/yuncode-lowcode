-- 插入具体角色（role_type=2）
-- 注意：需要先确认"用户"分类的ID（从上面的查询结果中获取）

-- 假设"用户"分类的ID是1，插入几个具体角色
INSERT INTO sys_role (tenant_id, parent_id, role_type, role_name, role_code, description, sort_order, status, deleted, create_time, update_time, create_by, update_by)
VALUES
(2, 1, 2, '管理员', 'admin', '系统管理员，拥有所有权限', 1, 0, 0, NOW(), NOW(), 'system', 'system'),
(2, 1, 2, '普通用户', 'user', '普通用户角色', 2, 0, 0, NOW(), NOW(), 'system', 'system'),
(2, 1, 2, '访客', 'guest', '访客角色，只读权限', 3, 0, 0, NOW(), NOW(), 'system', 'system');

-- 查询确认
SELECT id, tenant_id, parent_id, role_type, role_name, role_code
FROM sys_role
WHERE deleted = 0 AND role_type = 2
ORDER BY sort_order;
