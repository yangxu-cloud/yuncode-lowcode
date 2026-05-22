# 踢出通知功能 - 前端集成完成

## ✅ 已完成的集成

### 1. 创建的文件

#### [src/composables/useKickOutNotification.ts](yuncode-lowcode-admin/src/composables/useKickOutNotification.ts)
- SSE 连接管理的 Vue 3 Composable
- 自动处理连接、重连、断开
- 支持多种登录类型的 token 获取
- 完整的踢出通知处理逻辑

#### [src/components/KickOutDialog.vue](yuncode-lowcode-admin/src/components/KickOutDialog.vue)
- Element Plus 风格的踢出通知弹窗组件
- 大号倒计时显示
- 最后3秒黄色紧迫提示
- 流畅的动画效果

#### [src/App.vue](yuncode-lowcode-admin/src/App.vue)（已修改）
- 在应用根组件集成踢出通知
- 自动检测用户登录状态
- 登录后自动建立 SSE 连接

---

## 🎯 工作原理

### 用户登录后的流程

```
1. 用户登录成功
   ↓
2. token 保存到 localStorage (token_admin / token_tenant / token_user)
   ↓
3. App.vue 检测到登录状态
   ↓
4. 自动建立 SSE 连接到 /api/user/notifications
   ↓
5. 连接成功，等待服务器推送通知
```

### 管理员踢出用户时的流程

```
1. 管理员点击"踢出"按钮
   ↓
2. 后端发送 SSE 通知（5秒倒计时）
   ↓
3. 前端收到 kick_out 事件
   ↓
4. 显示 Element Plus 通知 + 弹窗
   ↓
5. 每秒更新倒计时（kick_out_update 事件）
   ↓
6. 倒计时结束（kick_out_final 事件）
   ↓
7. 自动退出登录，跳转到登录页
```

---

## 🔍 功能特性

### ✅ 已实现的功能

- ✅ **自动连接管理**
  - 登录后自动建立 SSE 连接
  - 连接断开自动重连（最多5次）
  - 退出登录自动断开连接

- ✅ **多用户类型支持**
  - 自动识别 admin / tenant / user 三种登录类型
  - 从正确的 localStorage key 获取 token
  - 支持 sessionStorage 的登录类型隔离

- ✅ **精美的弹窗界面**
  - Element Plus Dialog 组件
  - 渐变色倒计时背景
  - 警告图标抖动动画
  - 最后3秒黄色紧迫提示

- ✅ **双重通知**
  - Element Plus Notification（右上角）
  - 全屏弹窗对话框（居中）

- ✅ **完整的日志**
  - 所有操作都有控制台日志
  - 带有 `[SSE]` 前缀，方便过滤

---

## 🧪 测试方法

### 1. 启动前端项目

```bash
cd yuncode-lowcode-admin
npm run dev
```

### 2. 登录系统

使用任意一种登录方式（管理员/租户/普通用户）

### 3. 查看控制台

应该看到：
```
[App] 用户已登录，初始化 SSE 连接，登录类型: admin
[SSE] 正在建立连接...
[SSE] ✅ 连接已建立
```

### 4. 管理员踢出用户

- 使用另一个管理员账号登录
- 进入在线用户管理
- 找到刚才登录的用户
- 点击"踢出"按钮

### 5. 查看被踢出用户的效果

被踢出的用户应该看到：
- 右上角弹出 Element Plus 通知
- 屏幕中央显示踢出弹窗
- 倒计时从 5 开始递减
- 最后3秒数字变黄色
- 倒计时结束自动跳转到登录页

---

## 📋 代码示例

### 在登录成功后手动初始化（可选）

如果您的登录流程很复杂，可能需要手动初始化：

```typescript
import { useKickOutNotification } from '@/composables/useKickOutNotification'

// 在登录成功后
const { connectSSE } = useKickOutNotification()

const handleLoginSuccess = async () => {
  try {
    // ... 登录逻辑

    // 登录成功后，建立 SSE 连接
    connectSSE()
  } catch (error) {
    console.error('登录失败', error)
  }
}
```

### 在退出登录时断开连接（可选）

如果需要手动控制断开连接：

```typescript
import { useKickOutNotification } from '@/composables/useKickOutNotification'

const { disconnect } = useKickOutNotification()

const handleLogout = async () => {
  try {
    // 断开 SSE 连接
    disconnect()

    // ... 退出登录逻辑
  } catch (error) {
    console.error('退出失败', error)
  }
}
```

---

## 🐛 故障排查

### 问题1：没有建立 SSE 连接

**检查项**：
- [ ] 浏览器控制台是否有 `[SSE] 正在建立连接...` 日志
- [ ] localStorage 中是否有 `token_admin` / `token_tenant` / `token_user`
- [ ] sessionStorage 中是否有 `activeLoginType` 或 `loginType`

**解决方案**：
```javascript
// 在浏览器控制台执行
console.log('token_admin:', localStorage.getItem('token_admin'))
console.log('token_tenant:', localStorage.getItem('token_tenant'))
console.log('token_user:', localStorage.getItem('token_user'))
console.log('loginType:', sessionStorage.getItem('loginType'))
```

---

### 问题2：建立了连接但没有收到通知

**检查项**：
- [ ] 后端日志是否显示 `收到 SSE 连接请求`
- [ ] 后端日志是否显示 `用户 SSE 在线状态: userId=xxx, isOnline=true`
- [ ] userId 是否匹配

**解决方案**：
查看后端日志，确认：
1. SSE 连接是否成功建立
2. 踢出时是否找到正确的 SSE 连接
3. userId 是否一致

---

### 问题3：收到通知但没有显示弹窗

**检查项**：
- [ ] 浏览器控制台是否有 `[SSE] ⚠️ 收到踢出通知` 日志
- [ ] 是否有 JavaScript 错误
- [ ] Element Plus 是否正确加载

**解决方案**：
```javascript
// 在浏览器控制台执行
// 测试弹窗组件是否正常
import KickOutDialog from '@/components/KickOutDialog.vue'
```

---

## 📝 相关文件

### 后端文件

- [NotificationService.java](yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/service/NotificationService.java) - SSE 服务
- [NotificationController.java](yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/controller/NotificationController.java) - SSE 接口
- [OnlineUserServiceImpl.java](yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/service/impl/OnlineUserServiceImpl.java:177-192) - 踢出逻辑

### 前端文件

- [useKickOutNotification.ts](yuncode-lowcode-admin/src/composables/useKickOutNotification.ts) - SSE Composable
- [KickOutDialog.vue](yuncode-lowcode-admin/src/components/KickOutDialog.vue) - 弹窗组件
- [App.vue](yuncode-lowcode-admin/src/App.vue) - 根组件（已集成）

---

## 🎉 完成

现在踢出通知功能已完全集成到前端项目中！

用户登录后会自动建立 SSE 连接，当被管理员踢出时会：
1. 显示 Element Plus 通知
2. 显示全屏弹窗
3. 5秒倒计时
4. 自动退出登录

无需任何额外配置！
