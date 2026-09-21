package com.yuncode.common.sdk.impl;

import com.yuncode.common.sdk.SDK;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SDK 初始化器 — 在 Spring 启动时注册所有 SDK 实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SDKInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        DefaultUserSDK userSDK = new DefaultUserSDK();
        DefaultTenantSDK tenantSDK = new DefaultTenantSDK();
        DefaultAppSDK appSDK = new DefaultAppSDK();
        DefaultDatabaseSDK dbSDK = new DefaultDatabaseSDK(jdbcTemplate, transactionTemplate);

        SDK.init(appSDK, userSDK, tenantSDK, dbSDK);
        log.info("Yuncode SDK 已初始化完成");
    }
}
