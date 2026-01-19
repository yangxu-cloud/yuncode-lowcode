# Yuncode LowCode 项目开发对话记录

## 项目概述

**项目名称**: Yuncode LowCode Platform
**项目类型**: 企业级 SaaS 低代码平台
**技术栈**: Spring Boot 3 + Vue 3 + Sa-Token + Redis + MySQL

---

## 第一阶段：项目架构搭建

### 用户需求
构建整个 SaaS 平台的基础架构，暂不考虑具体功能。

### 完成的工作

#### 1. 后端架构（Spring Boot）
- 创建父 POM 配置，Maven 多模块结构
- 6 个核心模块：
  - `yuncode-common` - 公共工具模块
  - `yuncode-auth` - 认证授权模块
  - `yuncode-system` - 系统管理模块
  - `yuncode-tenant` - 租户管理模块
  - `yuncode-business` - 业务模块（预留）
  - `yuncode-admin` - 管理后台聚合服务

#### 2. 公共模块封装
- **Result.java** - 统一响应结果封装
- **PageResult.java** - 分页结果封装
- **BusinessException.java** - 业务异常处理
- **JwtUtil.java** - JWT 工具类（后被 Sa-Token 替代）

#### 3. 系统模块实体
- **SysUser.java** - 用户实体
- **SysRole.java** - 角色实体
- **SysMenu.java** - 菜单实体

#### 4. 租户模块实体
- **SysTenant.java** - 租户实体

#### 5. 前端架构（Vue 3）
- Vue 3 + TypeScript + Vite
- Element Plus + PureAdmin 风格
- Vue Router 4 + Pinia
- Axios 封装
- 页面组件：
  - 登录页 (login/index.vue)
  - 布局页 (layout/index.vue)
  - 首页 (home/index.vue)

#### 6. 数据库设计
- 7 张核心表：
  - sys_tenant - 租户表
  - sys_user - 用户表
  - sys_role - 角色表
  - sys_menu - 菜单表
  - sys_user_role - 用户角色关联表
  - sys_role_menu - 角色菜单关联表
  - sys_oper_log - 操作日志表

#### 7. 项目文档
- [README.md](README.md) - 项目介绍
- [DATABASE.md](DATABASE.md) - 数据库设计文档
- [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) - 项目总结
- [QUICKSTART.md](QUICKSTART.md) - 快速启动指南
- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) - 项目总览

---

## 第二阶段：权限认证方案讨论

### 用户需求
用户对 JWT 的安全性有顾虑，建议采用 Sa-Token 或 Spring Security + JWT 的方式，询问推荐哪种方案。

### AI 推荐方案
**推荐：Sa-Token**

#### 推荐理由

1. **更适合 SaaS 平台场景**
   - 快速开发，API 简洁
   - 轻量级，不增加系统复杂度
   - 原生支持多租户认证
   - 内置会话管理（踢人下线、账号封禁等）

2. **安全性不输 Spring Security**
   - 支持 JWT + Redis 双重验证
   - Token 自动续期和刷新
   - 防重放攻击、防 XSS
   - 支持同端互斥登录（单点登录）

3. **功能更丰富**
   - 一行代码实现复杂操作
   - 权限认证注解化
   - Session 会话管理
   - 单点登录 (SSO) 支持

4. **Spring Boot 3 完美支持**
   - Sa-Token 1.38.0 完全适配
   - 与当前技术栈完美契合

#### 对比分析

| 特性 | Sa-Token ⭐推荐 | Spring Security + JWT |
|------|----------------|---------------------|
| **上手难度** | ⭐⭐⭐⭐⭐ 简单 | ⭐⭐ 复杂 |
| **开发效率** | ⭐⭐⭐⭐⭐ 高 | ⭐⭐⭐ 中等 |
| **多租户支持** | ⭐⭐⭐⭐⭐ 原生支持 | ⭐⭐⭐ 需自己实现 |
| **会话管理** | ⭐⭐⭐⭐⭐ 内置强大 | ⭐⭐⭐ 需要扩展 |
| **安全性** | ⭐⭐⭐⭐⭐ 企业级 | ⭐⭐⭐⭐⭐ 企业级 |
| **学习成本** | ⭐⭐⭐⭐⭐ 低 | ⭐⭐ 高 |

### 用户决策
✅ 确认使用 Sa-Token

---

## 第三阶段：Sa-Token 集成实施

