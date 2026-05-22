# 用户缓存功能实现说明

## 概述

为了优化 `getCurrentUserInfo` 接口的性能，避免每次调用都查询数据库，我们实现了基于 Redis 的用户信息缓存功能。

## 实现内容

### 1. 用户缓存服务

**接口：** `UserCacheService`
**实现：** `UserCacheServiceImpl`
**位置：** `yuncode-system/src/main/java/com/yuncode/system/service/`

#### 功能
- `cacheUser(userId, user, timeout)` - 缓存用户信息
- `getCachedUser(userId)` - 获取缓存的用户信息
- `evictUserCache(userId)` - 删除用户缓存
- `updateUserCache(userId, user)` - 更新用户缓存

#### 存储方式
- **Redis Key 格式：** `user:info:{userId}`
- **默认过期时间：** 30分钟（1800秒）
- **序列化方式：** Jackson2JsonRedisSerializer

### 2. 缓存集成点

#### 登录时缓存用户信息
```java
// AuthService.login() 方法中
// 8. 缓存用户信息到 Redis（30分钟）
userCacheService.cacheUser(user.getId(), user, 1800);
```

#### 获取用户信息时使用缓存
```java
// AuthService.getCurrentUserInfo() 方法中
// 先从缓存获取用户信息
SysUser user = userCacheService.getCachedUser(userId);

// 如果缓存不存在，从数据库查询并缓存
if (user == null) {
    user = sysUserMapper.selectById(userId);
    if (user == null) {
        throw new BusinessException("用户不存在");
    }
    // 缓存用户信息，默认30分钟
    userCacheService.cacheUser(userId, user, 1800);
}
```

### 3. 用户服务扩展

**类：** `UserService`
**位置：** `yuncode-system/src/main/java/com/yuncode/system/service/UserService.java`

#### 功能
- `updateUserWithCacheEvict(user)` - 更新用户信息并清除缓存
- `getUserByIdWithCache(userId)` - 获取用户信息（带缓存）

## 缓存策略

### Cache-Aside 模式（旁路缓存）
1. **读取操作：**
   - 先从缓存读取
   - 如果缓存不存在，从数据库读取
   - 将数据写入缓存
   - 返回数据

2. **更新操作：**
   - 先更新数据库
   - 然后删除缓存
   - 下次读取时重新加载最新数据

3. **删除操作：**
   - 删除数据库记录
   - 同时删除缓存

### 缓存失效
- **时间失效：** 30分钟后自动过期
- **主动失效：** 更新用户信息时主动删除缓存

## 性能优化

### 优化前
每次调用 `getCurrentUserInfo` 都需要：
1. 查询用户表（1次 SQL）
2. 查询租户表（1次 SQL）
3. 总共 2 次数据库查询

### 优化后
命中缓存后只需要：
1. 从 Redis 读取用户信息（1次 Redis GET）
2. 查询租户表（1次 SQL）
3. 总共 1 次数据库查询（减少 50%）

### 预期性能提升
- **Redis 响应时间：** ~1ms
- **MySQL 查询时间：** ~10-50ms
- **性能提升：** 在高并发场景下可显著降低数据库负载

## 使用示例

### 登录时自动缓存
```java
// 登录成功后，用户信息会自动缓存到 Redis
userCacheService.cacheUser(user.getId(), user, 1800);
```

### 获取用户信息（自动使用缓存）
```java
// 前端调用 /api/auth/info
// 后端自动从缓存获取用户信息，如果缓存不存在才查询数据库
```

### 更新用户信息时清除缓存
```java
SysUser user = new SysUser();
user.setId(1L);
user.setNickname("新昵称");

// 更新并清除缓存
userService.updateUserWithCacheEvict(user);
```

### 手动清除缓存
```java
// 如果需要强制刷新用户信息
userCacheService.evictUserCache(userId);
```

## 注意事项

1. **缓存一致性**
   - 用户信息更新后必须清除缓存
   - 可以通过 `updateUserWithCacheEvict` 方法自动处理

2. **缓存穿透**
   - 如果用户不存在，不会缓存 null 值
   - 每次都会查询数据库

3. **缓存雪崩**
   - 使用了固定的过期时间（30分钟）
   - 如果需要，可以添加随机偏移量

4. **分布式环境**
   - Redis 在分布式环境下共享
   - 多个应用实例可以共享缓存

## 监控建议

### Redis 监控指标
- 缓存命中率
- 缓存过期频率
- Redis 内存使用情况

### 监控命令
```bash
# 查看所有用户缓存
redis-cli keys "user:info:*"

# 查看特定用户的缓存
redis-cli get "user:info:1"

# 查看缓存数量
redis-cli dbsize
```

## 扩展建议

### 1. 租户信息缓存
可以进一步优化，将租户信息也缓存：
```java
tenantCacheService.cacheTenant(tenantId, tenant, 3600); // 1小时
```

### 2. 本地缓存 + Redis 缓存
使用 Caffeine 作为 L1 缓存，Redis 作为 L2 缓存：
```java
@Cacheable(value = "users", key = "#userId")
public SysUser getUserById(Long userId) {
    return sysUserMapper.selectById(userId);
}
```

### 3. 缓存预热
系统启动时预热常用用户信息：
```java
@PostConstruct
public void warmUpCache() {
    // 预加载管理员用户
    List<SysUser> admins = sysUserMapper.selectAdminUsers();
    admins.forEach(user ->
        userCacheService.cacheUser(user.getId(), user, 3600)
    );
}
```

## 相关文件

1. **UserCacheService.java** - 用户缓存服务接口
2. **UserCacheServiceImpl.java** - 用户缓存服务实现
3. **UserService.java** - 用户服务扩展
4. **AuthService.java** - 认证服务（集成缓存）
5. **RedisConfig.java** - Redis 配置（支持 LocalDateTime 序列化）

## 总结

通过实现用户缓存功能，我们：
- ✅ 减少了数据库查询次数
- ✅ 提高了接口响应速度
- ✅ 降低了数据库负载
- ✅ 提升了系统并发能力
- ✅ 保持了数据一致性（通过缓存失效机制）

这是一个典型的用空间换时间的优化策略，在用户信息读多写少的场景下效果显著。
