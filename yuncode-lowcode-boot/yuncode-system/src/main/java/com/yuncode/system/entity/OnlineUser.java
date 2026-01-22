package com.yuncode.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 在线用户实体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段，兼容Redis中的旧数据
public class OnlineUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID（业务会话标识，UUID格式）
     * 用于 Redis key 和前端 Cookie key
     * 与 Sa-Token 的 token 是两个不同的概念
     */
    private String sessionId;

    /**
     * Sa-Token 的 JWT Token
     * 用于 Sa-Token 认证，通过 StpUtil.kickoutByTokenValue() 踢出
     */
    private String token;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 位置
     */
    private String location;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 状态：active-活跃，idle-闲置
     */
    private String status;

    /**
     * 是否活跃（兼容旧数据，已废弃）
     * @deprecated 使用 status 字段代替
     */
    @Deprecated
    private Boolean active;

    /**
     * 是否在线
     */
    public boolean isActive() {
        // 兼容旧数据：如果active字段存在，使用它；否则使用status字段
        if (active != null) {
            return active;
        }
        return "active".equals(status);
    }

    /**
     * 更新最后访问时间
     */
    public void updateLastAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
        this.status = "active";
    }
}
