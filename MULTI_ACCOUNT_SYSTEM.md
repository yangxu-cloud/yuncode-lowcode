# 多账号体系登录隔离实现文档

## 概述

系统现已实现完整的多账号体系登录隔离功能，支持管理员（admin）、租户管理员（tenant）和普通用户（user）三种登录类型在同一浏览器或不同页签中独立登录，互不干扰。

## 架构设计

### 1. 后端实现（Sa-Token 多账号体系）

#### 1.1 多账号配置类
**文件**: [SaTokenMultiAccountConfig.java](yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/config/SaTokenMultiAccountConfig.java)

为每种登录类型创建独立的 `StpLogic` 实例：
- `adminStpLogic` - 系统管理员登录体系
- `tenantStpLogic` - 租户管理员登录体系
- `userStpLogic` - 普通用户登录体系（@Primary，作为默认）

```java
@Bean(name = "adminStpLogic")
public StpLogic adminStpLogic() {
    return new StpLogic("admin");
}

@Bean(name = "tenantStpLogic")
public StpLogic tenantStpLogic() {
    return new StpLogic("tenant");
}

@Bean(name = "userStpLogic")
@Primary
public StpLogic userStpLogic() {
    return new StpLogic("user");
}
```

#### 1.2 登录服务隔离
**文件**:
- [AdminLoginService.java](yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/service/AdminLoginService.java)
- [TenantLoginService.java](yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/service/TenantLoginService.java)
- [UserLoginService.java](yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/service/UserLoginService.java)

每个登录服务注入对应的 `StpLogic` 实例：

```java
@Qualifier("adminStpLogic")
private final StpLogic adminStpLogic;

// 登录时使用对应的 StpLogic
adminStpLogic.login(user.getId());
adminStpLogic.getSession().set("userId", user.getId());
adminStpLogic.getSession().set("loginType", "admin");
```

#### 1.3 认证拦截器
**文件**: [SaTokenConfig.java](yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/config/SaTokenConfig.java)

根据请求路径自动选择对应的 `StpLogic` 进行认证：

```java
private StpLogic getStpLogicByPath(String path) {
    if (path.startsWith("/auth/admin") || path.startsWith("/console")) {
        return adminStpLogic;
    } else if (path.startsWith("/auth/tenant") || path.startsWith("/tenant")) {
        return tenantStpLogic;
    } else {
        return userStpLogic;
    }
}
```

### 2. 前端实现（LocalStorage 隔离）

#### 2.1 存储键隔离
**文件**: [user.ts](yuncode-lowcode-admin/src/stores/user.ts)

使用不同的 localStorage 键存储不同类型的登录信息：

```typescript
const getStorageKeys = (type: string) => ({
  token: `token_${type}`,      // token_admin, token_tenant, token_user
  userInfo: `userInfo_${type}`, // userInfo_admin, userInfo_tenant, userInfo_user
  loginType: `loginType_${type}`
});
```

#### 2.2 请求拦截器
**文件**: [request.ts](yuncode-lowcode-admin/src/utils/request.ts)

根据请求 URL 自动选择对应的 token：

```typescript
const requestPath = config.url || '';

if (requestPath.startsWith("/auth/admin") || requestPath.startsWith("/console")) {
  token = localStorage.getItem("token_admin");
  usedType = "admin";
} else if (requestPath.startsWith("/auth/tenant") || requestPath.startsWith("/tenant")) {
  token = localStorage.getItem("token_tenant");
  usedType = "tenant";
} else {
  token = localStorage.getItem("token_user");
  usedType = "user";
}
```

#### 2.3 路由守卫
**文件**: [router/index.ts](yuncode-lowcode-admin/src/router/index.ts)

根据路径恢复对应的登录信息：

```typescript
if (!userStore.isLogin) {
  let preferredType = "";
  if (to.path.startsWith("/console")) {
    preferredType = "admin";
  } else if (to.path.startsWith("/login")) {
    preferredType = "user";
  }
  userStore.initUserInfo(preferredType);
}
```

## 数据隔离流程

### 登录流程

1. **管理员登录** (`/console/login`)
   - 前端调用 `/auth/admin/login`
   - 后端使用 `adminStpLogic.login(userId)`
   - Token 存储到 `localStorage.token_admin`
   - 用户信息存储到 `localStorage.userInfo_admin`

2. **租户登录** (`/login` 选择租户登录)
   - 前端调用 `/auth/tenant/login`
   - 后端使用 `tenantStpLogic.login(userId)`
   - Token 存储到 `localStorage.token_tenant`
   - 用户信息存储到 `localStorage.userInfo_tenant`

3. **普通用户登录** (`/login`)
   - 前端调用 `/auth/user/login`
   - 后端使用 `userStpLogic.login(userId)`
   - Token 存储到 `localStorage.token_user`
   - 用户信息存储到 `localStorage.userInfo_user`

