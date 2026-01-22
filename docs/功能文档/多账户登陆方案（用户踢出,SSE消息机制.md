# 多账户登录方案（用户踢出、SSE消息机制）

## 📋 目录

- [方案概述](#方案概述)
- [架构设计](#架构设计)
- [核心概念](#核心概念)
- [技术实现](#技术实现)
- [数据流程](#数据流程)
- [使用示例](#使用示例)
- [问题排查](#问题排查)

---

## 方案概述

本方案实现了**完整的多用户、多会话登录管理**，支持：
- ✅ 同一账号多设备同时登录
- ✅ 同一账号多标签页同时登录
- ✅ 精确踢出指定会话（不影响其他会话）
- ✅ SSE 实时推送踢出通知
- ✅ 倒计时自动退出
- ✅ Redis 存储在线用户状态
- ✅ 完全的会话隔离机制

---

## 架构设计

### 核心原理

```
┌─────────────────────────────────────────────────────────────┐
│                      前端架构                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  浏览器标签页 A                    浏览器标签页 B              │
│  ┌──────────────┐                ┌──────────────┐           │
│  │ Cookie       │                │ Cookie       │           │
│  │ session-xxx  │                │ session-yyy  │           │
│  └──────┬───────┘                └──────┬───────┘           │
│         │                              │                     │
│  ┌──────▼───────┐                ┌──────▼───────┐           │
│  │sessionStorage│                │sessionStorage│           │
│  │token: token1 │                │token: token2 │           │
│  │sessionId: xxx│                │sessionId: yyy│           │
│  └──────┬───────┘                └──────┬───────┘           │
│         │                              │                     │
│  ┌──────▼──────────────────────────────▼───────┐        │
│  │             SSE 连接                       │        │
│  │  /api/user/notifications?token=...&sessionId=xxx│        │
│  └──────────────────────────────────────────────┘        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      后端架构                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Sa-Token 认证层                       │   │
│  │  • 生成 JWT Token（用于 API 认证）                   │   │
│  │  • 管理用户 Session                                 │   │
│  │  • 存储 sessionId 到 Session                          │   │
│  └──────────────────────────────────────────────────────┘   │
│                           │                                  │
│  ┌────────────────▼───────────────────────────────────┐   │
│  │           Redis 存储层                              │   │
│  │  ┌──────────────────────────────────────────────┐ │   │
│  │  │ online_user:{sessionId}                       │ │   │
│  │  │ {                                             │ │   │
│  │  │   "sessionId": "xxx",                         │ │   │
│  │  │   "token": "jwt-token",                       │ │   │
│  │  │   "userId": 1,                                │ │   │
│  │  │   "username": "admin",                         │ │   │
│  │  │   "status": "active"                           │ │   │
│  │  │ }                                             │ │   │
│  │  └──────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────┘   │
│                           │                                  │
│  ┌────────────────▼───────────────────────────────────┐   │
│  │           SSE 连接管理                              │   │
│  │  • Map<String, SseEmitter>                        │   │
│  │  • Key: userId-sessionId                           │   │
│  │  • Value: SseEmitter                               │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 核心概念

### 1. sessionId vs Token

**重要区分：**

| 概念 | sessionId | Token |
|------|----------|-------|
| **类型** | UUID（32字符无特殊字符） | Sa-Token JWT（200+字符） |
| **用途** | 业务会话标识 | API 认证凭证 |
| **存储位置** | Redis key、Cookie key | Sa-Token、HTTP Header |
| **示例** | `3fb093fe08fd40c18cb4fff1453ec41d` | `eyJ0eXAiOiJKV1QiLCJhbGc...` |
| **生命周期** | 退出/踢出时删除 | Sa-Token 过期时间 |

**为什么需要分离？**
1. **Token 有特殊字符**（`.`、`-`、`_`），不适合作为 Redis/Cookie key
2. **Token 太长**（200+字符），影响性能和可读性
3. **需要会话隔离**：同一用户多设备登录，每个会话独立管理

### 2. 存储结构

#### Redis 存储
```
Key: online_user:3fb093fe08fd40c18cb4fff1453ec41d
Value: {
  "sessionId": "3fb093fe08fd40c18cb4fff1453ec41d",
  "token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "userId": 1,
  "username": "admin",
  "nickname": "管理员",
  "avatar": "",
  "tenantId": 1,
  "tenantName": "默认租户",
  "ip": "127.0.0.1",
  "location": "",
  "userAgent": "Mozilla/5.0...",
  "loginTime": "2026-01-22T00:45:28",
  "lastAccessTime": "2026-01-22T00:45:28",
  "status": "active"
}
TTL: 7 天
```

#### Cookie 存储
```
Key: session-3fb093fe08fd40c18cb4fff1453ec41d
Value: {
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "sessionId": "3fb093fe08fd40c18cb4fff1453ec41d",
  "expires": 1769132800000,
  "refreshToken": "gr5w...",
  "userId": 1,
  "tenantId": 1
}
```

#### sessionStorage 存储
```
current-token: "eyJ0eXAiOiJKV1QiLCJhbGc..."
current-sessionId: "3fb093fe08fd40c18cb4fff1453ec41d"
userInfo-admin: {
  "userId": 1,
  "username": "admin",
  "nickname": "管理员",
  "sessionId": "3fb093fe08fd40c18cb4fff1453ec41d",
  "loginType": "admin",
  ...
}
```

#### SSE 连接存储（内存）
```
Key: 1-3fb093fe08fd40c18cb4fff1453ec41d
Value: SseEmitter 实例
```

---

## 技术实现

### 后端实现

#### 1. 实体类设计

**OnlineUser.java**
```java
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnlineUser implements Serializable {
    /**
     * 会话ID（业务会话标识，UUID格式）
     * 用于 Redis key 和前端 Cookie key
     * 与 Sa-Token 的 token 是两个不同的概念
     */
    private String sessionId;

    /**
     * Sa-Token 的 JWT Token
     * 用于 Sa-Token 认证，通过 StpUtil.kickoutByTokenValue() 踢出
     */
    private String token;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 登录IP地址
     */
    private String ip;

    /**
     * 登录位置
     */
    private String location;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 状态：active（活跃）、idle（闲置）
     */
    private String status;

    // 更新最后访问时间
    public void updateLastAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
        this.status = "active";
    }
}
```

#### 2. 登录服务实现

**AdminLoginService.java**
```java
@Service
public class AdminLoginService {

    @LoginLog(loginType = "admin")
    public LoginVO login(LoginDTO loginDTO, HttpServletRequest request) {
        // 1. 校验租户编码
        // 2. 查询租户
        // 3. 校验租户状态
        // 4. 查询用户
        // 5. 校验用户状态
        // 6. 校验密码

        // 7. 使用 Sa-Token 进行登录
        StpUtil.login(user.getId());

        // 8. 生成业务会话ID（UUID，用于 Redis key 和前端 Cookie）
        String sessionId = java.util.UUID.randomUUID().toString().replace("-", "");
        log.info("生成业务会话ID: userId={}, sessionId={}", user.getId(), sessionId);

        // 将 sessionId 存入 Sa-Token session，供退出时使用
        StpUtil.getSession().set("sessionId", sessionId);
        log.info("sessionId 已存入 Sa-Token session，验证: {}", StpUtil.getSession().get("sessionId"));

        // 9. 获取 Sa-Token 的 JWT Token
        String token = StpUtil.getTokenValue();

        // 10. 添加在线用户记录到 Redis
        OnlineUser onlineUser = new OnlineUser();
        onlineUser.setSessionId(sessionId);  // 业务会话ID
        onlineUser.setToken(token);          // Sa-Token JWT
        onlineUser.setUserId(user.getId());
        onlineUser.setUsername(username);
        onlineUser.setNickname(user.getNickname());
        onlineUser.setAvatar(user.getAvatar());
        onlineUser.setTenantId(tenantId);
        onlineUser.setTenantName(tenant.getTenantName());
        onlineUser.setIp(getClientIP(request));
        onlineUser.setLocation("");
        onlineUser.setUserAgent(request.getHeader("User-Agent"));

        onlineUserService.addOnlineUser(sessionId, onlineUser);

        // 11. 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setSessionId(sessionId);  // 返回业务会话ID给前端
        loginVO.setTokenName(saTokenProperties.getTokenName());
        loginVO.setUserId(StpUtil.getLoginIdAsLong());
        loginVO.setUsername(username);
        loginVO.setNickname(StpUtil.getSession().get("nickname", ""));
        loginVO.setAvatar(StpUtil.getSession().get("avatar", ""));
        loginVO.setTenantId(tenantId);

        if (tenant != null) {
            loginVO.setTenantName(tenant.getTenantName());
        }

        return loginVO;
    }
}
```

**关键点：**
- 登录时生成唯一的 `sessionId`（UUID）
- 将 `sessionId` 存入 Sa-Token session，供退出时使用
- 返回给前端：`token`（用于 API 认证）+ `sessionId`（用于 SSE 连接）

#### 3. 在线用户服务

**OnlineUserServiceImpl.java**
```java
@Slf4j
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final String ONLINE_USER_KEY_PREFIX = "online_user:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void addOnlineUser(String sessionId, OnlineUser onlineUser) {
        onlineUser.setLoginTime(LocalDateTime.now());
        onlineUser.setLastAccessTime(LocalDateTime.now());
        onlineUser.setStatus("active");
        onlineUser.setSessionId(sessionId);

        // 存储到 Redis，7天过期
        String key = ONLINE_USER_KEY_PREFIX + sessionId;
        log.info("[Redis] 添加在线用户记录，key={}, sessionId={}, userId={}, username={}",
                key, sessionId, onlineUser.getUserId(), onlineUser.getUsername());
        redisTemplate.opsForValue().set(key, onlineUser, 7, TimeUnit.DAYS);
        log.info("[Redis] 在线用户记录已添加，key={}", key);
    }

    @Override
    public void removeOnlineUser(String sessionId) {
        String key = ONLINE_USER_KEY_PREFIX + sessionId;
        log.info("[Redis] 准备删除在线用户记录，key={}, sessionId={}", key, sessionId);
        Boolean deleted = redisTemplate.delete(key);
        log.info("[Redis] 删除结果: {}, key={}, sessionId={}", deleted, key, sessionId);
    }

    @Override
    public void kickOutUser(String sessionId) {
        log.info("开始踢出用户，sessionId: {}", sessionId);

        // 获取被踢出用户的信息
        OnlineUser kickedUser = getOnlineUser(sessionId);
        if (kickedUser == null) {
            log.warn("未找到在线用户记录，sessionId: {}", sessionId);
            return;
        }

        String kickedUsername = kickedUser.getUsername();
        Long kickedUserId = kickedUser.getUserId();
        String token = kickedUser.getToken();  // Sa-Token JWT

        // 发送 SSE 踢出通知
        notificationService.sendKickOutNotification(
            kickedUserId,
            sessionId,
            "被管理员踢出",
            5  // 5秒倒计时
        );

        // 等待倒计时结束后执行踢出
        // 注意：实际踢出由前端倒计时结束后调用退出接口完成
        log.info("踢出指令已发送，sessionId: {}, username={}", sessionId, kickedUsername);
    }

    @Override
    public List<OnlineUser> getAllOnlineUsers() {
        Set<String> keys = redisTemplate.keys(ONLINE_USER_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        List<OnlineUser> onlineUsers = new ArrayList<>();

        for (String key : keys) {
            try {
                OnlineUser user = (OnlineUser) redisTemplate.opsForValue().get(key);
                if (user != null && user.getSessionId() != null && user.getToken() != null) {
                    // 验证 token 是否仍然有效
                    try {
                        Object loginId = StpUtil.getLoginIdByToken(user.getToken());
                        if (loginId != null) {
                            onlineUsers.add(user);
                        } else {
                            // Token 已失效，清理 Redis 中的过期数据
                            redisTemplate.delete(key);
                        }
                    } catch (Exception e) {
                        // Token 验证失败，清理 Redis 中的过期数据
                        redisTemplate.delete(key);
                    }
                }
            } catch (Exception e) {
                log.error("处理在线用户记录失败: key={}, error={}", key, e.getMessage());
            }
        }

        return onlineUsers;
    }
}
```

**关键点：**
- Redis key 格式：`online_user:{sessionId}`
- 自动清理失效 token（通过 `StpUtil.getLoginIdByToken()` 验证）
- 踢出时发送 SSE 通知，而不是立即删除 Redis

#### 4. SSE 通知服务

**NotificationService.java**
```java
@Slf4j
@Service
public class NotificationService {

    /**
     * 存储用户的 SSE 连接
     * Key: userId-sessionId, Value: SseEmitter
     * 使用 userId+sessionId 作为 key，支持同一用户多个会话同时在线
     */
    private final Map<String, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    /**
     * 定时任务线程池，用于发送倒计时更新
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public SseEmitter createUserNotification(Long userId, String sessionId) {
        // 30分钟超时
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 使用 userId+sessionId 作为 key，支持同一用户多个会话同时在线
        String userKey = userId + "-" + sessionId;
        userEmitters.put(userKey, emitter);

        // 设置回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成: userId={}, sessionId={}", userId, sessionId);
            userEmitters.remove(userKey);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 连接超时: userId={}, sessionId={}", userId, sessionId);
            userEmitters.remove(userKey);
            emitter.complete();
        });

        emitter.onError((ex) -> {
            log.error("SSE 连接错误: userId={}, sessionId={}", userId, sessionId, ex);
            userEmitters.remove(userKey);
        });

        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 连接已建立"));
        } catch (IOException e) {
            log.error("发送连接成功消息失败: userId={}, sessionId={}", userId, sessionId, e);
        }

        log.info("创建 SSE 连接成功: userId={}, sessionId={}, 当前连接数={}",
                userId, sessionId, userEmitters.size());
        return emitter;
    }

    /**
     * 发送踢出通知（带倒计时）
     */
    public void sendKickOutNotification(Long userId, String sessionId, String reason, int seconds) {
        // 使用 userId+sessionId 作为 key，精确发送通知到指定会话
        String userKey = userId + "-" + sessionId;
        SseEmitter emitter = userEmitters.get(userKey);

        if (emitter == null) {
            log.warn("用户 SSE 连接不存在，无法发送踢出通知: userId={}, sessionId={}", userId, sessionId);
            return;
        }

        log.info("开始发送踢出通知: userId={}, sessionId={}, reason={}, countdown={}秒",
                userId, sessionId, reason, seconds);

        // 发送初始踢出通知
        KickOutNotification notification = new KickOutNotification(
                "您已被管理员强制下线",
                seconds,
                reason
        );

        try {
            emitter.send(SseEmitter.event()
                    .name("kick_out")
                    .data(notification));
            log.info("踢出通知已发送: userId={}, sessionId={}, countdown={}秒", userId, sessionId, seconds);
        } catch (IOException e) {
            log.error("发送踢出通知失败: userId={}, sessionId={}", userId, sessionId, e);
            return;
        }

        // 启动倒计时任务，每秒更新一次
        final int[] countdown = {seconds};
        scheduler.scheduleAtFixedRate(() -> {
            countdown[0]--;

            if (countdown[0] <= 0) {
                // 倒计时结束，发送最后一条消息
                try {
                    emitter.send(SseEmitter.event()
                            .name("kick_out_final")
                            .data("倒计时结束，即将退出"));
                } catch (IOException e) {
                    log.error("发送倒计时结束消息失败: userId={}, sessionId={}", userId, sessionId, e);
                }
                // 完成连接
                emitter.complete();
                log.info("踢出倒计时结束，连接已关闭: userId={}, sessionId={}", userId, sessionId);
            } else if (countdown[0] <= 5) {
                // 最后5秒，每秒发送一次更新
                try {
                    KickOutNotification update = new KickOutNotification(
                            "您已被管理员强制下线",
                            countdown[0],
                            reason
                    );
                    emitter.send(SseEmitter.event()
                            .name("kick_out_update")
                            .data(update));
                } catch (IOException e) {
                    log.error("发送倒计时更新失败: userId={}, sessionId={}, countdown={}",
                            userId, sessionId, countdown[0], e);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 关闭指定用户的 SSE 连接
     */
    public void closeUserConnection(Long userId, String sessionId) {
        String userKey = userId + "-" + sessionId;
        SseEmitter emitter = userEmitters.remove(userKey);

        if (emitter != null) {
            try {
                emitter.complete();
                log.info("SSE 连接已关闭: userId={}, sessionId={}", userId, sessionId);
            } catch (Exception e) {
                log.error("关闭 SSE 连接失败: userId={}, sessionId={}", userId, sessionId, e);
            }
        }
    }
}
```

**关键点：**
- SSE 连接 key：`userId-sessionId`（支持多会话）
- 倒计时机制：5秒倒计时，最后3秒每秒更新
- 精确踢出：只踢出指定 sessionId 的会话

#### 5. SSE 控制器

**NotificationController.java**
```java
@RestController
@RequestMapping("/user/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 建立 SSE 连接
     * 前端调用此接口后，会保持长连接，可以接收服务器推送的实时通知
     *
     * 注意：EventSource 不支持自定义请求头，所以 token 通过 URL 参数传递
     * 同时传递 sessionId 用于标识业务会话
     */
    @GetMapping(produces = "text/event-stream")
    public SseEmitter connect(
            @RequestParam(value = "token") String token,
            @RequestParam(value = "sessionId") String sessionId) {
        log.info("========================================");
        log.info("收到 SSE 连接请求");

        // 验证 token
        if (token == null || token.isEmpty()) {
            log.warn("❌ 未提供 token");
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("❌ 未提供 sessionId");
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        try {
            // 使用 Sa-Token 验证 token 并获取登录 ID
            Object loginId = StpUtil.getLoginIdByToken(token);

            if (loginId == null) {
                log.warn("❌ token 无效或已过期");
                throw new BaseException(ErrorCode.UNAUTHORIZED);
            }

            Long userId = Long.valueOf(loginId.toString());

            log.info("✅ 用户身份验证成功: userId={}, sessionId={}", userId, sessionId);
            log.info("当前 SSE 连接数: {}", notificationService.getActiveConnections());

            SseEmitter emitter = notificationService.createUserNotification(userId, sessionId);

            log.info("✅ SSE 连接创建成功: userId={}, sessionId={}, 当前连接数={}",
                    userId, sessionId, notificationService.getActiveConnections());
            log.info("========================================");

            return emitter;

        } catch (Exception e) {
            log.error("❌ SSE 连接失败", e);
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
    }
}
```

#### 6. 退出登录服务

**AuthService.java**
```java
public void logout() {
    StpLogic stpLogic = getCurrentStpLogic();

    String sessionId = null;
    String token = null;
    try {
        token = stpLogic.getTokenValue();
        // 从 Sa-Token session 中获取业务会话ID
        sessionId = stpLogic.getSession().get("sessionId", "");
        log.info("[Logout] 开始登出流程，sessionId={}, token (前32位): {}",
            sessionId, token != null ? token.substring(0, Math.min(32, token.length())) : "null");
    } catch (Exception e) {
        log.warn("[Logout] 获取 token 或 sessionId 失败: {}", e.getMessage());
    }

    try {
        // 获取当前用户信息
        Long userId = stpLogic.getLoginIdAsLong();
        String username = stpLogic.getSession().get("username", "");

        // 更新登录日志的登出时间
        loginLogService.updateLogoutTime(username, LocalDateTime.now());

        log.info("[Logout] 用户登出成功: userId={}, username={}, loginType={}",
                userId, username, stpLogic.getLoginType());

    } catch (Exception e) {
        log.error("[Logout] 更新登出时间失败", e);
    } finally {
        // 移除在线用户记录
        try {
            if (sessionId != null && !sessionId.isEmpty()) {
                log.info("[Logout] 准备移除在线用户记录，sessionId={}", sessionId);
                onlineUserService.removeOnlineUser(sessionId);
                log.info("[Logout] 已移除在线用户记录，sessionId={}", sessionId);
            } else {
                log.warn("[Logout] sessionId 为空，无法移除在线用户记录");
            }
        } catch (Exception e) {
            log.error("[Logout] 移除在线用户记录失败: {}", e.getMessage(), e);
        }

        // 执行登出操作
        try {
            stpLogic.logout();
            log.info("[Logout] StpLogic.logout() 执行成功");
        } catch (Exception e) {
            log.debug("[Logout] 执行 logout 失败: {}", e.getMessage());
        }
    }

    log.info("[Logout] 登出流程完成");
}
```

**关键点：**
- 先从 session 获取 `sessionId`
- 然后删除 Redis 中的在线用户记录
- 最后调用 `StpUtil.logout()` 清除 Sa-Token session
- 顺序很重要：先删除业务数据，再清除认证数据

### 前端实现

#### 1. 认证工具类

**auth.ts**
```typescript
export interface DataInfo<T> {
  /** token (后端 Sa-Token 生成的 JWT Token，用于 API 认证) */
  accessToken: string;
  /** sessionId (业务会话标识，UUID 格式，用于前端 Cookie 和 SSE 连接) */
  sessionId: string;
  /** `accessToken`的过期时间（时间戳） */
  expires: T;
  /** 用于调用刷新accessToken的接口时所需的token */
  refreshToken: string;
  /** 用户ID */
  userId?: number;
  /** 租户ID */
  tenantId?: number;
  /** 登录类型（用于区分不同登录方式：admin/user/tenant） */
  loginType?: string;
  // ... 其他字段
}

/** Cookie key 前缀 */
export const CURRENT_TOKEN_KEY = "current-token";
export const CURRENT_SESSION_KEY = "current-sessionId";

/**
 * 获取 Cookie key（根据后端返回的 sessionId）
 */
export function getSessionKey(sessionId: string): string {
  return `session-${sessionId}`;
}

/**
 * 设置 token 和用户信息
 */
export function setToken(data: DataInfo<Date>) {
  const token = data.accessToken;
  const sessionId = data.sessionId;
  const userId = data.userId;
  const tenantId = data.tenantId || 0;
  const loginType = data.loginType || "admin";

  console.log("[Auth] 设置 Token，sessionId:", sessionId, "token (前32位):", token.substring(0, 32), "userId:", userId, "tenantId:", tenantId, "loginType:", loginType);

  // Cookie 数据包含 sessionId
  const cookieString = JSON.stringify({
    accessToken: token,
    sessionId: sessionId,  // 重要！
    expires,
    refreshToken,
    userId,
    tenantId
  });
  const sessionKey = getSessionKey(sessionId);

  // 存储 Cookie（按 sessionId 区分 key，确保每个会话独立）
  Cookies.set(sessionKey, cookieString);

  // 保存当前 token 和 sessionId 到 sessionStorage
  sessionStorage.setItem(CURRENT_TOKEN_KEY, token);
  sessionStorage.setItem(CURRENT_SESSION_KEY, sessionId);

  // 用户信息存到 sessionStorage（包含 sessionId）
  const userInfo = {
    userId,
    tenantId,
    refreshToken,
    expires,
    avatar: data?.avatar ?? "",
    username: data?.username ?? "",
    nickname: data?.nickname ?? "",
    roles: data?.roles ?? [],
    permissions: data?.permissions ?? [],
    loginType,
    sessionId  // 重要！
  };

  sessionStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo));
}

/**
 * 获取 token
 */
export function getToken(): DataInfo<number> {
  // 从 sessionStorage 获取当前 token 和 sessionId
  const token = sessionStorage.getItem(CURRENT_TOKEN_KEY);
  const sessionId = sessionStorage.getItem(CURRENT_SESSION_KEY);

  if (token && sessionId) {
    const sessionKey = getSessionKey(sessionId);

    // 优先从 Cookie 获取
    const cookieData = Cookies.get(sessionKey);
    if (cookieData) {
      return JSON.parse(cookieData);
    }

    // 其次从 sessionStorage 获取用户信息
    const userInfoStr = sessionStorage.getItem(USER_INFO_KEY);
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr);
      userInfo.accessToken = token;
      return userInfo;
    }
  }

  return null;
}

/**
 * 移除 token
 */
export function removeToken() {
  const sessionId = sessionStorage.getItem(CURRENT_SESSION_KEY);

  if (sessionId) {
    const sessionKey = getSessionKey(sessionId);
    Cookies.remove(sessionKey);

    console.log("[Auth] removeToken() - 清除 sessionId:", sessionId);
  }

  sessionStorage.removeItem(CURRENT_TOKEN_KEY);
  sessionStorage.removeItem(CURRENT_SESSION_KEY);
  sessionStorage.removeItem(USER_INFO_KEY);
}
```

**关键点：**
- Cookie key 格式：`session-{sessionId}`
- Cookie 数据包含 `sessionId` 字段
- sessionStorage 存储 `current-token` 和 `current-sessionId`

#### 2. SSE 连接管理

**useKickOutNotification.ts**
```typescript
export function useKickOutNotification() {
  const userStore = useUserStoreHook()
  let eventSource: EventSource | null = null

  // 倒计时状态
  const countdown = ref(5)
  const showKickOutDialog = ref(false)
  const kickOutData = ref({
    message: '',
    reason: '',
    countdown: 5
  })

  /**
   * 获取当前登录用户的 token 和 sessionId
   */
  const getCurrentAuthInfo = (): { token: string | null; sessionId: string | null } => {
    const tokenData = getToken()
    if (!tokenData || !tokenData.accessToken) {
      console.warn('[SSE] 未找到 token')
      return { token: null, sessionId: null }
    }

    const token = tokenData.accessToken
    const sessionId = tokenData.sessionId

    if (!sessionId) {
      console.warn('[SSE] 未找到 sessionId')
      return { token: null, sessionId: null }
    }

    const sessionKey = getSessionKey(sessionId)
    const cookieData = Cookies.get(sessionKey)

    if (!cookieData) {
      console.warn(`[SSE] Cookie 中未找到 session，key: ${sessionKey}`)
      return { token: null, sessionId: null }
    }

    console.log(`[SSE] 成功获取认证信息，sessionId: ${sessionId}`)
    console.log(`[SSE] token (前32位): ${token.substring(0, 32)}`)
    return { token, sessionId }
  }

  /**
   * 建立 SSE 连接
   */
  const connectSSE = () => {
    const { token, sessionId } = getCurrentAuthInfo()

    if (!token || !sessionId) {
      console.warn('[SSE] 未找到 token 或 sessionId，无法建立连接')
      return
    }

    console.log(`[SSE] 使用当前会话的 sessionId: ${sessionId}`)
    console.log('[SSE] 正在建立连接...')

    // 关闭旧连接
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }

    try {
      // 通过 URL 参数传递 token 和 sessionId
      const url = `/api/user/notifications?${yuncodeConfig.tokenName}=${encodeURIComponent(token)}&sessionId=${encodeURIComponent(sessionId)}`
      eventSource = new EventSource(url)

      // 连接成功
      eventSource.addEventListener('connected', (event) => {
        console.log('[SSE] ✅ 连接已建立', event.data)
      })

      // 收到踢出通知
      eventSource.addEventListener('kick_out', (event) => {
        console.log('[SSE] ⚠️ 收到踢出通知')
        try {
          const data = JSON.parse(event.data)
          console.log('[SSE] 通知数据:', data)
          handleKickOut(data)
        } catch (e) {
          console.error('[SSE] 解析通知数据失败:', e)
        }
      })

      // 收到倒计时更新
      eventSource.addEventListener('kick_out_update', (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log(`[SSE] ⏱️ 倒计时: ${data.countdown} 秒`)
          updateCountdown(data.countdown)
        } catch (e) {
          console.error('[SSE] 解析倒计时数据失败:', e)
        }
      })

      // 倒计时结束
      eventSource.addEventListener('kick_out_final', (event) => {
        console.log('[SSE] 👋 倒计时结束')
        handleLogout()
      })

      // 连接错误
      eventSource.onerror = (error) => {
        console.error('[SSE] ❌ 连接错误', error)

        // 检查是否是认证错误（401）
        if (eventSource && eventSource.readyState === EventSource.CLOSED) {
          console.warn('[SSE] 连接已关闭，可能是 token 失效，停止重连')
          eventSource.close()
          eventSource = null
        }
      }
    } catch (error) {
      console.error('[SSE] 创建连接失败:', error)
    }
  }

  /**
   * 处理踢出通知
   */
  const handleKickOut = (data: any) => {
    kickOutData.value = {
      message: data.message || '您已被管理员强制下线',
      reason: data.reason || '被管理员踢出',
      countdown: data.countdown || 5
    }
    countdown.value = data.countdown || 5
    showKickOutDialog.value = true

    // 显示 Element Plus 通知
    ElNotification({
      title: '系统通知',
      message: data.message || '您已被管理员强制下线',
      type: 'warning',
      duration: 5000,
      showClose: false
    })
  }

  /**
   * 更新倒计时
   */
  const updateCountdown = (count: number) => {
    countdown.value = count

    // 最后3秒显示紧迫提示
    if (count <= 3) {
      ElNotification({
        title: '即将退出',
        message: `${count} 秒后将强制退出！`,
        type: 'error',
        duration: 1000,
        showClose: false
      })
    }
  }

  /**
   * 退出登录
   */
  const handleLogout = () => {
    console.log('[SSE] 开始退出登录...')

    // 关闭对话框
    showKickOutDialog.value = false

    // 先关闭 SSE 连接，防止重连
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }

    // 调用退出登录
    userStore.logOut()
  }

  // 组件卸载时清理
  onUnmounted(() => {
    disconnect()
  })

  return {
    countdown,
    showKickOutDialog,
    kickOutData,
    connectSSE,
    disconnect,
    logoutNow: handleLogout,
    handleLogout
  }
}
```

**关键点：**
- SSE URL 参数：`/api/user/notifications?token=xxx&sessionId=yyy`
- 监听三个事件：`kick_out`、`kick_out_update`、`kick_out_final`
- 倒计时结束后调用 `userStore.logOut()`

#### 3. Axios 请求配置

**http/index.ts**
```typescript
// API 基础路径前缀
const API_BASE_PREFIX = import.meta.env.VITE_API_BASE_PREFIX || "/api";

// 开发环境下，Vite 已经配置了代理，不需要 baseURL
// 生产环境下，需要 baseURL 来指向完整的 API 地址
const isDev = import.meta.env.DEV;
const baseURL = isDev ? "" : API_BASE_PREFIX;

const defaultConfig: AxiosRequestConfig = {
  baseURL: baseURL,
  timeout: 10000,
  headers: {
    Accept: "application/json, text/plain, */*",
    "Content-Type": "application/json",
    "X-Requested-With": "XMLHttpRequest"
  }
};

class PureHttp {
  private httpInterceptorsRequest(): void {
    PureHttp.axiosInstance.interceptors.request.use(
      async (config: PureHttpRequestConfig): Promise<any> => {
        // 为所有请求添加 /api 前缀（如果还没有的话）
        if (config.url && !config.url.startsWith("/api") && !config.url.startsWith("http")) {
          config.url = "/api" + config.url;
        }

        // ... token 处理逻辑
      }
    );
  }
}
```

**关键点：**
- 开发环境：`baseURL = ""`（让 Vite 代理处理）
- 生产环境：`baseURL = "/api"`
- 请求拦截器：自动为所有请求添加 `/api` 前缀

---

## 数据流程

### 1. 登录流程

```
┌─────────────┐
│  前端登录表单  │
└──────┬──────┘
       │ POST /api/auth/admin/login
       ▼
┌─────────────┐
│ AdminLogin  │
│   Service   │
└──────┬──────┘
       │ 1. 校验用户名密码
       │ 2. StpUtil.login(userId)
       │ 3. 生成 sessionId (UUID)
       │ 4. 存储 sessionId 到 Sa-Token session
       │ 5. 获取 Sa-Token JWT token
       │ 6. 添加在线用户记录到 Redis
┌───────┴───────────┐
│      Redis         │
│  online_user:{    │
│   sessionId}        │
└───────────────────┘
       │ 返回 {token, sessionId}
       ▼
┌─────────────┐
│  前端保存认证   │
│  • Cookie:      │
│    session-{     │
│     sessionId}   │
│  • sessionStorage:│
│    token         │
│    sessionId     │
└─────────────────┘
```

### 2. SSE 连接建立流程

```
┌─────────────┐
│  前端登录成功  │
└──────┬──────┘
       │ App.vue onMounted
       ▼
┌─────────────┐
│ checkAndConnect│
│   • getToken() │
│   • 检查 Cookie │
└──────┬──────┘
       │ 有 token 和 sessionId
       ▼
┌─────────────┐
│ connectSSE()  │
│   • GET /api/  │
│      user/    │
│      notifications│
│      ?token=xxx&sessionId=yyy│
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│ NotificationController│
│   • 验证 token        │
│   • 获取 userId        │
└──────┬────────────────┘
       │
       ▼
┌─────────────────────┐
│ NotificationService  │
│   • 创建 SSE 连接     │
│   • Key: userId-      │
││      sessionId        │
│   • 发送 connected 事件│
└─────────────────────┘
```

### 3. 踢出流程

```
┌─────────────┐
│ 管理员点击踢出 │
└──────┬──────┘
       │ POST /api/system/online-users/{sessionId}/kick
       ▼
┌─────────────────┐
│OnlineUserService  │
│  kickOutUser()  │
└──────┬──────────┘
       │
       ├───────────┐
       ▼           │
┌─────────────┐  │
│Notification│  │
│  Service   │  │
└──────┬──────┘  │
       │           │
       ▼           │
┌─────────────┐  │
│  SSE 连接    │  │
│userId-sessionId│ │
└──────┬──────┘  │
       │           │
       ▼           │
   ┌───┴────────┐ │
   │发送通知      │ │
   │• kick_out    │ │
   │• 倒计时更新  │ │
   │• kick_out_  │ │
   │  final      │ │
   └─────────────┘ │
                     │
                     ▼
         ┌───────────┴──────┐
         │   前端倒计时显示    │
         │   • 5、4、3、2、1    │
         └───────┬────────────┘
                 │
                 ▼
         ┌───────────┴───────┐
         │  倒计时结束        │
         │  • 调用 logOut()    │
         │  • 清除 Cookie     │
         │  • 清除 sessionStorage│
         │  • 跳转登录页       │
         └────────────────────┘
```

### 4. 退出登录流程

```
┌─────────────┐
│ 用户点击退出   │
└──────┬──────┘
       │ userStore.logOut()
       ▼
┌─────────────┐
│  logout API  │
│ POST /api/   │
│  auth/logout│
└──────┬──────┘
       │
       ▼
┌─────────────┐
│AuthService  │
│ .logout()   │
└──────┬──────┘
       │ 1. 从 session 获取 sessionId
       │ 2. 删除 Redis 在线用户记录
       │ 3. 调用 StpUtil.logout()
       │
       ├──────────┐
       ▼          │
┌──────────┐   │
│  Redis   │   │
│  删除    │   │
│online_  │   │
│  user:  │   │
│{sessionId}│  │
└─────────┘   │
                │
       ┌───────┴──────────┐
       │                  │
       ▼                  ▼
┌─────────────┐    ┌──────────────┐
│Sa-Token     │    │ Notification │
│清除 Session │    │  Service      │
└─────────────┘    │关闭 SSE 连接  │
                      └──────────────┘
```

---

## 使用示例

### 1. 登录

```javascript
// 前端调用登录
const loginData = {
  username: 'admin',
  password: 'admin123'
}

const res = await adminLogin(loginData)
// 后端返回：
// {
//   code: 200,
//   data: {
//     token: "eyJ0eXAiOiJKV1QiLCJhbGc...",
//     sessionId: "3fb093fe08fd40c18cb4fff1453ec41d",
//     userId: 1,
//     username: "admin",
//     ...
//   }
// }

// 保存 token 和 sessionId
setToken(res.data)

// Cookie 存储：
// Key: session-3fb093fe08fd40c18cb4fff1453ec41d
// Value: { accessToken: "...", sessionId: "3fb093fe...", ... }

// sessionStorage 存储：
// current-token: "eyJ0eXAiOiJKV1Q..."
// current-sessionId: "3fb093fe08fd40c18cb4fff1453ec41d"
```

### 2. 查看在线用户

```java
// 后端 API
GET /api/system/online-users?page=1&size=20

// 返回：
{
  "code": 200,
  "data": {
    "records": [
      {
        "sessionId": "3fb093fe08fd40c18cb4fff1453ec41d",
        "userId": 1,
        "username": "admin",
        "nickname": "管理员",
        "tenantId": 1,
        "tenantName": "默认租户",
        "ip": "127.0.0.1",
        "loginTime": "2026-01-22T00:45:28",
        "lastAccessTime": "2026-01-22T00:45:28",
        "status": "active"
      }
    ],
    "total": 1
  }
}
```

### 3. 踢出用户

```javascript
// 前端调用踢出
const sessionId = '3fb093fe08fd40c18cb4fff1453ec41d'

// 方式 1: 单个踢出
await kickOutUser(sessionId)

// 方式 2: 批量踢出
await batchKickOut([sessionId1, sessionId2, sessionId3])

// 后端处理：
// 1. 查找 Redis 中的在线用户记录
// 2. 发送 SSE 踢出通知（带倒计时）
// 3. 前端显示倒计时对话框
// 4. 倒计时结束后，前端调用退出接口
```

### 4. 用户收到踢出通知

```javascript
// 前端自动监听 SSE 事件
eventSource.addEventListener('kick_out', (event) => {
  const data = JSON.parse(event.data)
  // data: { message: "您已被管理员强制下线", countdown: 5, reason: "被管理员踢出" }

  // 显示对话框
  showKickOutDialog.value = true
  countdown.value = data.countdown
})

// 倒计时更新
eventSource.addEventListener('kick_out_update', (event) => {
  const data = JSON.parse(event.data)
  countdown.value = data.countdown
  // 5、4、3、2、1
})

// 倒计时结束
eventSource.addEventListener('kick_out_final', (event) => {
  // 自动调用退出
  logOut()
})
```

---

## 问题排查

### 常见问题

#### 1. SSE 连接失败

**症状：** 前端提示"SSE 未找到 token 或 sessionId"

**排查步骤：**
1. 检查登录后端返回数据是否包含 `sessionId`
2. 检查前端控制台是否输出：
   ```
   [Auth] 设置 Token，sessionId: xxx
   [App] sessionId: xxx
   ```
3. 检查 Cookie 中是否有 `session-{sessionId}`

**解决方案：**
- 确保 `LoginResponse` 和 `UserResult` 接口包含 `sessionId` 字段
- 确保 `setToken()` 函数在 Cookie 中保存了 `sessionId`

#### 2. 踢出后 Redis 没有清理

**症状：** 踢出用户后，在线用户列表中仍然存在

**排查步骤：**
1. 查看后端日志，确认是否有：
   ```
   [Logout] 准备移除在线用户记录，sessionId=xxx
   [Redis] 准备删除在线用户记录，key=online_user:xxx
   [Redis] 删除结果: true
   ```
2. 检查 Redis 中是否还有 key：
   ```bash
   redis-cli KEYS "online_user:*"
   ```

**解决方案：**
- 确保退出时调用的是 `/api/auth/logout` 而不是 `/auth/logout`
- 检查 axios 请求拦截器是否正常添加 `/api` 前缀

#### 3. 多标签页登录问题

**症状：** 同一账号在多个标签页登录，踢出一个时其他也退出了

**原因：** SSE 连接 key 应该是 `userId-sessionId`，而不是 `userId`

**解决方案：**
- 确保 `NotificationService` 使用 `userId + "-" + sessionId` 作为 key
- 确保 `NotificationController` 接收 `sessionId` 参数
- 确保前端 SSE 连接传递 `sessionId` 参数

#### 4. Cookie key 冲突

**症状：** 不同会话的 Cookie 覆盖

**原因：** Cookie key 没有使用 `sessionId` 区分

**解决方案：**
- Cookie key 格式：`session-{sessionId}`
- 每个会话都有独立的 Cookie

### 调试日志

**前端日志：**
```
[Auth] 设置 Token，sessionId: 3fb093fe08fd40c18cb4fff1453ec41d
[App] 用户已登录，初始化 SSE 连接
[App] sessionId: 3fb093fe08fd40c18cb4fff1453ec41d
[SSE] 成功获取认证信息，sessionId: 3fb093fe08fd40c18cb4fff1453ec41d
[SSE] 正在建立连接...
[SSE] ✅ 连接已建立
```

**后端日志：**
```
生成业务会话ID: userId=1, sessionId=3fb093fe08fd40c18cb4fff1453ec41d
sessionId 已存入 Sa-Token session，验证: 3fb093fe08fd40c18cb4fff1453ec41d
[Redis] 添加在线用户记录，key=online_user:3fb093fe08d40c18cb4fff1453ec41d
[Redis] 在线用户记录已添加
========================================
收到 SSE 连接请求
✅ 用户身份验证成功: userId=1, sessionId=3fb093fe08fd40c18cb4fff1453ec41d
✅ SSE 连接创建成功: userId=1, sessionId=3fb093fe08d40c18cb4fff1453ec41d
========================================
[Logout] 开始登出流程，sessionId=3fb093fe08fd40c18cb4fff1453ec41d
[Logout] 准备移除在线用户记录，sessionId=3fb093fe08d40c18cb4fff1453ec41d
[Redis] 准备删除在线用户记录，key=online_user:3fb093fe08d40c18cb4fff1453ec41d
[Redis] 删除结果: true, key=online_user:3fb093fe08d40c18cb4fff1453ec41d
[Logout] 已移除在线用户记录，sessionId=3fb093fe08d40c18cb4fff1453ec41d
[Logout] StpLogic.logout() 执行成功
[Logout] 登出流程完成
```

---

## 总结

### 方案优势

1. **完全的会话隔离**：每个登录会话都有独立的 sessionId
2. **精确的踢出控制**：可以踢出指定会话，不影响其他会话
3. **良好的用户体验**：SSE 实时通知 + 倒计时
4. **自动清理失效会话**：通过 token 验证自动清理过期会话
5. **高性能**：使用 32 字符 UUID 作为 key，而不是 200+ 字符的 JWT

### 技术亮点

1. **sessionId 与 Token 分离**：清晰职责分离
2. **多级存储策略**：Cookie + sessionStorage + Redis
3. **SSE 长连接**：实时推送踢出通知
4. **自动清理机制**：登录时清理失效 token
5. **完善的日志系统**：方便问题排查

### 注意事项

1. **必须同步修改**：前后端的 `sessionId` 字段要保持一致
2. **顺序很重要**：退出时先删除业务数据，再清除认证数据
3. **SSE 连接管理**：及时断开无效连接，避免内存泄漏
4. **Redis TTL 设置**：7天过期，避免占用过多内存
5. **Token 验证**：获取在线用户列表时验证 token 有效性

---

## 附录

### 相关文件列表

**后端文件：**
- `OnlineUser.java` - 在线用户实体类
- `LoginVO.java` - 登录响应 VO
- `AdminLoginService.java` - 管理员登录服务
- `UserLoginService.java` - 用户登录服务
- `TenantLoginService.java` - 租户登录服务
- `OnlineUserService.java` - 在线用户服务接口
- `OnlineUserServiceImpl.java` - 在线用户服务实现
- `NotificationService.java` - SSE 通知服务
- `NotificationController.java` - SSE 控制器
- `AuthService.java` - 认证服务（退出登录）

**前端文件：**
- `auth.ts` - 认证工具类
- `user.ts` - 用户 API
- `auth.ts` - 登录/登出 API
- `useKickOutNotification.ts` - SSE 连接管理
- `user.ts` (store) - 用户状态管理
- `http/index.ts` - Axios 配置
- `App.vue` - 应用根组件（SSE 初始化）

**配置文件：**
- `application.yml` - Sa-Token 配置
- `.env.development` - 前端环境变量
- `vite.config.ts` - Vite 代理配置

---

**文档版本：** v1.0
**最后更新：** 2026-01-22
**维护者：** 云代码团队
