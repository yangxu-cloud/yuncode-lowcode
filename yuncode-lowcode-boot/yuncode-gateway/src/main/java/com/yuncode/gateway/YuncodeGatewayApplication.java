package com.yuncode.gateway;

import com.yuncode.common.event.SimpleEventBus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Yuncode Gateway Application
 * 云码低代码平台 - 网关服务
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.yuncode.gateway")
@Import(SimpleEventBus.class)
public class YuncodeGatewayApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(YuncodeGatewayApplication.class, args).getEnvironment();
        boolean nacosEnabled = "true".equals(env.getProperty("nacos.discovery.enabled"));

        System.out.println("""

                ================================================
                    Yuncode LowCode - Gateway Service
                    云码低代码平台 - 网关服务
                ================================================
                   [Port] 9000
                ================================================
                   [Gateway Status]  http://localhost:9000/gateway/status
                   [Gateway Routes]  http://localhost:9000/gateway/routes
                   [Proxy To]        http://localhost:8080
                """);

        if (nacosEnabled) {
            System.out.println("   [Mode]          Nacos Service Discovery (ON)");
        } else {
            System.out.println("   [Mode]          Fixed Routing (Nacos OFF)");
        }

        System.out.println("""
                   [Nacos Config]  可在配置文件中启用
                ================================================
                """);
    }
}
