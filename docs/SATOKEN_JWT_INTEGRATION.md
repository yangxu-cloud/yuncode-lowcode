# Sa-Token + JWT 集成文档

## 📚 简介

本项目采用 **Sa-Token + JWT** 的验证模式，结合了 Sa-Token 的简洁易用和 JWT 的无状态特性，实现高性能、高安全性的认证授权方案。

## 🎯 为什么选择 Sa-Token + JWT？

### 优势对比

| 特性 | 纯 Session 模式 | 纯 JWT 模式 | Sa-Token + JWT ⭐ |
|------|---------------|------------|------------------|
| **性能** | 中等（依赖 Redis） | 高（无状态） | 高（JWT + Redis 缓存） |
| **可扩展性** | 低（有状态） | 高（无状态） | 高 |
| **会话管理** | 强（完全控制） | 弱（无法撤销） | 强（JWT + Redis） |
| **开发效率** | 中等 | 低（需手写大量代码） | 高（Sa-Token 封装） |
| **分布式支持** | 需要Redis | 天然支持 | 天然支持 |
| **安全性** | 中等 | 中等 | 高（多层防护） |

### 核心优势

1. **高性能**
   - JWT 无状态，减少 Redis 读取
   - 客户端可缓存 Token，减少网络请求

2. **高安全性**
   - JWT 签名验证，防篡改
   - Redis 存储，支持主动撤销（踢人下线）
   - 多层防护（JWT + Redis 双重验证）

3. **易于扩展**
   - 无状态特性，适合微服务架构
   - 跨域支持良好
   - 移动端友好

4. **开发效率高**
   - Sa-Token 提供 API 简洁易用
   - 一行代码实现复杂功能
   - 完整的权限管理

## ✅ 已完成的配置

### 1. Maven 依赖

#### 父 POM (pom.xml)
```xml
<properties>
    <sa-token.version>1.38.0</sa-token.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Sa-Token 权限认证 -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
            <version>${sa-token.version}</version>
        </dependency>

        <!-- Sa-Token 整合 Redis -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-redis-jackson</artifactId>
            <version>${sa-token.version}</version>
        </dependency>

        <!-- Sa-Token 整合 JWT -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-jwt</artifactId>
            <version>${sa-token.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 认证模块 POM (yuncode-auth/pom.xml)
```xml
<dependencies>
    <!-- Sa-Token 权限认证 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
    </dependency>

    <!-- Sa-Token 整合 Redis -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
    </dependency>

    <!-- Sa-Token 整合 JWT (无状态模式) -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-jwt</artifactId>
    </dependency>

    <!-- Hutool 工具类 (包含密码加密功能) -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>
</dependencies>
```

### 2. 配置文件

**application.yml**
```yaml
# Sa-Token 配置
sa-token:
  # Token 名称
  token-name: satoken
  # Token 有效期（单位：秒）默认 30 天
  timeout: 2592000
  # 是否允许同一账号并发登录
  is-concurrent: true
  # 是否共用一个 token
  is-share: false
  # token 风格（使用 JWT 时必须为 simple-uuid）
  token-style: simple-uuid
  # 是否从 header 读取 token
  is-read-header: true
  # token 前缀
  token-prefix: Bearer

# Sa-Token JWT 配置
sa-token-jwt:
  # 是否开启 JWT 模式
  enable: true
  # JWT 密钥（必须大于 32 位）
  secret-key: yuncode-lowcode-sa-token-jwt-secret-key-2024
  # JWT 有效期
  timeout: 2592000
```

**重要配置说明：**
- `token-style: simple-uuid` - 使用 JWT 时必须是 simple-uuid
- `secret-key` - JWT 签名密钥，生产环境必须修改为更安全的密钥
- `timeout` - Token 有效期，与 Sa-Token 的 timeout 保持一致

### 3. Java 配置类

**SaTokenJwtConfig.java**
```java
@Configuration
public class SaTokenJwtConfig {

    /**
     * 配置 Sa-Token 使用 JWT 模式
     *
     * Sa-Token 提供三种 JWT 实现：
     * 1. StpLogicJwtForSimple - 简单模式（推荐）⭐
     * 2. StpLogicJwtForStateless - 完全无状态模式
     * 3. StpLogicJwtForMixed - 混合模式
     */
    @Bean
    @Primary
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}
```

**三种 JWT 模式对比：**

| 模式 | 特点 | 适用场景 |
|------|------|---------|
| **StpLogicJwtForSimple** ⭐ | JWT + Redis，最佳平衡 | 推荐使用，功能完整 |
| **StpLogicJwtForStateless** | 完全无状态，纯 JWT | 高并发场景，无需会话管理 |
| **StpLogicJwtForMixed** | 混合模式，复杂场景 | 大型分布式系统 |

## 🔐 工作原理

### Sa-Token + JWT 认证流程

```
┌─────────────┐
│  用户登录    │
└──────┬──────┘
       │
       ↓
┌─────────────────────────────────┐
│  1. 验证用户名和密码             │
│  2. 生成 JWT Token              │
│  3. Token 存储到 Redis（可选）   │
│  4. 返回 Token 给客户端         │
└──────┬──────────────────────────┘
       │
       ↓
┌─────────────┐
│   客户端     │
│ (存储 Token) │
└──────┬──────┘
       │
       │ 每次请求携带 Token
       ↓
