-- =============================================
-- 修复管理员密码
-- =============================================

USE yuncode_lowcode;

-- 更新管理员密码（使用从日志中获取的正确哈希值）
UPDATE sys_user
SET password = '$2a$10$2nxyH9SZdqheJkj3oFF08exImJdGXE8MGn51ISTz1VlmKGQzUuEPy'
WHERE username = 'admin';

-- 验证更新
SELECT
    id,
    tenant_id,
    username,
    nickname,
    status,
    password
FROM sys_user
WHERE username = 'admin';

-- =============================================
-- 登录信息
-- =============================================
-- 租户编码: default
-- 用户名: admin
-- 密码: admin123
-- =============================================
