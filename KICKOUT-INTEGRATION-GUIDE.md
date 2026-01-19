# 踢出通知集成指南

## 快速开始（3步即可）

### 1. 在 HTML 页面中引入脚本

```html
<!-- 在 </body> 标签前引入 -->
<script src="/path/to/kickout-notification.js"></script>
```

### 2. 登录成功后初始化

```javascript
// 登录成功后调用
loginSuccess() {
  // ... 保存 token
  localStorage.setItem('satoken', token)

  // 初始化踢出通知管理器
  window.KickOutManager.init()
}
```

### 3. 完成！

当管理员踢出用户时，会自动显示弹出框和5秒倒计时。

---

## 详细使用说明

### 方式一：直接引入（最简单）

**1. 将 `kickout-notification.js` 文件放到项目的静态资源目录**

例如：
- `/src/assets/js/kickout-notification.js`
- `/public/js/kickout-notification.js`
- `/static/js/kickout-notification.js`

**2. 在 HTML 中引入**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>您的应用</title>
</head>
<body>
  <div id="app">
    <!-- 您的应用内容 -->
  </div>

  <!-- 引入踢出通知脚本 -->
  <script src="/js/kickout-notification.js"></script>

  <!-- 或使用 CDN -->
  <script>
    // 在登录成功后初始化
    function onLoginSuccess(token) {
      localStorage.setItem('satoken', token)
      window.KickOutManager.init()
    }
  </script>
</body>
</html>
```

**3. 登录成功时初始化**

```javascript
// 登录成功回调
function handleLoginSuccess(token) {
  localStorage.setItem('satoken', token)
  window.KickOutManager.init()  // 初始化踢出通知
}
```

---

### 方式二：Vue 3 集成

**1. 在 main.js 中引入**

```javascript
import { createApp } from 'vue'
import App from './App.vue'

// 引入踢出通知脚本
import '/assets/js/kickout-notification.js'

const app = createApp(App)
app.mount('#app')
```

**2. 在登录成功后初始化**

```javascript
// LoginView.vue
import { ref } from 'vue'
import { useRouter } from 'vue-router'

export default {
  setup() {
    const router = useRouter()

    const login = async (credentials) => {
      try {
        const response = await loginAPI(credentials)

        // 保存 token
        localStorage.setItem('satoken', response.data.token)

        // 初始化踢出通知
        window.KickOutManager.init()

        // 跳转到首页
        router.push('/')
      } catch (error) {
        console.error('登录失败:', error)
      }
    }

    return { login }
  }
}
```

**3. 在退出登录时清理**

```javascript
// LogoutView.vue 或退出方法
const logout = () => {
  // 清理踢出通知管理器
  window.KickOutManager.destroy()

  // 清除 token
  localStorage.removeItem('satoken')

  // 跳转到登录页
  router.push('/login')
}
```

---

### 方式三：React 集成

**1. 在 index.js 中引入**

```javascript
import React from 'react'
import ReactDOM from 'react-dom'
import App from './App'
import '/assets/js/kickout-notification.js'

ReactDOM.render(<App />, document.getElementById('root'))
```

**2. 在登录组件中使用**

```javascript
import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

function LoginPage() {
  const navigate = useNavigate()

  const handleLogin = async (credentials) => {
    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials)
      })

      const data = await response.json()

      // 保存 token
      localStorage.setItem('satoken', data.token)

      // 初始化踢出通知
      window.KickOutManager.init()

      // 跳转到首页
      navigate('/')
    } catch (error) {
      console.error('登录失败:', error)
    }
  }

  return (
    <form onSubmit={handleLogin}>
      {/* 登录表单 */}
    </form>
  )
}
```

**3. 在 App 组件中清理**

```javascript
import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

function App() {
  const navigate = useNavigate()

  useEffect(() => {
    // 检查是否有 token
    const token = localStorage.getItem('satoken')

    if (token) {
      // 初始化踢出通知
      window.KickOutManager.init()
    }
  }, [])

  const handleLogout = () => {
    // 清理踢出通知管理器
    window.KickOutManager.destroy()

    // 清除 token
    localStorage.removeItem('satoken')

    // 跳转到登录页
    navigate('/login')
  }

  return (
    <div>
      <button onClick={handleLogout}>退出登录</button>
    </div>
  )
}
```

---

### 方式四：Angular 集成

**1. 在 angular.json 中配置**

```json
{
  "projects": {
    "your-app": {
      "architect": {
        "build": {
          "options": {
            "scripts": [
              "src/assets/js/kickout-notification.js"
            ]
          }
        }
      }
    }
  }
}
```

**2. 在登录服务中初始化**

```typescript
// login.service.ts
import { Injectable } from '@angular/core'
import { Router } from '@angular/router'

declare const KickOutManager: any

@Injectable({
  providedIn: 'root'
})
export class LoginService {
  constructor(private router: Router) {}

