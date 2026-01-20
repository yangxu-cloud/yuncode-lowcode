package com.yuncode.common.exception;

import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 基础异常类
 * 所有自定义异常的父类
 *
 * @author yuncode
 */
@Getter
public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 错误详情（用于开发环境返回详细错误信息）
     */
    private final String detail;

    /**
     * 错误码枚举
     */
    private final ErrorCode errorCode;

    /**
     * 构造函数 - 使用错误码枚举
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * 构造函数 - 使用错误码枚举 + 自定义消息
     */
    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * 构造函数 - 使用错误码枚举 + 自定义消息 + 详细信息
     */
    public BaseException(ErrorCode errorCode, String message, String detail) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * 构造函数 - 使用错误码枚举 + 原始异常
     */
    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.errorCode = errorCode;
        this.detail = cause != null ? cause.getMessage() : null;
    }

    /**
     * 构造函数 - 使用错误码枚举 + 自定义消息 + 原始异常
     */
    public BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.message = message;
        this.errorCode = errorCode;
        this.detail = cause != null ? cause.getMessage() : null;
    }

    /**
     * 构造函数 - 直接指定错误码和消息（不使用枚举）
     */
    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.errorCode = null;
        this.detail = null;
    }

    /**
     * 构造函数 - 直接指定错误码和消息 + 详细信息
     */
    public BaseException(Integer code, String message, String detail) {
        super(message);
        this.code = code;
        this.message = message;
        this.errorCode = null;
        this.detail = detail;
    }

    /**
     * 构造函数 - 直接指定错误码和消息 + 原始异常
     */
    public BaseException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.errorCode = null;
        this.detail = cause != null ? cause.getMessage() : null;
    }

    /**
     * 获取完整的错误消息（包含详情）
     */
    public String getFullMessage() {
        if (StringUtils.hasText(detail)) {
            return message + ": " + detail;
        }
        return message;
    }

    @Override
    public String toString() {
        return "BaseException{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", detail='" + detail + '\'' +
                ", errorCode=" + (errorCode != null ? errorCode.name() : "null") +
                '}';
    }
}
