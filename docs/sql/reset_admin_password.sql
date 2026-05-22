-- =============================================
-- 重置管理员密码为 admin123
-- =============================================

USE yuncode_lowcode;

-- 删除旧的管理员用户（如果存在）
DELETE FROM sys_user WHERE username = 'admin';

-- 重新插入管理员用户
-- 密码: admin123
-- 加密方式: BCrypt ($2a$10$...)
INSERT INTO `sys_user` (
    `id`,
    `tenant_id`,
    `username`,
    `password`,
    `nickname`,
    `real_name`,
    `email`,
    `phone`,
    `status`,
    `remark`
) VALUES (
    1,                              -- id
    1,                              -- tenant_id (默认租户)
    'admin',                        -- username
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', -- password (admin123)
    '管理员',                       -- nickname
    '系统管理员',                   -- real_name
    'admin@yuncode.com',           -- email
    '13800138000',                  -- phone
    0,                              -- status (0正常)
    '系统默认管理员'                -- remark
);

-- =============================================
-- 验证数据
-- =============================================

SELECT id, tenant_id, username, nickname, status FROM sys_user WHERE username = 'admin';

-- =============================================
-- 登录信息
-- =============================================
-- 租户编码: default
-- 用户名: admin
-- 密码: admin123
-- =============================================

-- =============================================
-- 如果密码还是不对，可以手动生成新的 BCrypt 哈希
-- =============================================
-- 使用以下 Java 代码生成 BCrypt 哈希：
--
-- import cn.hutool.crypto.digest.BCrypt;
-- String hash = BCrypt.hashpw("admin123");
-- System.out.println(hash);
--
-- 或者使用在线工具：
-- https://bcrypt-generator.com/
-- 输入: admin123
-- 选择: $2a$10$ (BCrypt)
-- =============================================
