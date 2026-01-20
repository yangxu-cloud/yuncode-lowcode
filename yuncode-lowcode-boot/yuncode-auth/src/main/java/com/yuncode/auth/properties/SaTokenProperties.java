package com.yuncode.auth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "sa-token")
public class SaTokenProperties {

    /**
     * Token 名称（同时也是 Cookie 名称）
     * 默认值：token（与 application.yml 中的 sa-token.token-name 配置一致）
     */
    private String tokenName = "token";

    /**
     * Token 有效期（单位：秒）默认 30 天，-1 代表永不过期
     */
    private Long timeout = 2592000L;

    /**
     * Token 临时有效期（指定时间内无操作就视为 token 过期）单位：秒
     */
    private Long activityTimeout = -1L;

    /**
     * 是否允许同一账号并发登录（为 true 时允许一起登录，为 false 时新登录挤掉旧登录）
     */
    private Boolean isConcurrent = true;

    /**
     * 在多人登录同一账号时，是否共用一个 token（为 true 时所有登录共用一个 token，为 false 时每次登录新建一个 token）
     */
    private Boolean isShare = false;

    /**
     * token 风格（默认可取值：uuid、simple-uuid、random-32、random-64、random-128、tik）
     */
    private String tokenStyle = "uuid";

    /**
     * 是否输出操作日志
     */
    private Boolean isLog = false;

    /**
     * 是否从 cookie 读取 token
     */
    private Boolean isReadCookie = false;

    /**
     * 是否从 header 读取 token
     */
    private Boolean isReadHeader = true;

    /**
     * token 前缀
     */
    private String tokenPrefix = "Bearer ";
}
