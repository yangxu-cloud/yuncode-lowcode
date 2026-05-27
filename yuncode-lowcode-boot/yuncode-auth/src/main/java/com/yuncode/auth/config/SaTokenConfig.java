package com.yuncode.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Sa-Token 配置类
 * 使用 Sa-Token + JWT 模式
 *
 * 拦截器检查所有登录类型的 StpLogic，任一有效即可通过。
 * 三种登录类型（admin/user/tenant）使用独立的 JWT 签名密钥隔离。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Autowired
    private List<StpLogic> stpLogics;

    /**
     * 注册 Sa-Token 拦截器，校验所有登录类型的 token
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 三种登录类型任一 token 有效即可通过
            boolean anyValid = stpLogics.stream().anyMatch(stp -> {
                try {
                    return stp.isLogin();
                } catch (Exception e) {
                    return false;
                }
            });
            if (!anyValid) {
                StpUtil.checkLogin(); // 抛出标准 NotLoginException
            }
        }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/settings/basic",
                        "/favicon.ico",
                        "/error",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/knife4j/**",
                        "/actuator/**"
                );
    }
}
