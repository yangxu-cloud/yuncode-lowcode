package com.yuncode.common.annotation;

import java.lang.annotation.*;

/**
 * 租户访问控制注解
 *
 * 标记在 Service 层方法上，用于控制租户级别的数据隔离。
 * STRICT：强制校验 tenantId 匹配（默认）
 * PERMISSIVE：记录警告但不拒绝（用于过渡期）
 * BYPASS：跳过检查（用于跨租户查询）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireTenant {

    Isolation isolation() default Isolation.STRICT;

    enum Isolation {
        STRICT,
        PERMISSIVE,
        BYPASS
    }
}
