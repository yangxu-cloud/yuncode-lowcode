package com.yuncode.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 * 使用 Sa-Token + JWT 模式
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器，进行登录校验
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验登录状态
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 排除认证接口
                        "/auth/**",
                        // 排除设置接口（允许匿名访问基础设置）
                        "/settings/basic",
                        // 排除工具接口（仅开发环境）
                        "/util/**",
                        // 排除静态资源
                        "/favicon.ico",
                        "/error",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/knife4j/**",
                        // 排除健康检查
                        "/actuator/**"
                );
    }

    /**
     * 权限认证规则配置（预留）
     * 可以在这里添加更复杂的权限校验逻辑
     */
    public void checkPermission() {
        // 根据路由路径进行权限校验
        // 例如：根据请求路径检查具体权限
        // StpUtil.checkPermission("user:add");
    }
}