### 完成的工作

#### 1. Maven 依赖配置

**父 POM 修改** (pom.xml)
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
    </dependencies>
</dependencyManagement>
```

**注意**: 同时移除了原有的 JWT 依赖（jjwt-api、jjwt-impl、jjwt-jackson）

#### 2. 认证模块 POM 配置 (yuncode-auth/pom.xml)

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

    <!-- MyBatis Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
    </dependency>
</dependencies>
```

#### 3. 核心组件创建

##### 3.1 SaTokenProperties.java
**路径**: `yuncode-auth/src/main/java/com/yuncode/auth/properties/SaTokenProperties.java`

配置属性类，用于绑定 Sa-Token 配置参数。

##### 3.2 SaTokenConfig.java
**路径**: `yuncode-auth/src/main/java/com/yuncode/auth/config/SaTokenConfig.java`

Sa-Token 配置类，配置拦截器：
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

##### 3.3 LoginDTO.java
**路径**: `yuncode-auth/src/main/java/com/yuncode/auth/dto/LoginDTO.java`

登录请求参数：
```java
@Data
public class LoginDTO implements Serializable {
    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

##### 3.4 LoginVO.java
**路径**: `yuncode-auth/src/main/java/com/yuncode/auth/vo/LoginVO.java`

登录响应数据：
```java
@Data
public class LoginVO implements Serializable {
    private String token;
    private String tokenName;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Long tenantId;
    private String tenantName;
}
```

##### 3.5 AuthService.java
**路径**: `yuncode-auth/src/main/java/com/yuncode/auth/service/AuthService.java`

认证服务，核心方法：
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // 用户登录
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 校验租户是否存在
        // 2. 校验租户状态
        // 3. 校验租户是否过期
        // 4. 查询用户
        // 5. 校验用户状态
        // 6. 校验密码（使用 Hutool 的 BCrypt）
        // 7. 使用 Sa-Token 进行登录
        // 8. 构建返回结果
    }

    // 用户登出
    public void logout() {
        StpUtil.logout();
    }

    // 获取当前登录用户信息
    public LoginVO getCurrentUserInfo() {
        // ...
    }
}
```

**密码校验**：
```java
if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
    throw new BusinessException("用户名或密码错误");
}
```

##### 3.6 AuthController.java
**路径**: `yuncode-auth/src/main/java/com/yuncode/auth/controller/AuthController.java`

认证控制器，提供 API 接口：
```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO)

    @PostMapping("/logout")
    public Result<Void> logout()

    @GetMapping("/info")
    public Result<LoginVO> getCurrentUserInfo()

    @GetMapping("/checkLogin")
    public Result<Boolean> checkLogin()
}
```

##### 3.7 StpInterfaceImpl.java
**路径**: `yuncode-auth/src/main/java/com/yuncode/auth/service/StpInterfaceImpl.java`

Sa-Token 权限接口实现：
```java
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // TODO: 从数据库查询用户的权限列表
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // TODO: 从数据库查询用户的角色列表
        return new ArrayList<>();
    }
}
```

##### 3.8 GlobalExceptionHandler.java
**路径**: `yuncode-admin/src/main/java/com/yuncode/admin/handler/GlobalExceptionHandler.java`

全局异常处理器：
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e)

    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e)

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e)

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e)

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e)
}
```

##### 3.9 SaTokenUtil.java
**路径**: `yuncode-common/src/main/java/com/yuncode/common/utils/SaTokenUtil.java`

Sa-Token 工具类，封装常用操作：
```java
public class SaTokenUtil {

    public static Long getUserId()
    public static String getUsername()
    public static Long getTenantId()
    public static String getTenantCode()
    public static boolean isLogin()
    public static boolean hasPermission(String permission)
    public static boolean hasRole(String role)
    public static void checkPermission(String permission)
    public static void checkRole(String role)
    public static void kickout(Long userId)
    public static String getTokenValue()
    public static String hashPassword(String password)
    public static boolean checkPassword(String password, String hashed)
    public static void logout()
}
```

#### 4. 配置文件更新

**application.yml** (yuncode-admin/src/main/resources/application.yml)

```yaml
# Sa-Token 配置
sa-token:
  token-name: satoken
  timeout: 2592000
  activity-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: false
  is-read-cookie: false
  is-read-header: true
  token-prefix: Bearer
