# 认证授权架构

> 合并自：SATOKEN_INTEGRATION.md / SATOKEN_JWT_INTEGRATION.md / TOKEN_CONFIG.md / MULTI_ACCOUNT_SYSTEM.md

## 技术选型

采用 **Sa-Token 1.38.0 + JWT (StpLogicJwtForSimple) + Redis** 方案。

| 特性 | 方案 |
|------|------|
| 认证框架 | Sa-Token (spring-boot3-starter) |
| Token 格式 | JWT (sa-token-jwt) |
| 会话存储 | Redis (lettuce + jackson) |
| 密码加密 | Hutool BCrypt |
| 多账号 | 独立 StpLogic 实例 (admin/tenant/user) |

## 核心配置

```yaml
sa-token:
  token-name: token
  timeout: 2592000
  is-concurrent: true      # 允许多端同时登录
  is-share: false           # 每次登录生成新 token
  token-style: simple-uuid  # JWT 模式必须
  is-read-cookie: false
  is-read-header: true
  jwt-secret-key: "yuncode-lowcode-sa-token-jwt-secret-key-2024-..."
```

JWT 模式通过 `StpLogicJwtForSimple` 启用，Token 签名存储在 JWT Payload 中，Redis 负责会话管理和主动失效（踢人下线）。

## 多账号体系

三种登录类型使用独立的 `StpLogic` 实例和 Redis key 前缀：

| 类型 | StpLogic Bean | 路径前缀 | Redis 前缀 |
|------|---------------|---------|-----------|
| 管理员 | adminStpLogic | `/console/*` | `satoken:login:session:admin:` |
| 租户管理员 | tenantStpLogic | `/tenant/*` | `satoken:login:session:tenant:` |
| 普通用户 | userStpLogic (@Primary) | 其他 | `satoken:login:session:user:` |

前端根据请求路径自动选择对应的 localStorage key（`token_admin` / `token_tenant` / `token_user`）。

## 认证流程

```
用户登录 → StpUtil.login(userId) → 生成 JWT → 存入 Redis Session
每次请求 → SaInterceptor 拦截 → 从 Header 提取 token → 验签 → 放行
```

## 相关后端组件

- `SaTokenConfig.java` — 拦截器配置 + URL 路径匹配
- `SaTokenMultiAccountConfig.java` — 多账号 StpLogic Bean
- `StpInterfaceImpl.java` — 权限/角色查询
- `AuthService.java` / `AdminLoginService.java` / `TenantLoginService.java` / `UserLoginService.java`
- `SaTokenHeaderFilter.java` — 手动处理 Bearer 前缀
