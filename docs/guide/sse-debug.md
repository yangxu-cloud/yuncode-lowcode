# SSE 踢出通知排查指南

## 问题：没有显示下线的弹出消息

### 排查步骤

#### 1️⃣ 检查前端是否建立了 SSE 连接

**使用调试工具**：
1. 在浏览器中打开 `debug-sse.html`
2. 点击"🔌 建立 SSE 连接"按钮
3. 查看日志中是否有"✅ SSE 连接成功建立"的消息

**或者使用浏览器开发者工具**：
```javascript
// 在浏览器控制台执行
const eventSource = new EventSource('/api/user/notifications')

eventSource.addEventListener('connected', (e) => {
  console.log('✅ SSE 已连接:', e.data)
})

eventSource.addEventListener('kick_out', (e) => {
  console.log('⚠️ 收到踢出通知:', JSON.parse(e.data))
})

eventSource.onerror = (e) => {
  console.error('❌ SSE 错误:', e)
}
```

---

#### 2️⃣ 检查后端日志

启动应用后，当您建立 SSE 连接时，应该看到以下日志：

```
========================================
收到 SSE 连接请求
✅ 用户身份验证成功: userId=123456
当前 SSE 连接数: 0
✅ SSE 连接创建成功: userId=123456, 当前连接数=1
========================================
```

如果看不到这些日志，说明：
- ❌ 前端没有建立 SSE 连接
- ❌ 或者请求被 Sa-Token 拦截了

---

#### 3️⃣ 检查 Token 是否有效

**在浏览器控制台执行**：
```javascript
// 检查是否有 token
const token = localStorage.getItem('satoken')
console.log('Token:', token ? token.substring(0, 30) + '...' : '❌ 未找到')

// 如果没有 token，需要先登录
if (!token) {
  console.error('❌ 请先登录系统！')
}
```

---

#### 4️⃣ 测试 SSE 端点是否可访问

**使用 curl 测试**（需要替换实际的 token）：
```bash
curl -H "satoken: YOUR_TOKEN_HERE" \
     -H "Accept: text/event-stream" \
     http://localhost:8080/api/user/notifications
```

预期结果：
```
data: {"type":"connected","message":"SSE 连接已建立"}

```

如果返回 401 或 403，说明认证失败。

---

#### 5️⃣ 检查 Sa-Token 配置

确认 `/api/user/notifications` 路径没有被排除在认证之外。

查看 `SaTokenConfig.java`：
```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/auth/**",
                // ... 其他排除路径
            );
}
```

**问题**：`/api/user/notifications` 需要认证，但前端可能没有正确传递 token。

---

#### 6️⃣ 踢出用户时的完整测试流程

**管理员端**：
1. 登录管理员账号
2. 进入在线用户管理页面
3. 找到 testuser，点击"踢出"按钮

**被踢出用户端（testuser）**：
1. 确保 testuser 已登录
2. 在浏览器中打开 `debug-sse.html`
3. 点击"🔌 建立 SSE 连接"
4. 等待管理员踢出
5. 查看是否收到踢出通知

**后端日志应该显示**：
```
开始踢出用户，token: xxx...
找到 token 对应的用户 ID: 123456
准备发送踢出通知: userId=123456, 当前SSE连接数=1
用户 SSE 在线状态: userId=123456, isOnline=true
⏰ 发送踢出通知: userId=123456, 倒计时=5秒
已发送踢出通知: userId=123456, 倒计时=5秒
...
已踢出用户: loginId=123456
```

---

### 常见问题及解决方案

#### ❓ 问题1：前端显示 401 Unauthorized

**原因**：Token 没有正确传递到后端

**解决方案**：
- 确保 `localStorage` 中有 `satoken`
- 检查浏览器是否自动发送 Cookie（如果使用 Cookie 存储）
- 或者手动在 URL 中添加 token：`/api/user/notifications?token=xxx`

---

#### ❓ 问题2：SSE 连接建立后立即断开

**原因**：可能是超时设置太短，或者被 Nginx/代理服务器断开

**解决方案**：
- 增加 `SseEmitter` 的超时时间
- 检查 Nginx 配置：
  ```nginx
  proxy_read_timeout 3600s;
  proxy_send_timeout 3600s;
  ```

---

#### ❓ 问题3：收到通知但没有弹出框

**原因**：前端监听事件名称不匹配

**解决方案**：
- 确认后端发送的事件名称是 `kick_out`
- 前端监听代码：
  ```javascript
  eventSource.addEventListener('kick_out', (event) => {
    const data = JSON.parse(event.data)
    // 显示弹窗
  })
  ```

---

#### ❓ 问题4：userId 不匹配，找不到 SSE 连接

**原因**：OnlineUser 中的 userId 与建立 SSE 连接时的 userId 不一致

**检查方法**：
```bash
# 查看 Redis 中的在线用户数据
redis-cli
127.0.0.1:6379> keys online_user:*
127.0.0.1:6379> get online_user:TOKEN_VALUE
```

**解决方案**：
- 确保登录时正确设置了 `onlineUser.setUserId(user.getId())`
- 检查 SSE 连接时的 userId 是否从 token 中正确获取

---

### 快速验证脚本

在浏览器控制台执行以下脚本，可以快速测试整个流程：

```javascript
// 1. 检查 token
const token = localStorage.getItem('satoken')
console.log('Token 状态:', token ? '✅' : '❌')

if (!token) {
  alert('❌ 请先登录！')
} else {
  console.log('Token 值:', token.substring(0, 30) + '...')

  // 2. 建立 SSE 连接
  console.log('🔌 正在建立 SSE 连接...')
  const eventSource = new EventSource('/api/user/notifications')

  eventSource.addEventListener('connected', (e) => {
    console.log('✅ SSE 已连接:', e.data)
  })

  eventSource.addEventListener('kick_out', (e) => {
    const data = JSON.parse(e.data)
    console.log('⚠️ 收到踢出通知:', data)
    alert(`⚠️ ${data.message}\n倒计时: ${data.countdown} 秒`)
  })

  eventSource.addEventListener('kick_out_update', (e) => {
    const data = JSON.parse(e.data)
    console.log(`⏱️ 倒计时: ${data.countdown} 秒`)
  })

  eventSource.addEventListener('kick_out_final', (e) => {
    console.log('👋 倒计时结束')
    alert('👋 即将退出登录')
    eventSource.close()
    localStorage.clear()
    window.location.href = '/login'
  })

  eventSource.onerror = (e) => {
    console.error('❌ SSE 错误:', e)
    console.error('ReadyState:', eventSource.readyState)
  }
}
```

---

### 日志检查清单

- [ ] 前端日志：建立 SSE 连接成功
- [ ] 后端日志：收到 SSE 连接请求
- [ ] 后端日志：用户身份验证成功
- [ ] 后端日志：SSE 连接创建成功
- [ ] 踢出时：准备发送踢出通知
- [ ] 踢出时：用户 SSE 在线状态为 true
- [ ] 踢出时：已发送踢出通知
- [ ] 前端：收到 kick_out 事件
- [ ] 前端：收到 kick_out_update 事件
- [ ] 前端：收到 kick_out_final 事件

---

### 联系支持

如果以上步骤都无法解决问题，请提供：
1. 浏览器控制台的完整日志
2. 后端应用日志（从建立 SSE 连接到踢出用户）
3. Redis 中 `online_user:*` 的数据
4. 使用的浏览器类型和版本