  login(credentials: any) {
    return this.http.post('/api/auth/login', credentials).subscribe(
      (response: any) => {
        // 保存 token
        localStorage.setItem('satoken', response.token)

        // 初始化踢出通知
        if (typeof KickOutManager !== 'undefined') {
          KickOutManager.init()
        }

        // 跳转到首页
        this.router.navigate(['/'])
      },
      (error) => {
        console.error('登录失败:', error)
      }
    )
  }

  logout() {
    // 清理踢出通知管理器
    if (typeof KickOutManager !== 'undefined') {
      KickOutManager.destroy()
    }

    // 清除 token
    localStorage.removeItem('satoken')

    // 跳转到登录页
    this.router.navigate(['/login'])
  }
}
```

---

## API 说明

### KickOutManager.init()

初始化 SSE 连接和踢出通知管理器。

```javascript
window.KickOutManager.init()
```

**最佳时机**：用户登录成功后

---

### KickOutManager.destroy()

销毁管理器，关闭 SSE 连接，移除弹窗元素。

```javascript
window.KickOutManager.destroy()
```

**最佳时机**：用户主动退出登录时

---

### KickOutManager.disconnect()

只断开 SSE 连接，不销毁管理器。

```javascript
window.KickOutManager.disconnect()
```

**使用场景**：临时断开连接，稍后可以重新调用 `init()` 重连

---

### KickOutManager.isConnectionActive()

检查 SSE 连接是否处于活动状态。

```javascript
const isActive = window.KickOutManager.isConnectionActive()
console.log('连接状态:', isActive) // true 或 false
```

---

## 自定义配置

### 修改弹窗样式

可以通过修改 `kickout-notification.js` 中的样式来自定义弹窗外观：

```javascript
// 在 createDialog() 方法中修改样式
const dialogHTML = `
  <div style="
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); /* 修改渐变色 */
    padding: 40px;  /* 修改内边距 */
    border-radius: 20px;  /* 修改圆角 */
    /* ... 其他样式 */
  ">
  </div>
`
```

---

### 修改重连策略

```javascript
// 在构造函数中修改
constructor() {
  this.maxReconnectAttempts = 10  // 最大重连次数
  this.reconnectDelay = 5000      // 重连延迟（毫秒）
}
```

---

### 禁用提示音

```javascript
// 在 playNotificationSound() 方法中直接返回
playNotificationSound() {
  // 禁用提示音
  return
}
```

---

## 调试技巧

### 1. 查看控制台日志

所有关键操作都会输出日志，带有 `[KickOutManager]` 前缀：

```
[KickOutManager] 初始化踢出通知管理器
[KickOutManager] 正在建立 SSE 连接...
[KickOutManager] ✅ SSE 连接已建立
[KickOutManager] ⚠️ 收到踢出通知
[KickOutManager] 👋 倒计时结束，即将退出
```

### 2. 测试弹窗显示

```javascript
// 在浏览器控制台执行测试
window.KickOutManager.showDialog({
  message: '测试踢出通知',
  reason: '这是测试',
  countdown: 5
})
```

### 3. 检查连接状态

```javascript
console.log('连接状态:', window.KickOutManager.isConnectionActive())
```

---

## 常见问题

### Q1: 弹窗没有显示

**检查清单**：
- [ ] 浏览器控制台是否有错误
- [ ] 是否已登录（localStorage 中有 satoken）
- [ ] SSE 连接是否成功建立
- [ ] 管理员是否真的踢出了用户

**解决方案**：
```javascript
// 1. 检查 token
const token = localStorage.getItem('satoken')
console.log('Token:', token ? '✅' : '❌')

// 2. 手动初始化
if (token) {
  window.KickOutManager.init()
}

// 3. 测试弹窗
window.KickOutManager.showDialog({
  message: '测试消息',
  reason: '测试原因',
  countdown: 5
})
```

---

### Q2: SSE 连接断开

**可能原因**：
- 网络不稳定
- Token 过期
- 服务器重启

**解决方案**：
管理器会自动重连，无需手动处理。如果达到最大重连次数，请刷新页面或重新登录。

---

### Q3: 倒计时结束后没有退出

**检查**：
1. 浏览器控制台是否有 `logout()` 相关日志
2. 是否有其他代码阻止了页面跳转

**解决方案**：
```javascript
// 手动调用退出方法
window.KickOutManager.logout()
```

---

## 浏览器兼容性

- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+
- ⚠️ IE 不支持（SSE 和部分 ES6 特性）

---

## 完整示例

参考以下文件查看完整示例：
- [kickout-test.html](kickout-test.html) - 纯 HTML 示例
- [debug-sse.html](debug-sse.html) - 调试工具
- [sse-kickout-frontend.js](sse-kickout-frontend.js) - 多框架示例

---

## 技术支持

遇到问题？
1. 查看 [SSE-DEBUG-GUIDE.md](SSE-DEBUG-GUIDE.md) 排查指南
2. 使用 [debug-sse.html](debug-sse.html) 调试工具
3. 检查浏览器控制台日志
4. 检查后端日志
