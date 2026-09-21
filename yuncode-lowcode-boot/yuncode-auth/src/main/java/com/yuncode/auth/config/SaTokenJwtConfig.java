package com.yuncode.auth.config;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import com.yuncode.common.utils.JwtKeyUtil;
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
 * 密钥推导逻辑统一由 {@link JwtKeyUtil} 管理，与 Gateway 保持一致。
 */
@Slf4j
@Configuration
public class SaTokenJwtConfig {

    @Value("${sa-token.jwt-secret-key}")
    private String baseSecretKey;

    @PostConstruct
    public void checkDefaultKey() {
        JwtKeyUtil.validateKey(baseSecretKey, "SaTokenJwtConfig");
        log.info("JWT 密钥检查通过");
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
        stpLogic.setConfig(createConfig(JwtKeyUtil.deriveSecretKey(baseSecretKey, "user")));
        return stpLogic;
    }

    /**
     * 租户管理员 StpLogic — 使用不同 JWT 密钥
     */
    @Bean
    public StpLogic tenantStpLogic() {
        StpLogicJwtForSimple stpLogic = new StpLogicJwtForSimple("tenant");
        stpLogic.setConfig(createConfig(JwtKeyUtil.deriveSecretKey(baseSecretKey, "tenant")));
        return stpLogic;
    }
}
