# 多用户在线管理实现方案

## 📋 概述

本项目实现了完善的 **SaaS 多租户在线用户管理方案**，支持实时在线用户监控、统计、踢出等功能。

### 核心功能

✅ **在线用户管理**：实时监控所有在线用户（平台管理员、租户管理员、普通用户）
✅ **多租户隔离**：不同租户的在线用户数据完全隔离
✅ **实时统计**：在线总数、活跃用户、闲置用户统计
✅ **强制踢出**：管理员可强制踢出用户，带5秒倒计时通知
✅ **SSE 实时通知**：使用 Server-Sent Events 实现服务器主动推送
✅ **异步处理**：踢出操作异步执行，不阻塞管理员界面
✅ **自动清理**：自动清理已失效的在线用户记录

---

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                       前端 (Vue 3)                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 在线用户列表  │  │  在线统计    │  │  踢出操作    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┴──────────────────┘              │
│                            ↓                                 │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         useKickOutNotification (SSE 连接)             │  │
│  │   - 接收踢出通知                                        │  │
│  │   - 显示倒计时对话框                                    │  │
│  │   - 自动退出登录                                        │  │
│  └───────────────────────────────────────────────────────┘  │
└───────────────────────────┬─────────────────────────────────┘
                            │ SSE + HTTP
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      后端 (Spring Boot)                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         OnlineUserController (REST API)               │  │
│  │   - GET  /api/system/online-users/list               │  │
│  │   - GET  /api/system/online-users/stats              │  │
│  │   - POST /api/system/online-users/kick-out           │  │
│  │   - POST /api/system/online-users/batch-kick-out     │  │
│  └───────────────────┬──────────────────────────────────┘  │
│                      ↓                                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           OnlineUserServiceImpl                       │  │
│  │   - addOnlineUser()     添加在线用户                  │  │
│  │   - getAllOnlineUsers() 获取所有在线用户              │  │
│  │   - kickOutUser()       踢出用户（异步）               │  │
│  │   - getOnlineUserStats() 获取统计数据                 │  │
│  └───────────────────┬──────────────────────────────────┘  │
│                      ↓                                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           NotificationService (SSE 服务)              │  │
│  │   - createUserNotification()  创建 SSE 连接           │  │
│  │   - sendKickOutNotification() 发送踢出通知            │  │
│  │   - closeUserConnection()     关闭 SSE 连接           │  │
│  └──────────────────────────────────────────────────────┘  │
│                      ↓                                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Redis 存储层                              │  │
│  │   Key: online_user:{token}                            │  │
│  │   Value: OnlineUser 对象（JSON 序列化）                │  │
│  │   TTL: 7 天                                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 核心组件

### 1. 后端组件

#### 1.1 OnlineUser 实体类

**文件位置**: `yuncode-system/src/main/java/com/yuncode/system/entity/OnlineUser.java`

**字段说明**:

| 字段名 | 类型 | 说明 |
|--------|------|------|
| sessionId | String | 会话ID（Token值），唯一标识 |
| tenantId | Long | 租户ID，用于多租户隔离 |
| tenantName | String | 租户名称 |
| userId | Long | 用户ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| avatar | String | 头像URL |
| ip | String | IP地址 |
| location | String | 地理位置 |
| userAgent | String | 用户代理（浏览器信息） |
| loginTime | LocalDateTime | 登录时间 |
| lastAccessTime | LocalDateTime | 最后访问时间 |
| status | String | 状态：active-活跃，idle-闲置 |

#### 1.2 OnlineUserService 服务接口

**文件位置**: `yuncode-system/src/main/java/com/yuncode/system/service/OnlineUserService.java`

**核心方法**:

```java
// 添加在线用户（登录时调用）
void addOnlineUser(String token, OnlineUser onlineUser);

// 移除在线用户（退出登录时调用）
void removeOnlineUser(String token);

// 获取所有在线用户
List<OnlineUser> getAllOnlineUsers();

// 分页查询在线用户
Page<OnlineUser> listOnlineUsers(Page<OnlineUser> page, String username, Long tenantId);

// 踢出单个用户（异步执行）
void kickOutUser(String token);

// 批量踢出用户
void batchKickOutUsers(List<String> tokens);

// 获取在线用户统计
Map<String, Object> getOnlineUserStats();

// 更新最后访问时间
void updateLastAccessTime(String token);

// 清理闲置用户
void cleanIdleUsers(int idleMinutes);
```

#### 1.3 NotificationService (SSE 服务)

**文件位置**: `yuncode-system/src/main/java/com/yuncode/system/service/NotificationService.java`

**核心功能**:

