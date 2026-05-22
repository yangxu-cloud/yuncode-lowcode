package com.yuncode.common.exception;

import lombok.Getter;

/**
 * 参数校验异常
 * 用于处理请求参数校验失败的异常情况
 *
 * 使用场景：
 * - @Valid 注解校验失败
 * - 手动参数校验失败
 * - 参数格式错误、类型错误等
 *
 * @author yuncode
 */
@Getter
public class ParamException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 参数字段名
     */
    private final String fieldName;

    /**
     * 参数值
     */
    private final Object fieldValue;

    /**
     * 构造函数 - 使用错误码枚举
     */
    public ParamException(ErrorCode errorCode) {
        super(errorCode);
        this.fieldName = null;
        this.fieldValue = null;
    }

    /**
     * 构造函数 - 使用错误码枚举 + 自定义消息
     */
    public ParamException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.fieldName = null;
        this.fieldValue = null;
    }

    /**
     * 构造函数 - 指定字段名和错误消息
     */
    public ParamException(String fieldName, String message) {
        super(ErrorCode.PARAM_INVALID, message);
        this.fieldName = fieldName;
        this.fieldValue = null;
    }

    /**
     * 构造函数 - 指定字段名、字段值和错误消息
     */
    public ParamException(String fieldName, Object fieldValue, String message) {
        super(ErrorCode.PARAM_INVALID, message);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * 构造函数 - 使用错误码枚举 + 字段名
     */
    public ParamException(ErrorCode errorCode, String fieldName, String message) {
        super(errorCode, message);
        this.fieldName = fieldName;
        this.fieldValue = null;
    }

    /**
     * 构造函数 - 使用错误码枚举 + 字段名 + 字段值
     */
    public ParamException(ErrorCode errorCode, String fieldName, Object fieldValue, String message) {
        super(errorCode, message);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * 获取字段的完整错误消息
     */
    @Override
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();
        if (fieldName != null) {
            sb.append("字段 [").append(fieldName).append("]");
            if (fieldValue != null) {
                sb.append(" (值: ").append(fieldValue).append(")");
            }
            sb.append(" ");
        }
        sb.append(getMessage());
        return sb.toString();
    }

    /**
     * 静态工厂方法 - 创建参数缺失异常
     */
    public static ParamException missing(String fieldName) {
        return new ParamException(fieldName, String.format("参数 [%s] 不能为空", fieldName));
    }

    /**
     * 静态工厂方法 - 创建参数无效异常
     */
    public static ParamException invalid(String fieldName, String reason) {
        return new ParamException(fieldName, String.format("参数 [%s] 无效: %s", fieldName, reason));
    }

    /**
     * 静态工厂方法 - 创建参数格式错误异常
     */
    public static ParamException formatError(String fieldName, String format) {
        return new ParamException(
            ErrorCode.PARAM_FORMAT_ERROR,
            fieldName,
            String.format("参数 [%s] 格式错误，期望格式: %s", fieldName, format)
        );
    }

    /**
     * 静态工厂方法 - 创建参数类型错误异常
     */
    public static ParamException typeError(String fieldName, String expectedType) {
        return new ParamException(
            ErrorCode.PARAM_TYPE_ERROR,
            fieldName,
            String.format("参数 [%s] 类型错误，期望类型: %s", fieldName, expectedType)
        );
    }
}
