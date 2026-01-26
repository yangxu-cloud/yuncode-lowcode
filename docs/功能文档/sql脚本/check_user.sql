-- =============================================
-- 检查用户数据
-- =============================================

USE yuncode_lowcode;

-- 1. 查看租户
SELECT * FROM sys_tenant;

-- 2. 查看用户（不显示密码）
SELECT id, tenant_id, username, nickname, status FROM sys_user;

-- 3. 如果没有用户或密码不对，重新插入管理员用户
DELETE FROM sys_user WHERE username = 'admin';

INSERT INTO `sys_user` (`id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `status`)
VALUES (1, 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', '系统管理员', 0);

-- 4. 验证插入结果
SELECT id, tenant_id, username, nickname, status FROM sys_user WHERE username = 'admin';

-- =============================================
-- 密码说明
-- =============================================
-- 用户名: admin
-- 密码: admin123
-- 加密方式: BCrypt
-- =============================================