```java
// 为用户创建 SSE 连接
SseEmitter createUserNotification(Long userId);

// 发送踢出通知（带倒计时）
void sendKickOutNotification(Long userId, String reason, int seconds);

// 发送普通通知消息
void sendMessage(Long userId, String message);

// 关闭指定用户的 SSE 连接
void closeUserConnection(Long userId);

// 获取当前在线 SSE 连接数
int getActiveConnections();

// 检查用户是否在线（有 SSE 连接）
boolean isUserOnline(Long userId);
```

**SSE 事件类型**:

| 事件名 | 说明 | 数据格式 |
|--------|------|----------|
| connected | 连接成功 | "SSE 连接已建立" |
| kick_out | 踢出通知（初始） | {message, countdown, reason} |
| kick_out_update | 倒计时更新 | {message, countdown, reason} |
| kick_out_final | 倒计时结束 | "倒计时结束，即将退出" |
| message | 普通消息 | 字符串 |

---

### 2. 前端组件

#### 2.1 在线用户列表页面

**文件位置**: `yuncode-lowcode-admin/src/views/system/OnlineUsers.vue`

**功能**:
- 展示所有在线用户列表
- 实时统计（在线总数、活跃、闲置）
- 支持按用户名搜索
- 支持单个踢出、批量踢出
- 踢出后5秒自动刷新列表

**关键代码**:

```typescript
// 踢出单个用户
const handleKickOut = async (user: OnlineUser) => {
    await ElMessageBox.confirm(`确认踢出用户: ${user.username}?`);
    loading.value = true;

    await kickOutUser(user.sessionId);
    ElMessage.success(`踢出成功，5秒后自动刷新列表`);

    // 5秒后刷新列表（等待后端异步踢出完成）
    setTimeout(() => {
        loadData();
        loadStats();
        loading.value = false;
    }, 5000);
};
```

#### 2.2 SSE 踢出通知管理

**文件位置**: `yuncode-lowcode-admin/src/composables/useKickOutNotification.ts`

**功能**:
- 建立 SSE 连接
- 接收踢出通知
- 显示倒计时对话框
- 倒计时结束后自动退出登录

**使用方式**:

```typescript
import { useKickOutNotification } from '@/composables/useKickOutNotification'

export default {
    setup() {
        const { connectSSE, disconnect, showKickOutDialog, countdown } = useKickOutNotification()

        // 登录成功后建立 SSE 连接
        onMounted(() => {
            connectSSE()
        })

        // 退出登录时断开连接
        onUnmounted(() => {
            disconnect()
        })

        return {
            showKickOutDialog,
            countdown
        }
    }
}
```

---

## 🔄 业务流程

### 流程1：用户登录 → 在线用户列表

```
┌─────────────┐
│  用户登录    │
└──────┬──────┘
       │
       ↓
┌─────────────────────────────────────┐
│  1. 登录成功后获取 Token             │
│  2. 构建 OnlineUser 对象            │
│  3. 调用 addOnlineUser()            │
│     - 保存到 Redis (7天过期)        │
│  4. 前端建立 SSE 连接               │
└─────────────────────────────────────┘
       │
       ↓
┌─────────────────────────────────────┐
│  在线用户列表显示该用户              │
│  - 用户名、昵称                     │
│  - 租户名称                         │
│  - IP地址、地理位置                 │
│  - 登录时间、最后访问时间           │
│  - 状态（活跃/闲置）                │
└─────────────────────────────────────┘
```

### 流程2：管理员踢出用户 → 5秒倒计时 → 强制退出

```
┌──────────────┐          ┌──────────────┐
│  管理员界面  │          │   被踢用户   │
└──────┬───────┘          └──────┬───────┘
       │                          │
       │ 1. 点击踢出按钮          │
       │    确认对话框            │
       │                          │
       ├──────────────────────────┤
       │                          │
       │ 2. 调用 kickOutUser() API │
       │                          │
       ↓                          ↓
┌────────────────────────────────────────┐
│           后端处理（异步）               │
│  ┌──────────────────────────────────┐ │
│  │ kickOutUser() 立即返回            │ │
│  └────────────┬─────────────────────┘ │
│               ↓                        │
│  ┌──────────────────────────────────┐ │
│  │ performKickOutAsync() 异步线程   │ │
│  │ 1. 发送 SSE 通知给被踢用户       │ │
│  │ 2. 等待 5 秒                     │ │
│  │ 3. 调用 StpUtil.kickout()        │ │
│  │ 4. 关闭 SSE 连接                 │ │
│  │ 5. 删除 Redis 在线记录           │ │
│  └──────────────────────────────────┘ │
└────────────────────────────────────────┘
       │                          │
       │ 3. 立即返回成功           │ 4. 收到 SSE 通知
       │    显示提示               │    显示倒计时对话框
       │    "踢出成功，5秒后        │    5, 4, 3, 2, 1...
       │     自动刷新列表"          │
       ↓                          ↓
┌──────────────┐          ┌──────────────┐
│ 5秒后刷新    │          │ 倒计时结束   │
│ 在线用户列表 │          │ 自动退出登录 │
│              │          │ 跳转登录页   │
└──────────────┘          └──────────────┘
```

