package com.yuncode.system.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 业务类型（0其它 1新增 2修改 3删除 4授权 5导出 6导入）
     */
    int businessType() default 0;

    /**
     * 方法描述
     */
    String description() default "";
}
