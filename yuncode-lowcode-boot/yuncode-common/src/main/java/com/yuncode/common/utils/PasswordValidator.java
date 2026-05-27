package com.yuncode.common.utils;

import com.yuncode.common.exception.BusinessException;
import com.yuncode.common.exception.ErrorCode;

/**
 * 密码强度校验工具
 *
 * 校验规则：
 * <ul>
 *   <li>最小长度（默认 6 位）</li>
 *   <li>是否包含大写字母</li>
 *   <li>是否包含小写字母</li>
 *   <li>是否包含数字</li>
 *   <li>是否包含特殊字符</li>
 * </ul>
 */
public final class PasswordValidator {

    private PasswordValidator() {}

    private static final int DEFAULT_MIN_LENGTH = 6;

    public static void validate(String password) {
        validate(password, DEFAULT_MIN_LENGTH, false, false, false, false);
    }

    public static void validate(String password, int minLength,
                                 boolean requireUppercase, boolean requireLowercase,
                                 boolean requireNumber, boolean requireSpecial) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }

        if (password.length() < minLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "密码长度不能少于 " + minLength + " 位");
        }

        if (requireUppercase && !containsUppercase(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含大写字母");
        }

        if (requireLowercase && !containsLowercase(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含小写字母");
        }

        if (requireNumber && !containsDigit(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含数字");
        }

        if (requireSpecial && !containsSpecial(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含特殊字符");
        }
    }

    private static boolean containsUppercase(String s) {
        return s.chars().anyMatch(c -> c >= 'A' && c <= 'Z');
    }

    private static boolean containsLowercase(String s) {
        return s.chars().anyMatch(c -> c >= 'a' && c <= 'z');
    }

    private static boolean containsDigit(String s) {
        return s.chars().anyMatch(c -> c >= '0' && c <= '9');
    }

    private static boolean containsSpecial(String s) {
        return s.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
    }
}