### 流程3：用户正常退出登录

```
┌─────────────┐
│ 点击退出登录 │
└──────┬──────┘
       │
       ↓
┌─────────────────────────────────────┐
│  1. 关闭 SSE 连接                   │
│  2. 调用退出登录 API                │
│  3. Sa-Token 清除会话               │
│  4. 调用 removeOnlineUser()         │
│     - 从 Redis 删除在线记录         │
│  5. 清除本地 token 和用户信息       │
│  6. 跳转到登录页                    │
└─────────────────────────────────────┘
```

---

## 💡 使用示例

### 后端：登录时添加在线用户

```java
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private OnlineUserService onlineUserService;

    public LoginVO login(LoginDTO loginDTO) {
        // 1. 校验用户名密码
        User user = validateUser(loginDTO);

        // 2. 执行登录（Sa-Token）
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 3. 构建在线用户对象
        OnlineUser onlineUser = new OnlineUser();
        onlineUser.setUserId(user.getId());
        onlineUser.setUsername(user.getUsername());
        onlineUser.setNickname(user.getNickname());
        onlineUser.setTenantId(user.getTenantId());
        onlineUser.setTenantName(user.getTenantName());
        onlineUser.setIp(RequestUtil.getClientIP());
        onlineUser.setLocation(RequestUtil.getIpLocation(RequestUtil.getClientIP()));
        onlineUser.setUserAgent(RequestUtil.getUserAgent());

        // 4. 添加到在线用户列表
        onlineUserService.addOnlineUser(token, onlineUser);

        // 5. 返回登录结果
        return new LoginVO(token, user);
    }
}
```

### 后端：退出登录时移除在线用户

```java
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private OnlineUserService onlineUserService;

    public void logout() {
        // 1. 获取当前 token
        String token = StpUtil.getTokenValue();

        // 2. 移除在线用户记录
        onlineUserService.removeOnlineUser(token);

        // 3. 关闭 SSE 连接
        Long userId = StpUtil.getLoginIdAsLong();
        notificationService.closeUserConnection(userId);

        // 4. Sa-Token 退出登录
        StpUtil.logout();
    }
}
```

### 后端：踢出用户

```java
@RestController
@RequestMapping("/api/system/online-users")
public class OnlineUserController {

    @Autowired
    private OnlineUserService onlineUserService;

    /**
     * 踢出单个用户
     */
    @PostMapping("/kick-out")
    public Result<Void> kickOut(@RequestBody String token) {
        onlineUserService.kickOutUser(token);
        return Result.success();
    }

    /**
     * 批量踢出用户
     */
    @PostMapping("/batch-kick-out")
    public Result<Void> batchKickOut(@RequestBody List<String> tokens) {
        onlineUserService.batchKickOutUsers(tokens);
        return Result.success();
    }

    /**
     * 分页查询在线用户
     */
    @GetMapping("/list")
    public Result<Page<OnlineUser>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username) {

        Page<OnlineUser> pageParam = new Page<>(page, size);
        Page<OnlineUser> result = onlineUserService.listOnlineUsers(pageParam, username, null);
        return Result.success(result);
    }

    /**
     * 获取在线用户统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> stats = onlineUserService.getOnlineUserStats();
        return Result.success(stats);
    }
}
```

### 前端：在主布局中建立 SSE 连接

```vue
<template>
  <div>
    <router-view />
    <KickOutDialog
      v-model:visible="showKickOutDialog"
      :countdown="countdown"
      @logout="handleLogoutNow"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useKickOutNotification } from '@/composables/useKickOutNotification'

const {
  connectSSE,
  disconnect,
  showKickOutDialog,
  countdown,
  handleLogoutNow
} = useKickOutNotification()

onMounted(() => {
  // 登录成功后建立 SSE 连接
  connectSSE()
})

onUnmounted(() => {
  // 组件卸载时断开连接
  disconnect()
})
</script>
```

---

## 🔧 配置说明

### 后端配置

**application.yml**:

```yaml
# Sa-Token 配置
sa-token:
  token-name: token  # Token 参数名称
  timeout: 2592000   # Token 有效期（秒），默认 30 天
  is-concurrent: true  # 允同一账号并发登录
  is-share: false      # 每次登录新建一个 token

# Redis 配置
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### 前端配置

**.env.development**:

```env
# Token 参数名称（必须与后端一致）
VITE_TOKEN_NAME=token
```

**src/config/index.ts**:

```typescript
export const apiConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  tokenName: import.meta.env.VITE_TOKEN_NAME || "token",
  timeout: 10000
}
```

---

## 📊 数据结构

### Redis 存储结构

```
Key: online_user:{token}
Type: String (JSON 序列化)
TTL: 7 天

