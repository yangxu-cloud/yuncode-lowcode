package com.yuncode.admin.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.yuncode.common.exception.*;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.common.util.ExceptionLogUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统所有异常，返回标准的响应格式
 *
 * @author yuncode
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    // ========== 基础异常 ==========

    /**
     * 业务异常
     * 业务规则校验失败、数据冲突、权限不足等
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        // 记录日志（不记录堆栈）
        ExceptionLogUtil.logBusinessException(e, "业务异常");

        // 返回错误信息
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     * 请求参数格式错误、类型错误等
     */
    @ExceptionHandler(ParamException.class)
    public Result<Void> handleParamException(ParamException e, HttpServletRequest request) {
        // 记录日志
        ExceptionLogUtil.logParamError(e.getFieldName(), e.getFieldValue(), e.getMessage());

        // 返回错误信息
        return Result.error(e.getCode(), e.getFullMessage());
    }

    /**
     * 基础异常（所有自定义异常的父类）
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException e, HttpServletRequest request) {
        // 记录日志
        ExceptionLogUtil.logException(e, "基础异常");

        // 返回错误信息
        return Result.error(e.getCode(), e.getMessage());
    }

    // ========== Sa-Token 异常 ==========

    /**
     * Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        // 记录日志
        ExceptionLogUtil.logException(e, "用户未登录");

        // 根据异常场景返回不同消息
        String message = switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未提供Token";
            case NotLoginException.INVALID_TOKEN -> "Token无效";
            case NotLoginException.TOKEN_TIMEOUT -> "Token已过期";
            case NotLoginException.BE_REPLACED -> "Token已被替换";
            case NotLoginException.KICK_OUT -> "Token已被踢出";
            default -> "未登录，请先登录";
        };

        return Result.error(401, message);
    }

    /**
     * Sa-Token 权限不足异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        ExceptionLogUtil.logException(e, "权限不足");
        return Result.error(403, "权限不足: " + e.getPermission());
    }

    /**
     * Sa-Token 角色不足异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        ExceptionLogUtil.logException(e, "角色不足");
        return Result.error(403, "角色不足: " + e.getRole());
    }

    // ========== 参数校验异常 ==========

    /**
     * @Valid 注解校验异常（RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        // 获取第一个错误
        FieldError fieldError = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        String message = "参数校验失败";
        if (fieldError != null) {
            message = String.format("参数 [%s] %s", fieldError.getField(), fieldError.getDefaultMessage());
            ExceptionLogUtil.logParamError(fieldError.getField(), null, fieldError.getDefaultMessage());
        } else {
            ExceptionLogUtil.logException(e, "参数校验失败");
        }

        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * @Validated 注解校验异常（RequestParam, PathVariable）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        ExceptionLogUtil.logException(e, "参数校验失败: " + message);

        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        String message = "参数绑定失败";
        if (fieldError != null) {
            message = String.format("参数 [%s] %s", fieldError.getField(), fieldError.getDefaultMessage());
            ExceptionLogUtil.logParamError(fieldError.getField(), null, fieldError.getDefaultMessage());
        } else {
            ExceptionLogUtil.logException(e, "参数绑定失败");
        }

        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = String.format("缺少必要参数 [%s]", e.getParameterName());
        ExceptionLogUtil.logException(e, message);

        return Result.error(ErrorCode.PARAM_MISSING.getCode(), message);
    }

    /**
     * 参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        Class<?> requiredType = e.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "unknown";

        String message = String.format("参数 [%s] 类型错误，期望类型: %s", e.getName(), typeName);

        ExceptionLogUtil.logException(e, message);

        return Result.error(ErrorCode.PARAM_TYPE_ERROR.getCode(), message);
    }

    /**
     * 请求体不可读异常（JSON格式错误）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        ExceptionLogUtil.logException(e, "请求体格式错误");

        return Result.error(ErrorCode.PARAM_FORMAT_ERROR.getCode(), "请求体格式错误，请检查JSON格式");
    }

    // ========== HTTP 异常 ==========

    /**
     * 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        ExceptionLogUtil.logException(e, "接口不存在: " + e.getRequestURL());

        return Result.error(ErrorCode.NOT_FOUND.getCode(), "接口不存在: " + e.getRequestURL());
    }

    // ========== 系统异常 ==========

    /**
     * 系统异常（兜底处理）
     * 所有未被具体处理的异常都会到这里
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        // 记录完整的异常堆栈
        ExceptionLogUtil.logSystemException(e);

        // 判断是否为开发环境
        boolean isDev = isDevEnvironment();

        // 开发环境返回详细错误信息，生产环境隐藏敏感信息
        String message = isDev
                ? e.getClass().getSimpleName() + ": " + e.getMessage()
                : "系统内部错误，请联系管理员";

        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), message);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 判断是否为开发环境
     */
    private boolean isDevEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}

