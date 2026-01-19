package com.yuncode.auth.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码生成器
 * 用于生成 BCrypt 密码哈希
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        String password = "admin123";

        System.out.println("===========================================");
        System.out.println("BCrypt 密码哈希生成器");
        System.out.println("===========================================");
        System.out.println("原始密码: " + password);
        System.out.println();

        // 生成多个哈希，每次都会不同（因为salt是随机的）
        System.out.println("生成的 BCrypt 哈希值（可任选一个使用）:");
        System.out.println();

        for (int i = 1; i <= 5; i++) {
            String hash = BCrypt.hashpw(password);
            System.out.println(i + ". " + hash);

            // 立即验证
            boolean verified = BCrypt.checkpw(password, hash);
            System.out.println("   验证结果: " + (verified ? "✓ 通过" : "✗ 失败"));
            System.out.println();
        }

        // 测试数据库中现有的哈希
        String dbHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        System.out.println("===========================================");
        System.out.println("数据库中现有哈希的测试:");
        System.out.println("哈希值: " + dbHash);
        System.out.println("验证 admin123: " + BCrypt.checkpw("admin123", dbHash));
        System.out.println("验证 admin: " + BCrypt.checkpw("admin", dbHash));
        System.out.println();

        // 生成 SQL 更新语句
        System.out.println("===========================================");
        System.out.println("SQL 更新语句（复制上面任意一个哈希值）:");
        System.out.println("===========================================");
        String newHash = BCrypt.hashpw(password);
        System.out.println("-- 使用新生成的哈希更新密码");
        System.out.println("UPDATE sys_user");
        System.out.println("SET password = '" + newHash + "'");
        System.out.println("WHERE username = 'admin' AND tenant_id = 2;");
        System.out.println();
        System.out.println("-- 验证更新");
        System.out.println("SELECT id, tenant_id, username, password, nickname, status");
        System.out.println("FROM sys_user");
        System.out.println("WHERE username = 'admin' AND tenant_id = 2;");
    }
}
