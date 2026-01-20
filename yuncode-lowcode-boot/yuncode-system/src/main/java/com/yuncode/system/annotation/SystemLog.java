package com.yuncode.system.annotation;

import java.lang.annotation.*;

/**
 * 系统日志注解
 * 用于标记需要记录系统日志的方法（异常、性能监控等）
 *
 * @author yuncode
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SystemLog {

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 日志级别（TRACE, DEBUG, INFO, WARN, ERROR）
     */
    String level() default "INFO";
}
