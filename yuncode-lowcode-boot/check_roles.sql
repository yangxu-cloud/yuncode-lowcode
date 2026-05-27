-- 检查角色表数据
SELECT
    id,
    tenant_id,
    parent_id,
    role_type,
    role_name,
    role_code,
    description,
    sort_order,
    status,
    deleted
FROM sys_role
WHERE deleted = 0
ORDER BY parent_id, role_type, sort_order;

-- 检查是否有 role_type = 2 的具体角色
SELECT
    id,
    tenant_id,
    parent_id,
    role_type,
    role_name,
    role_code
FROM sys_role
WHERE deleted = 0 AND role_type = 2;

-- 检查每个分类下有多少个具体角色
SELECT
    p.id as category_id,
    p.role_name as category_name,
    p.tenant_id as category_tenant_id,
    COUNT(r.id) as role_count
FROM sys_role p
LEFT JOIN sys_role r ON r.parent_id = p.id AND r.deleted = 0 AND r.role_type = 2
WHERE p.deleted = 0 AND p.role_type = 1
GROUP BY p.id, p.role_name, p.tenant_id;