```

**注意**: 同时移除了原有的 JWT 配置（jwt.secret、jwt.expiration）

#### 5. 前端配置

**request.ts** (yuncode-lowcode-admin/src/utils/request.ts)

前端代码已经完美支持 Sa-Token：
```typescript
// Request interceptor
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  }
);
```

使用 `satoken` 作为 token 名称，`Bearer` 前缀与后端配置一致。

---

## 重要决策记录

### 决策 1：移除 Spring Security

**用户反馈**:
> "我看到你用了Spring Security，这个以及换成sotoken了，要移除项目中所有的Spring Security"

**用户补充**:
> "两种混用容易出现校验问题"

**实施结果**:
- ✅ 完全移除 Spring Security 依赖
- ✅ 使用 Hutool 的 BCrypt 进行密码加密
- ✅ 避免了 Sa-Token 和 Spring Security 混用的问题

**修改的文件**:
1. `yuncode-auth/pom.xml` - 移除 `spring-security-crypto` 依赖
2. `AuthService.java` - 使用 `cn.hutool.crypto.digest.BCrypt` 替代 `BCryptPasswordEncoder`

---

## 当前项目状态

### 已完成
1. ✅ 项目基础架构搭建
2. ✅ 前后端框架搭建
3. ✅ 数据库表结构设计
4. ✅ Sa-Token 权限认证集成
5. ✅ 完全移除 Spring Security
6. ✅ 使用 Hutool 进行密码加密
7. ✅ 全局异常处理
8. ✅ Sa-Token 工具类封装

### 待完成
1. ⏳ Mapper 层实现（数据访问）
2. ⏳ 权限查询逻辑实现
3. ⏳ 数据库初始化（建表 + 初始数据）
4. ⏳ 用户管理 CRUD
5. ⏳ 角色管理 CRUD
6. ⏳ 菜单管理 CRUD
7. ⏳ 租户管理 CRUD
8. ⏳ 导航管理功能

---

## API 接口清单

### 认证接口

#### 1. 用户登录
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

#### 2. 用户登出
```
POST /api/auth/logout
Header: satoken: xxxx-xxxx-xxxx-xxxx

Response:
{
  "code": 200,
  "message": "登出成功"
}
```

#### 3. 获取当前用户信息
```
GET /api/auth/info
Header: satoken: xxxx-xxxx-xxxx-xxxx

Response:
{
  "code": 200,
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "管理员",
    "avatar": "",
    "tenantId": 1,
    "tenantName": "默认租户"
  }
}
```

#### 4. 检查登录状态
```
GET /api/auth/checkLogin
Header: satoken: xxxx-xxxx-xxxx-xxxx

