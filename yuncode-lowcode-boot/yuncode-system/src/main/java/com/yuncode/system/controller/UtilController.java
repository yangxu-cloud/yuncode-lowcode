package com.yuncode.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 临时工具控制器 - 用于清除 Redis 缓存
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/util")
@Tag(name = "工具接口", description = "临时工具接口")
public class UtilController {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 清除所有在线用户缓存
     * 用于解决 OnlineUser 实体类字段变更后的序列化问题
     */
    @PostMapping("/clear-online-users")
    @Operation(summary = "清除在线用户缓存", description = "清除所有在线用户的Redis缓存数据")
    public String clearOnlineUsers() {
        Set<String> keys = redisTemplate.keys("online_user:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            return "已清除 " + keys.size() + " 条在线用户记录";
        }
        return "没有找到在线用户记录";
    }

    /**
     * 清除所有用户缓存
     */
    @PostMapping("/clear-user-cache")
    @Operation(summary = "清除用户缓存", description = "清除所有用户信息的Redis缓存数据")
    public String clearUserCache() {
        Set<String> keys = redisTemplate.keys("user:info:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            return "已清除 " + keys.size() + " 条用户缓存记录";
        }
        return "没有找到用户缓存记录";
    }
}
