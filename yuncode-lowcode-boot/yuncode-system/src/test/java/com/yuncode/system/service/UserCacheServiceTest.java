package com.yuncode.system.service;

import com.yuncode.system.config.TestApplicationConfig;
import com.yuncode.system.entity.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户缓存服务测试
 */
@SpringBootTest(classes = TestApplicationConfig.class)
public class UserCacheServiceTest {

    @Autowired
    private UserCacheService userCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    public void setUp() {
        // 清理测试数据
        var connectionFactory = redisTemplate.getConnectionFactory();
        if (connectionFactory != null) {
            var connection = connectionFactory.getConnection();
            connection.serverCommands().flushDb();
        }
    }

    @Test
    public void testCacheAndRetrieveUser() {
        // 创建测试用户
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setNickname("测试用户");
        user.setAvatar("avatar.png");

        // 缓存用户信息（30秒过期）
        userCacheService.cacheUser(1L, user, 30);

        // 从缓存获取
        SysUser cachedUser = userCacheService.getCachedUser(1L);

        // 验证
        assertNotNull(cachedUser, "缓存应该存在");
        assertEquals("testuser", cachedUser.getUsername());
        assertEquals("测试用户", cachedUser.getNickname());

        System.out.println("✅ 用户缓存测试通过！");
    }

    @Test
    public void testEvictUserCache() throws InterruptedException {
        // 创建测试用户
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("testuser2");

        // 缓存用户信息
        userCacheService.cacheUser(2L, user, 30);

        // 验证缓存存在
        SysUser cachedUser = userCacheService.getCachedUser(2L);
        assertNotNull(cachedUser, "缓存应该存在");

        // 删除缓存
        userCacheService.evictUserCache(2L);

        // 验证缓存已删除
        SysUser evictedUser = userCacheService.getCachedUser(2L);
        assertNull(evictedUser, "缓存应该已被删除");

        System.out.println("✅ 用户缓存删除测试通过！");
    }

    @Test
    public void testCacheExpiration() throws InterruptedException {
        // 创建测试用户
        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("testuser3");

        // 缓存用户信息（3秒过期）
        userCacheService.cacheUser(3L, user, 3);

        // 立即获取，应该存在
        SysUser cachedUser = userCacheService.getCachedUser(3L);
        assertNotNull(cachedUser, "缓存应该立即存在");

        // 等待4秒后，缓存应该过期
        Thread.sleep(4000);
        SysUser expiredUser = userCacheService.getCachedUser(3L);
        assertNull(expiredUser, "缓存应该已过期");

        System.out.println("✅ 用户缓存过期测试通过！");
    }
}
