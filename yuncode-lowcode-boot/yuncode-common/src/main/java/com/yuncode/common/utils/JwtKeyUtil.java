package com.yuncode.common.utils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * JWT 密钥工具类
 *
 * 统一管理 Gateway 和 Sa-Token 的 JWT 密钥推导逻辑。
 * 三种登录类型（admin/user/tenant）使用不同的签名密钥：
 * - admin：直接使用 baseSecretKey
 * - user：baseSecretKey + "_user" 后缀
 * - tenant：baseSecretKey + "_tenant" 后缀
 *
 * 所有模块（Gateway + Admin）必须使用同一套推导规则，
 * 否则 token 验证会失败。
 */
public final class JwtKeyUtil {

    private JwtKeyUtil() {}

    /** 登录类型常量 */
    public static final String LOGIN_TYPE_ADMIN = "admin";
    public static final String LOGIN_TYPE_USER = "user";
    public static final String LOGIN_TYPE_TENANT = "tenant";

    /** 密钥后缀 */
    private static final String SUFFIX_ADMIN = "";
    private static final String SUFFIX_USER = "_user";
    private static final String SUFFIX_TENANT = "_tenant";

    /**
     * 根据基础密钥和登录类型生成对应密钥字符串
     */
    public static String deriveSecretKey(String baseSecretKey, String loginType) {
        if (baseSecretKey == null || baseSecretKey.isEmpty()) {
            throw new IllegalArgumentException("baseSecretKey 不能为空");
        }
        return switch (loginType) {
            case LOGIN_TYPE_ADMIN -> baseSecretKey;
            case LOGIN_TYPE_USER -> baseSecretKey + SUFFIX_USER;
            case LOGIN_TYPE_TENANT -> baseSecretKey + SUFFIX_TENANT;
            default -> baseSecretKey + "_" + loginType;
        };
    }

    /**
     * 生成 HMAC-SHA 密钥
     */
    public static SecretKey generateHmacKey(String baseSecretKey, String loginType) {
        String key = deriveSecretKey(baseSecretKey, loginType);
        return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * 检查密钥是否是默认/不安全的密钥
     */
    public static boolean isDefaultKey(String secretKey) {
        return secretKey == null
            || secretKey.isEmpty()
            || secretKey.startsWith("yuncode-lowcode-sa-token-jwt-secret-key");
    }

    /**
     * 验证密钥安全性，不符合则抛异常
     */
    public static void validateKey(String secretKey, String moduleName) {
        if (isDefaultKey(secretKey)) {
            String msg = String.format(
                "[%s] JWT 密钥安全错误！请通过环境变量 JWT_SECRET_KEY 设置足够长的随机密钥（至少32字节）",
                moduleName);
            throw new IllegalStateException(msg);
        }
    }
}
