# 前端登录问题排查指南

## 问题描述
打开页面直接跳到首页，没有跳转到登录页

## 原因分析
浏览器的 `localStorage` 中可能还保存着旧的 token，导致系统误判为已登录状态

---

## 解决方案

### 方案 1：清除浏览器 localStorage（推荐）⭐

#### Chrome/Edge 浏览器
1. 按 `F12` 打开开发者工具
2. 切换到 `Application` 或 `应用程序` 标签
3. 左侧找到 `Local Storage` → 选择你的网站地址
4. 删除以下键值：
   - `token`
   - `userInfo`
5. 刷新页面（`Ctrl + R` 或 `F5`）

#### 使用控制台快速清除
1. 按 `F12` 打开开发者工具
2. 切换到 `Console` 标签
3. 输入并执行：
```javascript
localStorage.clear();
location.reload();
```

### 方案 2：无痕模式测试
1. 打开无痕/隐私浏览窗口：
   - Chrome: `Ctrl + Shift + N`
   - Edge: `Ctrl + Shift + P`
   - Firefox: `Ctrl + Shift + P`
2. 访问 `http://localhost:3000`
3. 应该会正常跳转到登录页

### 方案 3：清除所有浏览器数据
1. Chrome/Edge: `Ctrl + Shift + Delete`
2. 选择"缓存的图片和文件" + "Cookie 和其他站点数据"
3. 时间范围选"全部时间"
4. 点击"清除数据"

---

## 验证修复

### 1. 检查路由跳转
打开页面后，应该：
- ✅ URL 自动跳转到 `http://localhost:3000/login`
- ✅ 显示登录表单

### 2. 检查控制台
按 `F12`，在 Console 标签中检查：
- ✅ 没有红色错误信息
- ✅ 可以看到路由日志（如果有的话）

### 3. 测试登录功能
使用默认账号登录：
```json
{
  "tenantCode": "default",
  "username": "admin",
  "password": "admin123"
}
```

点击登录后：
- ✅ 跳转到首页 `/home`
- ✅ 可以正常访问系统功能

---

## 开发建议

### 在开发环境自动清除 token
在 `main.ts` 中添加以下代码（开发环境自动清除 token）：

```typescript
// main.ts
const app = createApp(App);

// 开发环境：启动时清除旧 token（可选）
if (import.meta.env.DEV) {
  const clearToken = localStorage.getItem('token');
  if (clearToken) {
    console.log('开发模式：清除旧 token');
    localStorage.clear();
  }
}

app.use(createPinia());
// ... 其他代码
```

### 添加调试日志
在 `router/index.ts` 的路由守卫中添加日志：

```typescript
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  userStore.initUserInfo();

  // 调试日志
  console.log('路由守卫:', {
    to: to.path,
    isLogin: userStore.isLogin,
    hasToken: !!userStore.token
  });

  // ... 原有代码
});
```

---

## 常见问题

### Q1: 清除后还是直接跳到首页
**A**: 检查是否有 cookie 存储，也需要清除：
```javascript
// 在控制台执行
document.cookie.split(";").forEach(c => document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/"));
location.reload();
```

### Q2: 登录后刷新页面又回到登录页
**A**: 检查 `initUserInfo()` 是否正确恢复 token，在控制台查看：
```javascript
console.log('Token:', localStorage.getItem('token'));
console.log('UserInfo:', localStorage.getItem('userInfo'));
```

### Q3: 路由守卫不生效
**A**: 确保 Pinia 在 Router 之前初始化：
```typescript
// main.ts - 正确顺序
app.use(createPinia());  // 1. 先 Pinia
app.use(router);          // 2. 后 Router
```

---

## 正常的登录流程

1. **未登录状态**
   ```
   访问 / → 路由守卫检查 → 未登录 → 跳转到 /login
   ```

2. **登录成功**
   ```
   /login 提交表单 → 调用登录 API → 保存 token 到 localStorage → 跳转到 /home
   ```

3. **已登录状态**
   ```
   刷新页面 → initUserInfo() 恢复 token → 路由守卫检查 → 已登录 → 正常访问
   ```

---

## 联系支持

如果问题持续存在，请提供：
1. 浏览器控制台的完整日志
2. localStorage 的内容（F12 → Application → Local Storage）
3. 网络请求的响应（F12 → Network 标签）
