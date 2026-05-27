package com.yuncode.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 配置类
 * 根据配置 nacos.discovery.enabled 决定是否启用
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "nacos.discovery.enabled", havingValue = "true")
@EnableDiscoveryClient
public class NacosConfig {

    public NacosConfig() {
        log.info("==============================================");
        log.info("  Nacos Service Discovery Enabled!");
        log.info("==============================================");
    }
}
