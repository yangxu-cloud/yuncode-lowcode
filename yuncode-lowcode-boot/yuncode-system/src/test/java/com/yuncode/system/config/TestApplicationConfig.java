package com.yuncode.system.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 测试配置类
 * 用于单元测试，提供最小化的 Spring 上下文
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(
    basePackages = {
        "com.yuncode.system",
        "com.yuncode.common"
    },
    useDefaultFilters = false,
    includeFilters = {
        // 只包含我们需要的配置类
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.yuncode\\.system\\.config\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.yuncode\\.common\\.config\\..*"),
        // 只包含 UserCacheService (包括接口和实现类)
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.yuncode\\.system\\.service\\..*UserCache.*")
    }
)
public class TestApplicationConfig {
}
