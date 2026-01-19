import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码生成工具
 * 运行此类的 main 方法生成 BCrypt 密码哈希
 */
public class GeneratePassword {
    public static void main(String[] args) {
        String password = "admin123";
        String hash = BCrypt.hashpw(password);
        System.out.println("密码: " + password);
        System.out.println("BCrypt 哈希: " + hash);

        // 验证密码
        boolean matches = BCrypt.checkpw(password, hash);
        System.out.println("验证结果: " + matches);
    }
}