┌─────────────────────────────────┐
│  Sa-Token 拦截器                │
│  1. 从 Header 中提取 Token      │
│  2. 验证 JWT 签名               │
│  3. 检查 Token 是否在 Redis 中  │
│  4. 验证通过，放行请求          │
└──────┬──────────────────────────┘
       │
       ↓
┌─────────────┐
│  业务逻辑    │
└─────────────┘
```

### Token 结构

JWT Token 由三部分组成（用 `.` 分隔）：

```
Header.Payload.Signature
```

**示例：**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4ifQ.signature
```

- **Header**: 算法和 Token 类型
- **Payload**: 用户数据（userId, username, tenantId 等）
- **Signature**: 签名（防篡改）

## 💻 使用示例

### 1. 用户登录

```java
@Service
public class AuthService {

    public LoginVO login(LoginDTO loginDTO) {
        // 1. 验证用户名和密码
        SysUser user = validateUser(loginDTO);

        // 2. 使用 Sa-Token 登录（自动生成 JWT）
        StpUtil.login(user.getId());

        // 3. 设置会话数据
        StpUtil.getSession().set("tenantId", user.getTenantId());
        StpUtil.getSession().set("username", user.getUsername());

        // 4. 返回 JWT Token
        String token = StpUtil.getTokenValue();

        return loginVO;
    }
}
```

### 2. 权限验证

```java
// 方式1：使用注解
@SaCheckPermission("user:add")
public void addUser() {
    // 业务逻辑
}

// 方式2：使用代码校验
public void deleteUser() {
    StpUtil.checkPermission("user:delete");
    // 业务逻辑
}

// 方式3：判断是否有权限
if (StpUtil.hasPermission("user:edit")) {
    // 有权限
}
```

### 3. 获取当前用户信息

```java
// 获取用户 ID
Long userId = StpUtil.getLoginIdAsLong();

// 获取会话数据
Long tenantId = (Long) StpUtil.getSession().get("tenantId");
String username = (String) StpUtil.getSession().get("username");

// 获取 Token
String token = StpUtil.getTokenValue();
```

### 4. 登出

```java
// 登出（JWT Token 会被加入黑名单，如果使用 Redis）
StpUtil.logout();

// 踢人下线
StpUtil.kickout(userId);
```

## 🔧 核心组件

### 1. SaTokenConfig.java
配置拦截器和白名单

### 2. SaTokenJwtConfig.java
配置 JWT 模式

### 3. StpInterfaceImpl.java
实现权限和角色查询接口

### 4. AuthService.java
认证服务（登录、登出）

### 5. AuthController.java
认证接口

### 6. GlobalExceptionHandler.java
全局异常处理

## 📡 API 接口

### 登录接口
```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "tenantCode": "default",
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenName": "satoken",
    "userId": 1,
    "username": "admin",
    "tenantId": 1
  }
}
```

### 后续请求
```
GET /api/user/info
Header: satoken: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## ⚠️ 注意事项

### 1. Token 风格
使用 JWT 时，`token-style` 必须设置为 `simple-uuid`，否则无法生成 JWT。

### 2. 密钥安全
- 生产环境必须修改 `secret-key` 为更复杂的密钥
- 密钥长度必须大于 32 位
- 密钥泄露会导致严重安全问题

### 3. Token 有效期
- Sa-Token 的 `timeout` 和 JWT 的 `timeout` 保持一致
- 过期时间根据业务需求设置

### 4. Redis 依赖
虽然 JWT 是无状态的，但为了实现踢人下线、账号封禁等功能，仍然建议配置 Redis。

### 5. 跨域问题
前端跨域请求时，需要正确配置 CORS：
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsConfigurationRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

## 🎯 最佳实践

### 1. Token 刷新
```java
// 前端可以在 Token 快过期时请求刷新
@GetMapping("/auth/refresh")
public Result<String> refreshToken() {
    StpUtil.renewTimeout(2592000);
    return Result.success(StpUtil.getTokenValue());
}
```

### 2. 记住我功能
```java
public LoginVO login(LoginDTO loginDTO) {
    // 如果选择"记住我"
    if (loginDTO.getRememberMe()) {
        StpUtil.login(user.getId(), 2592000); // 30 天
    } else {
        StpUtil.login(user.getId()); // 默认时间
    }
}
```

### 3. 多设备登录控制
```yaml
sa-token:
  is-concurrent: false  # 不允许并发登录，新登录挤掉旧登录
```

## 🚀 性能优化

### 1. JWT 优化
- JWT Payload 中不要存储过多数据
- 敏感数据不要存储在 JWT 中

### 2. Redis 缓存
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

### 3. Token 校验缓存
Sa-Token 会自动缓存已解析的 Token，减少重复解析开销。

## 📚 参考资料

- [Sa-Token 官方文档](https://sa-token.cc/doc.html#/use/jwt)
- [Sa-Token JWT 教程](https://sa-token.cc/doc.html#/jwt/jwt-integrate)
- [JWT 在线解析工具](https://jwt.io/)

---

**总结**：Sa-Token + JWT 模式结合了两者的优势，既保证了高性能和可扩展性，又提供了完整的会话管理和权限控制功能，是微服务架构和分布式系统的最佳选择。
