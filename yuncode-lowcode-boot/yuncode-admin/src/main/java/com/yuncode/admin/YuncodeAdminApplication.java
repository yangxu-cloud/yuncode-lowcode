package com.yuncode.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Yuncode LowCode Admin Application
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.yuncode")
@MapperScan({
    "com.yuncode.system.mapper",
    "com.yuncode.tenant.mapper",
    "com.yuncode.auth.mapper"
})
public class YuncodeAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuncodeAdminApplication.class, args);
        System.out.println("""

                ======================================
                 Yuncode LowCode Platform Started!
                ======================================
                """);
    }
}
