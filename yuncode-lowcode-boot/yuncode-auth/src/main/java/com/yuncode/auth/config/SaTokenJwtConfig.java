package com.yuncode.auth.config;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token JWT 配置类
 *
 * 为每种登录类型创建独立的 StpLogic 实例，使用不同的 JWT 签名密钥。
 * 三种类型（admin/user/tenant）的 token 无法互相解析，实现安全隔离。
 */
@Slf4j
@Configuration
public class SaTokenJwtConfig {

    private static final String DEFAULT_JWT_SECRET_KEY = "yuncode-lowcode-sa-token-jwt-secret-key-2024-hutool-bcrypt-compatible-with-spring-boot-3-x8k9m2v4n6";

    @Value("${sa-token.jwt-secret-key}")
    private String baseSecretKey;

    @PostConstruct
    public void checkDefaultKey() {
        if (DEFAULT_JWT_SECRET_KEY.equals(baseSecretKey)) {
            log.warn("══════════════════════════════════════════════════════════════════");
            log.warn("  安全警告：JWT 签名密钥使用了默认值！");
            log.warn("  生产环境请通过环境变量 JWT_SECRET_KEY 设置自定义密钥！");
            log.warn("  默认密钥在任何公开仓库中均可查看到，存在严重安全隐患。");
            log.warn("══════════════════════════════════════════════════════════════════");
        }
    }

    private static SaTokenConfig createConfig(String secretKey) {
        SaTokenConfig config = new SaTokenConfig();
        config.setJwtSecretKey(secretKey);
        config.setTokenStyle("simple-uuid");
        config.setIsConcurrent(true);
        config.setIsShare(true);
        return config;
    }

    /**
     * 管理员 StpLogic — @Primary，StpUtil 默认指向此实例
     */
    @Bean
    @Primary
    public StpLogic adminStpLogic() {
        StpLogicJwtForSimple stpLogic = new StpLogicJwtForSimple("admin");
        stpLogic.setConfig(createConfig(baseSecretKey));
        return stpLogic;
    }

    /**
     * 普通用户 StpLogic — 使用不同 JWT 密钥
     */
    @Bean
    public StpLogic userStpLogic() {
        StpLogicJwtForSimple stpLogic = new StpLogicJwtForSimple("user");
        stpLogic.setConfig(createConfig(baseSecretKey + "_user"));
        return stpLogic;
    }

    /**
     * 租户管理员 StpLogic — 使用不同 JWT 密钥
     */
    @Bean
    public StpLogic tenantStpLogic() {
        StpLogicJwtForSimple stpLogic = new StpLogicJwtForSimple("tenant");
        stpLogic.setConfig(createConfig(baseSecretKey + "_tenant"));
        return stpLogic;
    }
}
