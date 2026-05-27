package com.yuncode.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Yuncode LowCode Admin Application
 * 云码低代码平台 - 后端服务
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.yuncode",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.yuncode\\.user\\.apps\\..*"
        ))
@MapperScan({
    "com.yuncode.system.mapper",
    "com.yuncode.auth.mapper"
})
public class YuncodeAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuncodeAdminApplication.class, args);
        System.out.println("""

                ================================================
                    Yuncode LowCode - Backend Service
                    云码低代码平台 - 后端服务
                ================================================
                   [Port] 8080
                   [Path] /api
                ================================================
                   [API Address] http://localhost:8080/api
                   [API Docs]    http://localhost:8080/api/doc.html
                   [Event API]   http://localhost:8080/api/event/types
                ================================================
                """);
    }
}
