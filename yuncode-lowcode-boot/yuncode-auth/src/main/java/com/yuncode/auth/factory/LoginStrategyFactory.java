package com.yuncode.auth.factory;

import com.yuncode.auth.strategy.LoginStrategy;
import com.yuncode.common.exception.BusinessException;
import com.yuncode.system.enums.LoginType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 登录策略工厂
 * 根据登录类型返回对应的登录策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginStrategyFactory {

    private final Map<String, LoginStrategy> strategyMap;

    /**
     * 根据登录类型获取对应的策略
     *
     * @param loginType 登录类型编码
     * @return 登录策略实例
     */
    public LoginStrategy getStrategy(String loginType) {
        LoginType type = LoginType.fromCode(loginType);
        String strategyBeanName = getStrategyBeanName(type);

        LoginStrategy strategy = strategyMap.get(strategyBeanName);
        if (strategy == null) {
            log.error("未找到登录策略: loginType={}, strategyBeanName={}", loginType, strategyBeanName);
            throw new BusinessException("登录类型不支持");
        }

        log.debug("获取登录策略: loginType={}, strategy={}", loginType, strategy.getClass().getSimpleName());
        return strategy;
    }

    /**
     * 根据登录类型枚举获取策略 Bean 名称
     *
     * @param loginType 登录类型枚举
     * @return 策略 Bean 名称
     */
    private String getStrategyBeanName(LoginType loginType) {
        return switch (loginType) {
            case TENANT -> "tenantLoginStrategy";
            case ADMIN -> "adminLoginStrategy";
            case USER -> "userLoginStrategy";
        };
    }
}