Value 示例:
{
  "sessionId": "abc123...",
  "tenantId": 1,
  "tenantName": "租户A",
  "userId": 1001,
  "username": "zhangsan",
  "nickname": "张三",
  "avatar": "https://...",
  "ip": "192.168.1.100",
  "location": "北京市",
  "userAgent": "Mozilla/5.0...",
  "loginTime": "2024-01-19T10:30:00",
  "lastAccessTime": "2024-01-19T11:30:00",
  "status": "active"
}
```

### SSE 通知数据格式

**踢出通知**:

```json
{
  "message": "您已被管理员强制下线",
  "countdown": 5,
  "reason": "被管理员踢出"
}
```

**倒计时更新**:

```json
{
  "message": "您已被管理员强制下线",
  "countdown": 4,
  "reason": "被管理员踢出"
}
```

---

## ⚠️ 注意事项

### 1. Token 配置一致性

**⚠️ 重要**：前后端的 Token 参数名称必须保持一致！

- 后端: `application.yml` 中的 `sa-token.token-name`
- 前端: `.env` 文件中的 `VITE_TOKEN_NAME`

如果不一致，SSE 连接会失败，无法接收踢出通知。

### 2. 异步踢出处理

踢出操作是异步执行的：
- **管理员视角**：点击踢出后立即返回成功，5秒后自动刷新列表
- **被踢用户**：收到 SSE 通知，显示5秒倒计时，倒计时结束后强制退出

这样可以避免阻塞管理员界面，提升用户体验。

### 3. 在线用户记录清理

系统会自动清理已失效的在线用户记录：
- 用户正常退出登录时删除
- 踢出用户时删除
- Token 失效时自动清理（通过 `getAllOnlineUsers()` 验证）

### 4. 多租户隔离

不同租户的在线用户数据完全隔离：
- 平台管理员可以看到所有租户的在线用户
- 租户管理员只能看到自己租户的在线用户（需要在查询时过滤 `tenantId`）

### 5. SSE 连接管理

- 每个用户只能有一个有效的 SSE 连接
- 新连接会自动替换旧连接
- 连接超时时间为 30 分钟
- 用户退出登录时会自动关闭连接

---

## 🚀 扩展建议

### 1. 添加更多通知类型

可以在 `NotificationService` 中添加更多通知方法：

```java
// 发送系统公告
public void sendSystemAnnouncement(Long userId, String announcement);

// 发送新消息提醒
public void sendNewMessageNotification(Long userId, String message);

// 发送会话过期提醒
public void sendSessionExpiryWarning(Long userId, int minutes);
```

### 2. 添加在线用户操作日志

记录管理员对在线用户的操作：

```java
@Service
public class OnlineUserOperationLogService {

    public void logKickOut(Long operatorId, Long kickedUserId, String reason) {
        // 保存操作日志
    }

    public void logView(Long operatorId, Long viewedUserId) {
        // 保存查看日志
    }
}
```

### 3. 添加用户行为分析

记录用户的活跃度、访问路径等：

```java
@Data
public class UserActivity {
    private Long userId;
    private String uri;         // 访问路径
    private LocalDateTime timestamp;
    private String duration;    // 停留时长
}
```

### 4. 添加异地登录提醒

检测用户IP变化，提醒用户：

```java
public void checkIpChange(String token, OnlineUser onlineUser) {
    OnlineUser lastLogin = getLastLogin(onlineUser.getUserId());
    if (lastLogin != null && !lastLogin.getIp().equals(onlineUser.getIp())) {
        // IP发生变化，发送通知
        notificationService.sendMessage(
            onlineUser.getUserId(),
            "您的账号在异地登录，如非本人操作请修改密码"
        );
    }
}
```

---

## 📚 相关文档

- [统一异常处理使用指南](EXCEPTION_HANDLING_GUIDE.md)
- [Token 配置说明](TOKEN_CONFIG.md)
- [Sa-Token 官方文档](https://sa-token.cc/)

---

## 🎯 总结

本方案提供了完善的 **SaaS 多租户在线用户管理** 功能：

✅ **实时监控**：通过 Redis + SSE 实现在线用户实时监控
✅ **多租户隔离**：支持多租户数据隔离
✅ **异步处理**：踢出操作异步执行，不阻塞界面
✅ **用户友好**：5秒倒计时通知，用户体验良好
✅ **自动清理**：自动清理失效的在线用户记录
✅ **易于扩展**：清晰的代码结构，易于添加新功能

希望这份文档能帮助团队快速理解和使用多用户在线管理功能！🚀