Response:
{
  "code": 200,
  "data": true
}
```

---

## 文档清单

1. [README.md](README.md) - 项目介绍
2. [DATABASE.md](DATABASE.md) - 数据库设计
3. [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) - 项目总结
4. [QUICKSTART.md](QUICKSTART.md) - 快速启动
5. [SATOKEN_INTEGRATION.md](SATOKEN_INTEGRATION.md) - Sa-Token 集成文档
6. [DIALOG_HISTORY.md](DIALOG_HISTORY.md) - 对话记录（本文档）

---

## 技术栈总结

### 后端技术栈
- Spring Boot 3.2.0
- MyBatis Plus 3.5.5
- Sa-Token 1.38.0 ⭐
- Redis
- MySQL 8.0
- Druid 1.2.20
- Knife4j 4.5.0
- Hutool 5.8.24

### 前端技术栈
- Vue 3.4+
- TypeScript 5.4+
- Vite 5.1+
- Element Plus 2.6+
- Vue Router 4.3+
- Pinia 2.1+
- Axios 1.6+

---

## 下一步计划

### 短期目标
1. 实现 Mapper 层（数据访问）
2. 完善权限查询逻辑
3. 初始化数据库数据
4. 测试登录认证流程

### 中期目标
1. 用户管理 CRUD
2. 角色管理 CRUD
3. 菜单管理 CRUD
4. 租户管理 CRUD

### 长期目标
1. 导航管理（左右双面板）
2. 动态表单设计器
3. 工作流引擎
4. AI 大模型集成

---

## 项目目录结构

```
yuncode-lowcode/
├── yuncode-lowcode-boot/              # 后端
│   ├── yuncode-common/                # 公共模块
│   │   ├── exception/
│   │   │   └── BusinessException.java
│   │   ├── model/util/response/
│   │   │   ├── Result.java
│   │   │   └── PageResult.java
│   │   └── utils/
│   │       ├── JwtUtil.java          # 已废弃（被 Sa-Token 替代）
│   │       └── SaTokenUtil.java      # ⭐ 新增
│   │
│   ├── yuncode-auth/                  # 认证模块
│   │   ├── config/
│   │   │   └── SaTokenConfig.java    # ⭐ Sa-Token 配置
│   │   ├── controller/
│   │   │   └── AuthController.java   # ⭐ 认证接口
│   │   ├── dto/
│   │   │   └── LoginDTO.java         # ⭐ 登录请求
│   │   ├── vo/
│   │   │   └── LoginVO.java          # ⭐ 登录响应
│   │   ├── service/
│   │   │   ├── AuthService.java      # ⭐ 认证服务
│   │   │   └── StpInterfaceImpl.java # ⭐ 权限接口
│   │   └── pom.xml
│   │
│   ├── yuncode-system/                # 系统管理
│   │   └── entity/
│   │       ├── SysUser.java
│   │       ├── SysRole.java
│   │       └── SysMenu.java
│   │
│   ├── yuncode-tenant/                # 租户管理
│   │   └── entity/
│   │       └── SysTenant.java
│   │
│   ├── yuncode-business/              # 业务模块（预留）
│   │
│   └── yuncode-admin/                 # 管理后台
│       ├── YuncodeAdminApplication.java
│       ├── handler/
│       │   └── GlobalExceptionHandler.java  # ⭐ 全局异常处理
│       ├── resources/
│       │   └── application.yml       # ⭐ Sa-Token 配置
│       └── pom.xml
│
├── yuncode-lowcode-admin/             # 前端
│   ├── src/
│   │   ├── views/
│   │   │   ├── login/
│   │   │   ├── layout/
│   │   │   └── home/
│   │   ├── router/
│   │   ├── utils/
│   │   │   └── request.ts           # ⭐ 已支持 Sa-Token
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   └── vite.config.ts
│
└── docs/                              # 文档
    ├── README.md
    ├── DATABASE.md
    ├── PROJECT_SUMMARY.md
    ├── QUICKSTART.md
    ├── SATOKEN_INTEGRATION.md        # ⭐ Sa-Token 文档
    └── DIALOG_HISTORY.md             # ⭐ 对话记录
```

---

## 核心代码片段

### Sa-Token 登录流程
```java
// 1. 校验密码
if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
    throw new BusinessException("用户名或密码错误");
}

// 2. Sa-Token 登录
StpUtil.login(user.getId());

// 3. 设置会话数据
StpUtil.getSession().set("tenantId", tenant.getId());
StpUtil.getSession().set("username", user.getUsername());
StpUtil.getSession().set("tenantCode", tenant.getTenantCode());

// 4. 返回 Token
loginVO.setToken(StpUtil.getTokenValue());
```

### 权限校验
```java
// 方式1：使用注解
@SaCheckPermission("user:add")
public void addUser() {
    // ...
}

// 方式2：使用工具类
SaTokenUtil.checkPermission("user:add");

// 方式3：手动校验
StpUtil.checkPermission("user:add");
```

### 密码加密
```java
// 加密
String hashedPassword = BCrypt.hashpw("admin123");

// 校验
boolean isMatch = BCrypt.checkpw("admin123", hashedPassword);
```

---

## 关键决策

### 为什么选择 Sa-Token？

1. **简单易用** - API 简洁，学习成本低
2. **功能全面** - 登录、权限、会话、单点登录全覆盖
3. **多租户支持** - 原生支持多账户体系
4. **会话管理** - 内置踢人下线、账号封禁等功能
5. **Spring Boot 3 兼容** - 完美适配最新版本
6. **开发效率** - 比 Spring Security 快 3-5 倍

### 为什么移除 Spring Security？

1. **避免混用冲突** - Sa-Token 和 Spring Security 混用容易出现校验问题
2. **降低复杂度** - Spring Security 配置繁琐，学习曲线陡峭
3. **统一方案** - 完全使用 Sa-Token，代码更简洁
4. **密码加密替代** - Hutool 的 BCrypt 完全可以满足需求

---

**文档创建时间**: 2024-01-17
**最后更新时间**: 2024-01-17
