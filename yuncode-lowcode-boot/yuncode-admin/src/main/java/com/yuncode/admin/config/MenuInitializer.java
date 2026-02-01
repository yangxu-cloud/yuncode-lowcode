package com.yuncode.admin.config;

import com.yuncode.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 菜单初始化器
 * 应用启动时自动初始化默认菜单
 *
 * 注意：已禁用自动初始化，请通过API手动调用 /menu/init
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Slf4j
//@Component  // 已禁用自动初始化
@Order(1)
@RequiredArgsConstructor
public class MenuInitializer implements ApplicationRunner {

    private final MenuService menuService;

    @Override
    public void run(ApplicationArguments args) {
        // 启动时不自动初始化，避免非Web上下文问题
        // 用户可以通过API /menu/init 手动初始化
        log.info("菜单自动初始化已禁用，请通过API手动调用初始化");
    }
}
