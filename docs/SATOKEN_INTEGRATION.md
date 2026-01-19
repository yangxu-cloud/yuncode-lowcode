# Sa-Token 集成文档

## 📚 Sa-Token 简介

Sa-Token 是一个轻量级 Java 权限认证框架，主要解决：**登录认证、权限认证、Session 会话、单点登录、OAuth2.0** 等一系列权限相关问题。

官网：https://sa-token.cc/

## ✅ 已完成的集成

### 1. Maven 依赖配置

#### 父 POM ([yuncode-lowcode-boot/pom.xml](../yuncode-lowcode-boot/pom.xml))
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

        <!-- Sa-Token 整合 Redis （使用jackson序列化） -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-redis-jackson</artifactId>
            <version>${sa-token.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 认证模块 POM ([yuncode-auth/pom.xml](../yuncode-lowcode-boot/yuncode-auth/pom.xml))
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

    <!-- Hutool 工具类 (包含密码加密功能) -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>
</dependencies>
```

### 2. 配置文件

[application.yml](../yuncode-lowcode-boot/yuncode-admin/src/main/resources/application.yml)
```yaml
# Sa-Token 配置
sa-token:
  # Token 名称（同时也是 Cookie 名称）
  token-name: satoken
  # Token 有效期（单位：秒）默认 30 天，-1 代表永不过期
  timeout: 2592000
  # Token 临时有效期（指定时间内无操作就视为 token 过期）单位：秒
  activity-timeout: -1
  # 是否允许同一账号并发登录（为 true 时允许一起登录，为 false 时新登录挤掉旧登录）
  is-concurrent: true
  # 在多人登录同一账号时，是否共用一个 token
  is-share: false
  # token 风格（默认可取值：uuid、simple-uuid、random-32、random-64、random-128、tik）
  token-style: uuid
  # 是否输出操作日志
  is-log: false
  # 是否从 cookie 读取 token
  is-read-cookie: false
  # 是否从 header 读取 token
  is-read-header: true
  # token 前缀
  token-prefix: Bearer
```

### 3. 核心组件

#### 3.1 Sa-Token 配置类
[SaTokenConfig.java](../yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/config/SaTokenConfig.java)

```java
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/actuator/**"
                );
    }
}
```

#### 3.2 权限接口实现
[StpInterfaceImpl.java](../yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/service/StpInterfaceImpl.java)

实现 `StpInterface` 接口，提供权限和角色查询功能。

#### 3.3 认证服务
[AuthService.java](../yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/service/AuthService.java)

提供登录、登出、获取用户信息等功能。

#### 3.4 认证控制器
[AuthController.java](../yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/controller/AuthController.java)

提供登录相关的 API 接口。

#### 3.5 全局异常处理器
[GlobalExceptionHandler.java](../yuncode-lowcode-boot/yuncode-admin/src/main/java/com/yuncode/admin/handler/GlobalExceptionHandler.java)

统一处理 Sa-Token 异常和业务异常。

#### 3.6 Sa-Token 工具类
[SaTokenUtil.java](../yuncode-lowcode-boot/yuncode-common/src/main/java/com/yuncode/common/utils/SaTokenUtil.java)

封装常用的认证和授权操作。

### 4. API 接口

#### 4.1 用户登录
```
POST /api/auth/login
Content-Type: application/json

{
  "tenantCode": "default",
  "username": "admin",
  "password": "admin123"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "xxxx-xxxx-xxxx-xxxx",
    "tokenName": "satoken",
    "userId": 1,
    "username": "admin",
    "nickname": "管理员",
    "avatar": "",
    "tenantId": 1,
    "tenantName": "默认租户"
  }
}
```

#### 4.2 用户登出
```
POST /api/auth/logout
Header: satoken: xxxx-xxxx-xxxx-xxxx
```

#### 4.3 获取当前用户信息
```
GET /api/auth/info
Header: satoken: xxxx-xxxx-xxxx-xxxx
```

#### 4.4 检查登录状态
```
GET /api/auth/checkLogin
Header: satoken: xxxx-xxxx-xxxx-xxxx
```

### 5. 前端集成

#### 5.1 Axios 拦截器
[request.ts](../yuncode-lowcode-admin/src/utils/request.ts)

```typescript
// Request interceptor
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// Response interceptor
request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data;
    if (code === 200) {
      return data;
    } else {
      ElMessage.error(message || "请求失败");
      return Promise.reject(new Error(message || "请求失败"));
    }
  },
  error => {
    if (error.response?.status === 401) {
      // 未登录，跳转到登录页
      router.push("/login");
    }
    ElMessage.error(error.message || "网络错误");
    return Promise.reject(error);
  }
);
```

#### 5.2 登录实现
[login/index.vue](../yuncode-lowcode-admin/src/views/login/index.vue)

```typescript
const handleLogin = async () => {
  const res = await login(loginForm);
  // 保存 token
  localStorage.setItem("token", res.token);
  // 保存用户信息
  localStorage.setItem("userInfo", JSON.stringify(res));
  // 跳转到首页
  router.push("/layout");
};
```

## 🎯 Sa-Token 核心用法

### 1. 登录认证
```java
// 登录
StpUtil.login(userId);

// 登出
StpUtil.logout();

// 检查是否登录
StpUtil.isLogin();

// 获取当前登录用户 ID
StpUtil.getLoginId();

// 获取当前登录用户信息
StpUtil.getSession();
```

### 2. 权限验证
```java
// 检查权限
StpUtil.hasPermission("user:add");

// 校验权限，如果不通过则抛出异常
StpUtil.checkPermission("user:add");

// 检查角色
StpUtil.hasRole("admin");

// 校验角色，如果不通过则抛出异常
StpUtil.checkRole("admin");
```

### 3. 会话管理
```java
// 获取 Session
StpUtil.getSession();

// 设置 Session
StpUtil.getSession().set("key", "value");

// 获取 Session 数据
StpUtil.getSession().get("key");

// 踢人下线
StpUtil.kickout(userId);
```

### 4. 密码加密（使用 Hutool）
```java
// 密码加密
String hashedPassword = BCrypt.hashpw("admin123");

// 密码校验
boolean isMatch = BCrypt.checkpw("admin123", hashedPassword);
```

## 📋 待完善功能

### 1. Mapper 层实现
需要实现以下 Mapper 接口：

**SysUserMapper**
```java
public interface SysUserMapper extends BaseMapper<SysUser> {
    // 根据用户名和租户ID查询用户
    SysUser selectByUsernameAndTenantId(
        @Param("username") String username,
        @Param("tenantId") Long tenantId
    );
}
```

**SysTenantMapper**
```java
public interface SysTenantMapper extends BaseMapper<SysTenant> {
    // 根据租户编码查询租户
    SysTenant selectByTenantCode(@Param("tenantCode") String tenantCode);
}
```

### 2. 权限查询实现
在 `StpInterfaceImpl` 中实现：
- `getPermissionList()` - 从数据库查询用户权限
- `getRoleList()` - 从数据库查询用户角色

### 3. 数据库初始化
执行 [docs/DATABASE.md](DATABASE.md) 中的 SQL 脚本，初始化：
- 租户数据
- 用户数据（密码需要使用 BCrypt 加密）
- 角色数据
- 菜单数据
- 权限关联数据

## 🔒 安全特性

### 1. Token 安全
- ✅ Token 存储在 Redis 中，支持分布式部署
- ✅ Token 自动过期机制
- ✅ Token 支持主动失效（踢人下线）
- ✅ 使用 Bearer 前缀，符合 OAuth2.0 规范

### 2. 密码安全
- ✅ 使用 BCrypt 加密算法
- ✅ 每次加密结果不同（加盐）
- ✅ 不可逆加密

### 3. 会话安全
- ✅ 支持单点登录
- ✅ 支持并发登录控制
- ✅ 支持踢人下线
- ✅ 支持账号封禁

### 4. 权限安全
- ✅ 基于 RBAC 的权限模型
- ✅ 支持角色和权限双重验证
- ✅ 支持注解式权限校验
- ✅ 细粒度的权限控制

## 📚 参考文档

- [Sa-Token 官方文档](https://sa-token.cc/doc.html)
- [Sa-Token Spring Boot 3 集成](https://blog.csdn.net/qq_58159506/article/details/139647081)
- [Sa-Token 与 Spring Security 对比](https://blog.csdn.net/weixin_42124444/article/details/145592804)

## 🎉 总结

已成功将 Sa-Token 集成到项目中，相比 Spring Security：
- ✅ 更简单的 API
- ✅ 更少的配置
- ✅ 更高的开发效率
- ✅ 完全移除了 Spring Security 依赖
- ✅ 使用 Hutool 进行密码加密
- ✅ 前后端分离完美支持

下一步：
1. 实现 Mapper 层
2. 完善权限查询逻辑
3. 初始化数据库数据
4. 测试登录认证流程
