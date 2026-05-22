-- =============================================
-- 更新 system 租户管理员密码
-- =============================================

USE yuncode_lowcode;

-- 查看 system 租户的 ID
SELECT id, tenant_code, tenant_name FROM sys_tenant WHERE tenant_code = 'system';

-- 查看当前 system 租户的 admin 用户
SELECT id, tenant_id, username, password, nickname FROM sys_user WHERE username = 'admin' AND tenant_id = 2;

-- 更新 system 租户（tenant_id=2）的 admin 用户密码
-- 密码: admin123
-- 使用多个不同的 BCrypt 哈希尝试（可能需要根据实际使用的BCrypt库版本调整）

-- 方案1: 使用 $2a$10$ 格式
UPDATE sys_user
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi'
WHERE username = 'admin' AND tenant_id = 2;

-- 如果方案1不行，尝试方案2: 使用 $2b$10$ 格式（更标准的BCrypt）
-- UPDATE sys_user
-- SET password = '$2b$10$YourGeneratedHashHere'
-- WHERE username = 'admin' AND tenant_id = 2;

-- 如果方案2不行，尝试方案3: 使用 $2y$10$ 格式（某些BCrypt实现使用）
-- UPDATE sys_user
-- SET password = '$2y$10$YourGeneratedHashHere'
-- WHERE username = 'admin' AND tenant_id = 2;

-- =============================================
-- 验证更新
-- =============================================
SELECT id, tenant_id, username, password, nickname, status
FROM sys_user
WHERE username = 'admin' AND tenant_id = 2;

-- =============================================
-- 登录信息
-- =============================================
-- 租户编码: system
-- 用户名: admin
-- 密码: admin123
-- =============================================

-- =============================================
-- 如果以上都不行，需要重新生成 BCrypt 哈希
-- =============================================
-- 方法1: 在线生成器
-- 访问: https://bcrypt-generator.com/
-- 输入: admin123
-- 选择 rounds: 10
-- 选择 type: $2a$ 或 $2b$ 或 $2y$
--
-- 方法2: Java 代码生成
-- import cn.hutool.crypto.digest.BCrypt;
-- String hash = BCrypt.hashpw("admin123");
-- System.out.println(hash);
--
-- 方法3: 使用项目中的 PasswordTest
-- 运行: mvn test -Dtest=PasswordTest
-- =============================================
