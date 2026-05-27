# 平台架构审查报告

> 日期: 2026-05-22
> 范围: 全平台后端（yuncode-lowcode-boot + yuncode-pure-admin 前端集成）
> 审查方式: 源码静态分析

---

## 🔴 P0 — 关键问题（运行异常 / 安全漏洞）

### 1. 生产环境 MyBatis SQL 日志配置错误

- 文件: [application-prod.yml:16](yuncode-lowcode-boot/yuncode-admin/src/main/resources/application-prod.yml#L16)
- 现象: `log-impl: org.apache.ibatis.logging.log4j2.Log4j2Impl`
- 原因: 项目使用 Logback（有 `logback-spring.xml`），无 Log4j2 依赖
- 后果: prod 模式启动 MyBatis 报 ClassNotFoundException
- 修复: 改为 `org.apache.ibatis.logging.slf4j.Slf4jImpl`

### 2. Gateway 和 Admin 响应格式不一致

- Gateway [GlobalExceptionHandler](yuncode-lowcode-boot/yuncode-gateway/src/main/java/com/yuncode/gateway/handler/GlobalExceptionHandler.java) 返回 `{success, code, message, timestamp, path}`
- Admin [GlobalExceptionHandler](yuncode-lowcode-boot/yuncode-admin/src/main/java/com/yuncode/admin/handler/GlobalExceptionHandler.java) 返回 `{code, message, data, timestamp}`
- 差异: Gateway 用 `success`(boolean)，Admin 用 `data`(object)
- 后果: 前端通过 Gateway 访问时响应解析失败
- 修复: Gateway 需对齐 Admin 的 Result 格式

### 3. Gateway 异常处理器手动拼 JSON

- 文件: [GlobalExceptionHandler.java:64](yuncode-lowcode-boot/yuncode-gateway/src/main/java/com/yuncode/gateway/handler/GlobalExceptionHandler.java#L64)
- 现象: `StringBuilder` 手动拼接 JSON
- 后果: 字符串含特殊字符时 JSON 格式被破坏
- 修复: 使用 Jackson 的 ObjectMapper 序列化

### 4. 三种登录类型隔离形同虚设

- 平台定义了 `admin`/`tenant`/`user` 三种登录类型，但共用同一个 `StpLogicJwtForSimple` bean
- 所有类型调用 `StpUtil.login(user.getId())`，共享同一令牌命名空间
- 登录类型仅作为会话声明存储，无加密边界
- 后果: 有效 token 的持有者可通过篡改会话数据越权
- 修复: 为每种登录类型创建独立的 StpLogic 实例，使用不同的 JWT 签名密钥

### 5. 未实施暴力破解防护 / 限流

- 登录端点 `/auth/**` 从 Sa-Token 拦截器中排除，无任何保护
- `SettingsVO` 定义了 `loginMaxAttempts`、`loginLockDuration` 等设置，但代码中未使用
- Gateway `KeyResolver` bean 已定义但未在任何路由中配置
- 后果: 攻击者可无限暴力破解任意账户
- 修复: 实施 Redis 计数器 + 账户锁定逻辑，Gateway 配置 `RequestRateLimiter`

### 6. CORS 配置全开放

- Gateway [application.yml](yuncode-lowcode-boot/yuncode-gateway/src/main/resources/application.yml) 配置:
  ```yaml
  allowedOriginPatterns: "*"
  allowCredentials: true
  ```
- Admin 无 CORS 配置（Servlet 容器默认允许同源）
- 后果: 任意网站可向网关发起认证跨域请求
- 修复: 限制为 `localhost:3000`（开发）和实际部署域名（生产）

---

## 🟡 P1 — 中等问题（安全隐患 / 架构缺陷）

### 7. JWT 默认密钥硬编码

- `jwt-secret-key: ${JWT_SECRET_KEY:yuncode-lowcode-sa-token-...}`
- 默认密钥公开，生产环境若忘记通过环境变量覆盖，任何人可伪造 JWT

### 8. 默认密码 "123456"

- `system.default-password: "123456"` + `UserService.createUser()` 默认使用
- 建议: 强制首次登录修改密码

### 9. Gateway 认证过滤器是空壳

- [AuthenticationFilter](yuncode-lowcode-boot/yuncode-gateway/src/main/java/com/yuncode/gateway/filter/AuthenticationFilter.java) 只检查 token 是否存在，不验证
- 网关不拒绝无效/过期 token，所有保护依赖后端
- 建议: 使用 Sa-Token 响应式集成实现网关层真实 token 校验

### 10. 双入口架构

- 前端可直接访问 Admin (`:8080/api/`) 或通过 Gateway (`:9000/api/`)
- 认证状态、CORS 策略可能不一致，增加攻击面

### 11. 租户隔离部分绕过

- MyBatis-Plus 多租户拦截器排除了 `sys_user`、`sys_role` 等关键表
- 注释说明"由 Service 层动态控制"，但依赖开发人员在每个查询手动加 `tenant_id`
- 后果: 一个遗漏的查询即可暴露租户 A 数据给租户 B
- 建议: 减少排除表数量，或增加编译时检查

### 12. Token 调试端点未保护

- [TokenDebugController](yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/controller/TokenDebugController.java) 在 `/auth/**` 下，被排除在 Sa-Token 拦截器外
- 任何人无需 token 即可调用，泄露 token 值和内部状态
- 建议: 用 `@Profile("dev")` 保护或从生产构建中移除

### 13. 请求参数传递 TenantId

- [TenantFilter](yuncode-lowcode-boot/yuncode-gateway/src/main/java/com/yuncode/gateway/filter/TenantFilter.java) 会退化为从 query params 读取 `tenantId`
- URL 参数会被记录在服务器日志、Referer header 中
- 建议: 仅通过 `X-Tenant-Id` header 接受

### 14. createBy/updateBy 硬编码为 "system"

- `MyBatisPlusConfig.getCurrentUser()` 返回固定值 `"system"`
- 所有数据变更的审计字段无法追溯真实操作人

### 15. 双重登录日志

- `AbstractLoginStrategy.login()` finally 块记录日志 + `@LoginLog` 切面再记录一次
- 每次登录产生两条重复日志，影响审计

---

## 🟢 P2 — 轻微问题（建议改进）

### 16. Gateway 对 common 的 web 排除不再必要

- SDK 拆分后 `yuncode-common` 已无 `spring-boot-starter-web` 依赖
- Gateway pom.xml 中的 `<exclusion>` 可清理

### 17. prod.yml Log4j2 引用（见 P0-1）

### 18. BCrypt 轮数仅 10 轮

- 2026 年标准建议 12-14 轮

### 19. 密码强度未在代码层强制

- `SettingsVO` 定义了强度设置（minLength、大写、数字、特殊字符）
- `LoginDTO` 只做了 `@NotBlank` 验证，未实施强度检查

### 20. 令牌过期时间 30 天过长

- `sa-token.timeout: 2592000`（30 天），管理用途建议缩短

### 21. 无 CSRF 保护

- 虽然目前不使用 cookie 传 token，但架构层面缺少 CSRF 防御

### 22. 无安全响应头

- 未配置 `Strict-Transport-Security`、`Content-Security-Policy`、`X-Frame-Options`

---

## 📋 前后端集成问题

### 🔴 P0 — 阻塞性问题

#### 23. Token 请求头不匹配

- 前端 [http/index.ts:122-124](yuncode-pure-admin/src/utils/http/index.ts#L122-L124): 发送 `Authorization: Bearer <jwt>`
- 后端 [application.yml:77-84](yuncode-lowcode-boot/yuncode-admin/src/main/resources/application.yml#L77-L84): Sa-Token 配置 `token-name: token`，读取 header 名称为 `token`
- 后果: 所有认证 API 调用都返回 401，因为 Sa-Token 找不到名为 `token` 的 header
- 修复: 前端改为 `config.headers["token"] = data.accessToken`（去掉 Bearer 前缀）

#### 24. Mock 登录响应格式不匹配

- Mock [login.ts:9-24](yuncode-pure-admin/mock/login.ts#L9-L24): 返回 `{ success: true, data: {...} }`，无 `code` 字段
- 前端 [user.ts:103](yuncode-pure-admin/src/store/modules/user.ts#L103): 检查 `response?.code === 200`
- 后果: Mock 模式下 `code === undefined`，登录永远被视为失败
- 修复: Mock 改为 `{ code: 200, message: "success", data: {...} }`

#### 25. 刷新令牌流程完全断裂

- 前端 [user.ts:111](yuncode-pure-admin/src/store/modules/user.ts#L111): 硬编码 `refreshToken: ""`
- 后端 `LoginVO`: 无 `refreshToken` 字段
- 后端: 无 `/refresh-token` 端点
- 后果: JWT 过期后自动刷新机制永远失败

#### 26. 用户角色硬编码为 `["admin"]`

- 前端 [user.ts:115](yuncode-pure-admin/src/store/modules/user.ts#L115): 登录适配器硬编码 `roles: ["admin"]`，忽略后端实际角色
- 前端 [utils.ts:85-93](yuncode-pure-admin/src/router/utils.ts#L85-L93): 路由权限过滤始终通过
- 后果: 前端路由级权限控制完全失效，所有用户看到所有菜单

### 🟡 P1 — 中等问题

#### 27. 硬编码 `/api` 前缀

- [http/index.ts:74-76](yuncode-pure-admin/src/utils/http/index.ts#L74-L76): 定义了 `API_BASE_PREFIX` 变量但硬编码字符串 `"/api"`
- 后果: 修改 `VITE_API_BASE_PREFIX` 环境变量无效
- 修复: 使用 `API_BASE_PREFIX` 变量替代字面量

#### 28. 登录白名单前缀不一致

- [http/index.ts:90-96](yuncode-pure-admin/src/utils/http/index.ts#L90-L96): `/refresh-token` 和 `/login` 没有 `API_BASE_PREFIX` 前缀，但 `auth/*/login` 有
- 后果: `"/login"` 因 URL 已被添加 `/api` 前缀而永远无法匹配白名单，是死代码

#### 29. 后端（非网关路径）无 CORS 配置

- Admin [application.yml](yuncode-lowcode-boot/yuncode-admin/src/main/resources/application.yml): 无 CORS 配置
- 后果: 生产环境若前端跳过网关直接访问后端，产生 CORS 错误

#### 30. Gateway Spring Boot Maven Plugin 可能未正确打包

- [gateway/pom.xml:98-105](yuncode-lowcode-boot/yuncode-gateway/pom.xml#L98-L105): 未设置 `<skip>false</skip>`，继承根 profile 的 `skip=true`
- 后果: Gateway JAR 可能不是可执行的 Spring Boot fat JAR

#### 31. Sa-Token 不必要地在 Gateway classpath 上

- yuncode-common 以 compile scope 依赖 `sa-token-spring-boot3-starter`，Gateway 间接继承
- Gateway 已排除 `spring-boot-starter-web` 但仍携带 Sa-Token JAR
- 风险: Sa-Token autoconfiguration 可能在 reactive 环境中引发类加载错误

#### 32. `deriveRouteName` 对目录菜单返回 null

- [MenuServiceImpl.java:431-453](yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/service/impl/MenuServiceImpl.java#L431-L453): 只为 type 1（叶子菜单）设置 `component`，type 0（目录）的 `getName()` 返回 null
- 前端处理了此情况（从第一个子菜单派生名称），但需确保目录总有子菜单

#### 33. 双重登录日志（扩展）

- 补充: `AbstractLoginStrategy.login()` finally 块 + `@LoginLog` 切面各记录一次登录日志
- 影响: 审计日志中存在重复记录

### 🟢 P2 — 轻微问题

#### 34. Gateway pom 中 redundant 的 properties 和 dependencyManagement

- [gateway/pom.xml](yuncode-lowcode-boot/yuncode-gateway/pom.xml): 重复声明 `spring-cloud.version`、`spring-cloud-alibaba.version` 和整个 `dependencyManagement` 块
- 这些已从根 pom 继承，重复存在维护风险

#### 35. 根 dependencyManagement 未使用的条目

- `druid-spring-boot-3-starter` 和 `minio` 在根 pom 中管理版本，但无任何模块使用

#### 36. yuncode-common 的 compile scope 过重

- `mybatis-plus-boot-starter` 和 `knife4j-openapi3-*` 应在 yuncode-common 中标记为 `<optional>true</optional>`
- 所有依赖 common 的模块（包括 Gateway）都得到 MyBatis/JDBC/Swagger 依赖

#### 37. `yuncodeConfig.tokenName` 与后端不匹配

- 前端 [config/index.ts:60](yuncode-pure-admin/src/config/index.ts#L60): `tokenName: 'yuncode-token'`
- 后端 `application.yml:76`: `sa-token.token-name: token`
- 前端的 token name 配置与后端实际值不一致

#### 38. App 模块命名不一致

- 三个 App 模块的 `<name>` 元素不规范（一个为中文名，另两个为占位符）

#### 39. 无 CSRF 保护（补充）

- 目前虽不用 cookie 传 token，但架构层面缺少 CSRF 防御

#### 40. 无安全响应头（补充）

- 未配置 `Strict-Transport-Security`、`Content-Security-Policy`、`X-Frame-Options`

---

## 📋 模块依赖架构检查

### 整体评估

依赖图是严格的 DAG（有向无环图），无循环依赖：

```
platform-sdk → common → tenant → system → auth → admin
```

所有跨模块 import 都有对应的 pom.xml 声明。版本全部通过根 pom.xml `<dependencyManagement>` 集中管理，无版本冲突。

### 已验证正确的架构决策

| 决策 | 状态 |
|------|------|
| Gateway 排除 `spring-boot-starter-web` | 正确且必要（否则 WebMVC + WebFlux 冲突） |
| Gateway 限制 `@ComponentScan(basePackages = "com.yuncode.gateway")` | 正确（防止发现 TraceIdFilter 等 Servlet 组件） |
| Admin 排除 `com.yuncode.user.apps.*` 组件扫描 | 正确（由 HotAppDeployer 管理） |
| Root profile 中 repackage skip=true（非 boot 模块） | 正确模式，但 Gateway 需显式覆盖 |
| App 模块仅依赖 yuncode-platform-sdk | 符合 SDK 隔离设计 |

### 关键问题

1. **Gateway Spring Boot Maven Plugin 配置不完整**: 无 `<skip>false</skip>` 覆盖，可能不生成可执行 JAR
2. **Sa-Token 在 Gateway 上不必要**: 建议在 common 中标记为 `<optional>true</optional>` 或在 Gateway 中排除
3. **common 的 compile scope 过重**: mybatis-plus 和 knife4j 应标记 optional
4. **Gateway pom 冗余声明**: 已从根 pom 继承的配置重复声明有维护风险
5. **druid 和 minio 已管理但未使用**: 要么移除要么加注释说明为预留
