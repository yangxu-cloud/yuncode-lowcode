package com.yuncode.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 * 统一管理系统所有错误码
 *
 * 错误码规则：
 * - 1xx：信息提示
 * - 2xx：操作成功
 * - 4xx：客户端错误（参数错误、权限不足等）
 * - 5xx：服务端错误（系统异常、业务异常等）
 *
 * @author yuncode
 */
@Getter
public enum ErrorCode {

    // ========== 通用错误码 1xxx ==========
    SUCCESS(200, "操作成功"),
    OPERATION_FAILED(500, "操作失败"),

    // ========== 客户端错误 4xxx ==========

    // 401 认证相关
    UNAUTHORIZED(401, "未登录，请先登录"),
    TOKEN_INVALID(401, "Token无效或已过期"),
    TOKEN_MISSING(401, "缺少Token"),
    LOGIN_EXPIRED(401, "登录已过期，请重新登录"),
    ACCOUNT_LOCKED(401, "账号已被锁定"),
    ACCOUNT_DISABLED(401, "账号已被禁用"),

    // 403 权限相关
    FORBIDDEN(403, "无权访问"),
    NO_PERMISSION(403, "权限不足"),
    ROLE_NOT_ENOUGH(403, "角色不足"),

    // 400 参数相关
    BAD_REQUEST(400, "请求参数错误"),
    PARAM_INVALID(400, "参数校验失败"),
    PARAM_MISSING(400, "缺少必要参数"),
    PARAM_TYPE_ERROR(400, "参数类型错误"),
    PARAM_FORMAT_ERROR(400, "参数格式错误"),

    // 404 资源相关
    NOT_FOUND(404, "资源不存在"),
    USER_NOT_FOUND(404, "用户不存在"),
    TENANT_NOT_FOUND(404, "租户不存在"),
    ROLE_NOT_FOUND(404, "角色不存在"),
    MENU_NOT_FOUND(404, "菜单不存在"),
    DEPT_NOT_FOUND(404, "部门不存在"),

    // 409 冲突相关
    CONFLICT(409, "资源冲突"),
    DATA_ALREADY_EXISTS(409, "数据已存在"),
    USERNAME_ALREADY_EXISTS(409, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(409, "邮箱已存在"),
    PHONE_ALREADY_EXISTS(409, "手机号已存在"),

    // ========== 服务端错误 5xxx ==========

    // 500 系统异常
    INTERNAL_ERROR(500, "系统内部错误"),
    SYSTEM_ERROR(500, "系统异常，请联系管理员"),
    DATABASE_ERROR(500, "数据库操作失败"),
    NETWORK_ERROR(500, "网络异常"),
    FILE_UPLOAD_ERROR(500, "文件上传失败"),
    FILE_DOWNLOAD_ERROR(500, "文件下载失败"),

    // 503 服务不可用
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    MAINTENANCE_MODE(503, "系统维护中"),

    // ========== 业务错误码 6xxx ==========
    // 业务错误码使用 60xx - 69xx，可根据业务模块继续细分

    // 6000 用户相关
    USER_PASSWORD_ERROR(6000, "用户名或密码错误"),
    USER_OLD_PASSWORD_ERROR(6001, "原密码错误"),
    USER_PASSWORD_SAME(6002, "新密码不能与原密码相同"),
    USER_PASSWORD_WEAK(6003, "密码强度太低"),
    USER_NOT_LOGIN(6004, "用户未登录"),

    // 6010 租户相关
    TENANT_CODE_EMPTY(6010, "租户编码不能为空"),
    TENANT_CODE_INVALID(6011, "租户编码无效"),
    TENANT_EXPIRED(6012, "租户已过期"),
    TENANT_DISABLED(6013, "租户已禁用"),
    TENANT_LIMIT_EXCEEDED(6014, "租户用户数量超限"),

    // 6020 登录相关
    LOGIN_FAILED(6020, "登录失败"),
    LOGIN_LOCKED(6021, "账号已锁定，请稍后再试"),
    LOGIN_CAPTCHA_ERROR(6022, "验证码错误"),
    LOGIN_CAPTCHA_EXPIRED(6023, "验证码已过期"),
    LOGIN_TOO_MANY_ATTEMPTS(6024, "登录尝试次数过多，请稍后再试"),

    // 6030 数据相关
    DATA_NOT_FOUND(6030, "数据不存在"),
    DATA_ALREADY_DELETED(6031, "数据已被删除"),
    DATA_IN_USE(6032, "数据正在使用中，无法删除"),
    DATA_HAS_CHILDREN(6033, "存在子级数据，无法删除"),

    // 6040 操作相关
    OPERATION_NOT_ALLOWED(6040, "不允许执行此操作"),
    OPERATION_IN_PROGRESS(6041, "操作正在进行中，请勿重复提交"),
    OPERATION_TIMEOUT(6042, "操作超时"),
    BATCH_OPERATION_FAILED(6043, "批量操作部分失败"),

    // 6050 文件相关
    FILE_NOT_FOUND(6050, "文件不存在"),
    FILE_TYPE_NOT_ALLOWED(6051, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(6052, "文件大小超出限制"),
    FILE_UPLOAD_FAILED(6053, "文件上传失败"),

    // 6060 导入导出
    IMPORT_FAILED(6060, "数据导入失败"),
    IMPORT_FORMAT_ERROR(6061, "导入文件格式错误"),
    IMPORT_DATA_ERROR(6062, "导入数据有误"),
    EXPORT_FAILED(6063, "数据导出失败"),
    EXPORT_DATA_EMPTY(6064, "没有数据可导出"),

    // 6070 第三方服务
    THIRD_PARTY_ERROR(6070, "第三方服务调用失败"),
    SMS_SEND_FAILED(6071, "短信发送失败"),
    EMAIL_SEND_FAILED(6072, "邮件发送失败"),
    PAYMENT_ERROR(6073, "支付失败"),
    PAYMENT_TIMEOUT(6074, "支付超时");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误描述
     */
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据错误码获取枚举
     */
    public static ErrorCode of(Integer code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
