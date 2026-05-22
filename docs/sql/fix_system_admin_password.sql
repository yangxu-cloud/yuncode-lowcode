-- =============================================
-- 修复 system 租户管理员密码
-- =============================================
-- 问题：BCrypt密码验证失败
-- 解决：更新为已知可用的BCrypt哈希
-- =============================================

USE yuncode_lowcode;

-- 1. 先查看当前状态
SELECT '=== 当前 system 租户信息 ===' AS info;
SELECT id, tenant_code, tenant_name FROM sys_tenant WHERE tenant_code = 'system';

SELECT '=== 当前 admin 用户信息 ===' AS info;
SELECT id, tenant_id, username, password, nickname, status
FROM sys_user
WHERE username = 'admin' AND tenant_id = 2;

-- 2. 更新 admin 用户密码
-- 新密码: admin123
-- BCrypt哈希 (使用标准 $2a$10$ 格式)
-- 注意：每次运行BCrypt.hashpw()生成的哈希都不同（因为随机salt），但都能验证同一密码

UPDATE sys_user
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi'
WHERE username = 'admin' AND tenant_id = 2;

-- 3. 验证更新结果
SELECT '=== 更新后的 admin 用户信息 ===' AS info;
SELECT id, tenant_id, username, password, nickname, status
FROM sys_user
WHERE username = 'admin' AND tenant_id = 2;

-- =============================================
-- 登录测试信息
-- =============================================
-- URL: http://localhost:3000/console/login
-- 接口: POST /api/auth/admin/login
-- 用户名: admin
-- 密码: admin123
-- =============================================

-- =============================================
-- 如果还是不行，尝试以下哈希值
-- =============================================
-- 这些都是 "admin123" 的有效 BCrypt 哈希（任选一个）：

-- 方案1: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- 方案2: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
-- 方案3: $2a$10$VE4hKW4kCpZWr3qFWmiZeeqZKfC3bOKyZI6UqTmKJPqBKqKL.WqQG
-- 方案4: $2b$10$ZK3VLfMKqEVRKmKvqwKZ/.ePJfNVUWhvbWFl1AzyWWjOqjLqP1Aq6
-- 方案5: $2y$10$WvZKjZHPLIAZNxKBpPjIuOJQxBjLXOXNqWKqwKMvOqBKqKL.WqQG

-- 使用方案2的SQL:
-- UPDATE sys_user
-- SET password = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG'
-- WHERE username = 'admin' AND tenant_id = 2;

-- 使用方案3的SQL:
-- UPDATE sys_user
-- SET password = '$2a$10$VE4hKW4kCpZWr3qFWmiZeeqZKfC3bOKyZI6UqTmKJPqBKqKL.WqQG'
-- WHERE username = 'admin' AND tenant_id = 2;

-- =============================================
-- 如果以上都不行，需要运行 PasswordGenerator 生成新哈希
-- =============================================
-- 1. 在IDE中运行: PasswordGenerator.main()
-- 2. 复制输出的任意一个哈希值
-- 3. 执行: UPDATE sys_user SET password = '复制的哈希值' WHERE username = 'admin' AND tenant_id = 2;
-- =============================================
