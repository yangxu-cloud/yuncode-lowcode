# 多页签登录隔离测试指南

## 功能说明
系统现在支持在不同浏览器页签中同时登录不同类型的用户，每个页签保持独立的登录会话。

## 登录类型与存储隔离

### 管理员登录
- 登录页面: `http://localhost:3000/console/login`
- 默认账号: `admin / admin123`
- 存储: `token_admin`, `userInfo_admin`
- Token用于: `/console/*`, `/auth/admin/*`

### 租户登录
- 登录页面: `http://localhost:3000/login`
- 默认账号: `default租户 / testuser / admin123`
- 存储: `token_tenant`, `userInfo_tenant`
- Token用于: `/tenant/*`, `/auth/tenant/*`

### 用户登录
- 登录页面: `http://localhost:3000/login`
- 默认账号: `default租户 / testuser / admin123`
- 存储: `token_user`, `userInfo_user`
- Token用于: 所有其他路径

## 测试步骤

### 测试1: 管理员和用户同时登录
1. **页签1**: 访问 `http://localhost:3000/console/login`，使用 `admin/admin123` 登录
2. **页签2**: 访问 `http://localhost:3000/login`，使用 `default/testuser/admin123` 登录
3. 验证:
   - 页签1显示管理员用户名 "admin"
   - 页签2显示租户用户名 "testuser"
   - 刷新页签1，仍然显示 "admin"
   - 刷新页签2，仍然显示 "testuser"

### 测试2: 检查浏览器控制台日志
在浏览器开发者工具的Console中查看:

**页签1 (管理员) 应显示:**
```
[Request] 添加 Token (admin): eyJ0eXAiOiJKV1Q... URL: /console/...
```

**页签2 (用户) 应显示:**
```
[Request] 添加 Token (user): eyJ0eXAiOiJKV1Q... URL: /api/...
```

### 测试3: 退出登录隔离
1. 在页签1点击退出，应跳转到 `/console/login`
2. 验证页签2仍然保持登录状态
3. 在页签2点击退出，应跳转到 `/login`
4. 两个页签的登录状态互不影响

### 测试4: LocalStorage 验证
打开浏览器开发者工具 → Application → Local Storage:

管理员登录后应有:
```
token_admin: "eyJ0eXAiOiJKV1Q..."
userInfo_admin: "{\"username\":\"admin\",\"loginType\":\"admin\",...}"
loginType: "admin"
```

用户登录后应有:
```
token_user: "eyJ0eXAiOiJKV1Q..."
userInfo_user: "{\"username\":\"testuser\",\"loginType\":\"user\",...}"
loginType: "user"
```

## 实现原理

### Token选择逻辑 (request.ts)
请求拦截器根据请求URL路径自动选择正确的token:

```typescript
if (requestPath.startsWith("/auth/admin") || requestPath.startsWith("/console")) {
  token = localStorage.getItem("token_admin");  // 管理员请求
} else if (requestPath.startsWith("/auth/tenant") || requestPath.startsWith("/tenant")) {
  token = localStorage.getItem("token_tenant");  // 租户请求
} else {
  token = localStorage.getItem("token_user");    // 普通用户请求
}
```

### 路由守卫逻辑 (router/index.ts)
路由守卫根据当前路径恢复对应的用户信息:

```typescript
if (to.path.startsWith("/console")) {
  preferredType = "admin";  // 管理员页面
} else if (to.path.startsWith("/login")) {
  preferredType = "user";   // 普通登录页
}

userStore.initUserInfo(preferredType);
```

### 用户状态管理 (stores/user.ts)
使用独立的localStorage键存储不同类型的登录信息:

```typescript
const getStorageKeys = (type: string) => ({
  token: `token_${type}`,
  userInfo: `userInfo_${type}`,
  loginType: `loginType_${type}`
});
```

## 注意事项

1. **无需手动切换身份**: 每个页签根据URL自动使用对应类型的token
2. **登录状态隔离**: 不同页签的登录状态完全独立
3. **退出登录正确跳转**:
   - 管理员退出 → `/console/login`
   - 普通用户退出 → `/login`
4. **刷新页面保持状态**: 刷新后根据路径恢复对应的登录信息

## 常见问题

### Q: 为什么一个页签登录后，另一个页签显示错误用户?
A: 清除浏览器localStorage，确保没有旧数据干扰:
```javascript
localStorage.clear();
```

### Q: 如何确认使用了正确的token?
A: 打开浏览器控制台，查看 `[Request] 添加 Token (xxx)` 日志，确认token类型与当前页面匹配。

### Q: 能否在同一个页签切换登录类型?
A: 不需要。每个页签根据访问的URL自动使用对应的token。如需切换身份，在新页签登录即可。