### 请求认证流程

1. 前端发起请求
2. Axios 拦截器根据请求 URL 选择对应的 token
3. 将 token 添加到 `Authorization: Bearer {token}` header
4. 后端接收请求，根据 URL 路径匹配对应的 StpLogic
5. 使用对应的 StpLogic 验证 token 有效性

## Redis 存储结构

不同账号体系的 session 在 Redis 中独立存储：

```
satoken:login:session:admin:{tokenValue}  -> admin session data
satoken:login:session:tenant:{tokenValue} -> tenant session data
satoken:login:session:user:{tokenValue}   -> user session data
```

## 在线用户管理

在线用户信息使用 token 作为 key 存储，因此不同账号体系的在线用户也是隔离的：

```
online_user:{adminToken}   -> admin online user
online_user:{tenantToken}  -> tenant online user
online_user:{userToken}    -> user online user
```

## 测试场景

### 场景1: 不同页签同时登录

1. **页签1**: 访问 `http://localhost:3000/console/login`
   - 登录账号: `admin / admin123`
   - 登录后访问: `/console/*` 页面
   - 使用 token: `token_admin`

2. **页签2**: 访问 `http://localhost:3000/login`
   - 登录账号: `default / testuser / admin123`
   - 登录后访问: `/home`, `/system/*` 等页面
   - 使用 token: `token_user`

**结果**: 两个页签保持独立登录状态，互不影响

### 场景2: 退出登录隔离

1. 管理员在页签1退出 → 跳转到 `/console/login`
2. 普通用户在页签2仍保持登录状态
3. 普通用户退出 → 跳转到 `/login`

**结果**: 不同类型的退出操作互不影响

### 场景3: 刷新页面保持状态

1. 管理员页面刷新 → 从 `token_admin` 和 `userInfo_admin` 恢复
2. 普通用户页面刷新 → 从 `token_user` 和 `userInfo_user` 恢复

**结果**: 刷新后自动恢复对应类型的登录状态

## API 端点映射

| 登录类型 | 登录 API | Token 存储 | StpLogic | 适用路径 |
|---------|---------|-----------|----------|---------|
| 管理员 | `/auth/admin/login` | `token_admin` | `adminStpLogic` | `/console/*`, `/auth/admin/*` |
| 租户 | `/auth/tenant/login` | `token_tenant` | `tenantStpLogic` | `/tenant/*`, `/auth/tenant/*` |
| 用户 | `/auth/user/login` | `token_user` | `userStpLogic` | 其他所有路径 |

## 注意事项

1. **Token 前缀**: Sa-Token 配置中不设置 `token-prefix`，在前端手动添加 `Bearer` 前缀
2. **默认 StpLogic**: `userStpLogic` 使用 `@Primary` 注解，作为默认的 StpLogic 实例
3. **路径匹配**: 后端和前端都根据路径前缀选择对应的 token 和 StpLogic，保持一致
4. **并发登录**: `is-concurrent: true` 允许同一账号在不同设备/页签同时登录
5. **Token 共享**: `is-share: false` 每次登录生成新的 token，不共享

## 兼容性

- **Spring Boot**: 3.x
- **Sa-Token**: 最新版本（支持多账号体系）
- **Vue**: 3.x
- **Pinia**: 最新版本
- **TypeScript**: 5.x

## 相关文件

### 后端文件
- `yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/config/SaTokenMultiAccountConfig.java`
- `yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/config/SaTokenConfig.java`
- `yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/service/AdminLoginService.java`
- `yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/service/TenantLoginService.java`
- `yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/service/UserLoginService.java`
- `yuncode-lowcode-boot/yuncode-auth/src/main/java/com/yuncode/auth/filter/SaTokenHeaderFilter.java`

### 前端文件
- `yuncode-lowcode-admin/src/stores/user.ts`
- `yuncode-lowcode-admin/src/utils/request.ts`
- `yuncode-lowcode-admin/src/router/index.ts`
- `yuncode-lowcode-admin/src/api/auth.ts`
- `yuncode-lowcode-admin/src/views/layout/index.vue`

## 故障排查

### 问题1: 登录后刷新页面丢失登录状态
**原因**: `initUserInfo` 没有正确恢复对应类型的登录信息
**解决**: 确保路由守卫根据路径传递 `preferredType`

### 问题2: 请求返回 401 未登录
**原因**: 请求拦截器选择了错误的 token
**解决**: 检查 `request.ts` 中的路径匹配逻辑

### 问题3: 后端无法识别 token
**原因**: Sa-Token 拦截器使用了错误的 StpLogic
**解决**: 检查 `SaTokenConfig.java` 中的路径匹配逻辑

### 问题4: 不同类型的登录相互覆盖
**原因**: localStorage 使用了相同的 key
**解决**: 确保使用 `token_${type}` 格式的独立 key
