package com.yuncode.system.annotation;

import java.lang.annotation.*;

/**
 * 登录日志注解
 * 用于标记需要记录登录日志的方法
 *
 * @author yuncode
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoginLog {

    /**
     * 登录类型（admin, user, tenant）
     */
    String loginType() default "";
}
