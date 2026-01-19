package com.yuncode.admin;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码生成工具
 * 运行此方法生成正确的 BCrypt 密码哈希
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        // 生成密码哈希
        String password = "admin123";
        String hash = BCrypt.hashpw(password);

        System.out.println("=================================");
        System.out.println("密码: " + password);
        System.out.println("BCrypt 哈希: " + hash);
        System.out.println("=================================");
        System.out.println("\nSQL 更新语句:");
        System.out.println("UPDATE sys_user SET password = '" + hash + "' WHERE username = 'admin';");
        System.out.println("\n=================================");

        // 验证密码
        boolean matches = BCrypt.checkpw(password, hash);
        System.out.println("验证结果: " + matches);
        System.out.println("=================================");

        // 再生成一个用于测试
        String hash2 = BCrypt.hashpw(password);
        System.out.println("\n第二个哈希（每次都不同，但都能验证成功）: " + hash2);
        System.out.println("验证第二个哈希: " + BCrypt.checkpw(password, hash2));
    }
}
