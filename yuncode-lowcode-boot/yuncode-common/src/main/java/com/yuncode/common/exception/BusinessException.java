package com.yuncode.common.exception;

/**
 * 业务异常
 * 用于处理业务逻辑异常
 *
 * 使用场景：
 * - 业务规则校验失败
 * - 数据不存在
 * - 数据冲突
 * - 权限不足
 * - 状态不允许等业务异常
 *
 * @author yuncode
 */
public class BusinessException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造函数 - 使用错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 构造函数 - 使用错误码枚举 + 自定义消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造函数 - 使用错误码枚举 + 自定义消息 + 详细信息
     */
    public BusinessException(ErrorCode errorCode, String message, String detail) {
        super(errorCode, message, detail);
    }

    /**
     * 构造函数 - 使用错误码枚举 + 原始异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 构造函数 - 使用错误码枚举 + 自定义消息 + 原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 构造函数 - 直接指定错误码和消息（不使用枚举）
     * 兼容旧代码
     */
    public BusinessException(Integer code, String message) {
        super(code, message);
    }

    /**
     * 构造函数 - 直接指定错误码和消息 + 详细信息
     * 兼容旧代码
     */
    public BusinessException(Integer code, String message, String detail) {
        super(code, message, detail);
    }

    /**
     * 构造函数 - 直接指定错误码和消息 + 原始异常
     * 兼容旧代码
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * 构造函数 - 只指定消息（默认500错误码）
     * 兼容旧代码
     */
    public BusinessException(String message) {
        this(500, message);
    }

    /**
     * 构造函数 - 指定消息 + 原始异常（默认500错误码）
     * 兼容旧代码
     */
    public BusinessException(String message, Throwable cause) {
        this(500, message, cause);
    }

    // ========== 静态工厂方法 - 常用业务异常 ==========

    /**
     * 数据不存在
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message);
    }

    /**
     * 数据已存在
     */
    public static BusinessException alreadyExists(String message) {
        return new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, message);
    }

    /**
     * 操作失败
     */
    public static BusinessException failed(String message) {
        return new BusinessException(ErrorCode.OPERATION_FAILED, message);
    }

    /**
     * 无权访问
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message);
    }

    /**
     * 不允许的操作
     */
    public static BusinessException notAllowed(String message) {
        return new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, message);
    }
}
