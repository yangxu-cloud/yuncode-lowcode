package com.yuncode.auth.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token JWT 配置类
 *
 * 配置 Sa-Token 使用 JWT 简单模式
 */
@Configuration
public class SaTokenJwtConfig {

    /**
     * 配置 Sa-Token 使用 JWT 简单模式
     * 使用 @Primary 作为默认的 StpLogic 实例
     */
    @Bean
    @Primary
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}
