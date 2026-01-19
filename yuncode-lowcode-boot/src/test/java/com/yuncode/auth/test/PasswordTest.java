package com.yuncode.auth.test;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.crypto.digest.DigestUtil;
import org.junit.jupiter.api.Test;

/**
 * 密码哈希测试工具
 */
public class PasswordTest {

    @Test
    public void testPasswordHash() {
        String password = "admin123";

        // 方法1: 使用 Hutool 的 BCrypt (当前项目使用的方法)
        String hash1 = BCrypt.hashpw(password);
        System.out.println("方法1 (BCrypt.hashpw):");
        System.out.println(hash1);
        System.out.println("验证: " + BCrypt.checkpw(password, hash1));
        System.out.println();

        // 方法2: 使用 BCrypt.hashpw 并指定 salt
        String hash2 = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("方法2 (BCrypt.hashpw with gensalt):");
        System.out.println(hash2);
        System.out.println("验证: " + BCrypt.checkpw(password, hash2));
        System.out.println();

        // 方法3: 手动生成 (10轮)
        String hash3 = BCrypt.hashpw(password, "$2a$10$" + BCrypt.gensalt().substring(7));
        System.out.println("方法3 (手动指定10轮):");
        System.out.println(hash3);
        System.out.println("验证: " + BCrypt.checkpw(password, hash3));
        System.out.println();

        // 测试数据库中的哈希
        String dbHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        System.out.println("数据库中的哈希验证:");
        System.out.println("验证 admin123: " + BCrypt.checkpw("admin123", dbHash));
        System.out.println("验证 admin: " + BCrypt.checkpw("admin", dbHash));
        System.out.println("验证 admin123456: " + BCrypt.checkpw("admin123456", dbHash));
    }
}
