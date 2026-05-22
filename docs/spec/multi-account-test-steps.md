# 多账号体系登录隔离 - 测试验证步骤

## 前置条件

1. 后端服务已启动（Spring Boot 应用）
2. 前端服务已启动（Vue 开发服务器）
3. MySQL 数据库运行正常
4. Redis 服务运行正常
5. 浏览器开发者工具已打开（Console 和 Application 标签）

## 编译后端项目

### 方法1: 使用 IDE
1. 在 IntelliJ IDEA 中打开项目
2. 等待 Maven 依赖下载完成
3. 点击 `Build -> Rebuild Project`
4. 检查 Build Output 是否有编译错误

### 方法2: 使用 Maven 命令行
```bash
cd "c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot"
mvn clean compile -DskipTests
```

预期结果：编译成功，无错误

## 测试步骤

### 测试1: 管理员登录

1. 打开浏览器新页签，访问：`http://localhost:3000/console/login`

2. 输入管理员账号：
   - 用户名: `admin`
   - 密码: `admin123`

3. 点击登录

4. **验证点**：
   - [ ] 登录成功，跳转到控制台首页
   - [ ] 浏览器 Console 显示：`[User Store] 从admin恢复用户信息: admin`
   - [ ] 浏览器 Console 显示：`[Request] 添加 Token (admin): eyJ0eXAi...`
   - [ ] Application → Local Storage 中有：
     - `token_admin: "eyJ0eXAi..."`
     - `userInfo_admin: "{\"username\":\"admin\",\"loginType\":\"admin\",...}"`
     - `loginType: "admin"`
   - [ ] 页面右上角显示管理员信息（admin）

### 测试2: 普通用户登录（不同页签）

1. 打开新的浏览器页签（不要关闭管理员页签）

2. 访问：`http://localhost:3000/login`

3. 输入普通用户账号：
   - 租户编码: `default`
   - 用户名: `testuser`
   - 密码: `admin123`

4. 点击登录

5. **验证点**：
   - [ ] 登录成功，跳转到首页
   - [ ] 浏览器 Console 显示：`[User Store] 从user恢复用户信息: testuser`
   - [ ] 浏览器 Console 显示：`[Request] 添加 Token (user): eyJ0eXAi...`
   - [ ] Application → Local Storage 中有：
     - `token_user: "eyJ0eXAi..."`
     - `userInfo_user: "{\"username\":\"testuser\",\"loginType\":\"user\",...}"`
     - `loginType: "user"`
   - [ ] 页面右上角显示用户信息（testuser）

### 测试3: 刷新页签验证状态保持

1. 刷新管理员页签（`/console/*`）

2. **验证点**：
   - [ ] 仍然显示 admin 登录状态
   - [ ] Console 显示：`[User Store] 从admin恢复用户信息: admin`
   - [ ] 请求使用 admin token

3. 刷新普通用户页签

4. **验证点**：
   - [ ] 仍然显示 testuser 登录状态
   - [ ] Console 显示：`[User Store] 从user恢复用户信息: testuser`
   - [ ] 请求使用 user token

### 测试4: 检查请求日志

1. 在管理员页签中打开浏览器开发者工具 → Network 标签

2. 刷新页面或执行任何操作（如访问在线用户）

3. **验证点**：
   - [ ] 请求头包含：`Authorization: Bearer {token_admin的值}`
   - [ ] Console 显示：`[Request] 添加 Token (admin): ...`

4. 切换到普通用户页签

5. 刷新页面或执行任何操作

6. **验证点**：
   - [ ] 请求头包含：`Authorization: Bearer {token_user的值}`
   - [ ] Console 显示：`[Request] 添加 Token (user): ...`

### 测试5: 退出登录隔离

1. 在管理员页签点击退出

2. **验证点**：
   - [ ] 跳转到 `/console/login`
   - [ ] `token_admin` 和 `userInfo_admin` 已清除
   - [ ] 普通用户页签仍然保持登录状态

3. 在普通用户页签点击退出

4. **验证点**：
   - [ ] 跳转到 `/login`
   - [ ] `token_user` 和 `userInfo_user` 已清除

### 测试6: 多账号同时在线

1. 同时登录管理员和普通用户

2. 访问在线用户管理页面（两个页签都访问）

3. **验证点**：
   - [ ] 管理员页签可以看到所有在线用户（包括 admin 和 testuser）
   - [ ] 每个在线用户显示正确的登录类型（admin/user）
   - [ ] 可以踢出指定用户

### 测试7: API 端点隔离

**使用 Postman 或 curl 测试**

1. 管理员登录获取 token：
```bash
curl -X POST http://localhost:8080/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

2. 使用 admin token 访问 `/console/*` 路径：
```bash
curl -X GET http://localhost:8080/api/system/online-users \
  -H "Authorization: Bearer {admin_token}"
```
预期：返回在线用户列表

3. 使用 admin token 访问 `/tenant/*` 路径：
```bash
curl -X GET http://localhost:8080/api/tenant/some-endpoint \
  -H "Authorization: Bearer {admin_token}"
```
预期：可能返回 401 或 403（取决于权限配置）

## 常见问题排查

### 问题1: 编译错误

**错误信息**：`StpUtil cannot be resolved`

**解决方案**：
1. 检查所有登录服务类是否已将 `StpUtil` 替换为对应的 `StpLogic`
2. 确认 `SaTokenMultiAccountConfig.java` 已正确创建
3. 在 IDE 中重新导入 Maven 依赖

### 问题2: 登录失败

**错误信息**：`未登录，请先登录`

**可能原因**：
1. 前端请求拦截器选择了错误的 token
2. 后端 Sa-Token 拦截器使用了错误的 StpLogic

**排查步骤**：
1. 检查浏览器 Console，查看 `[Request] 添加 Token (xxx)` 日志
2. 确认 token 类型与当前页面路径匹配
3. 检查后端日志，确认使用的 StpLogic 类型

### 问题3: 刷新后登录状态丢失

**解决方案**：
1. 检查 `router/index.ts` 中的 `initUserInfo(preferredType)` 调用
2. 确认 `preferredType` 参数正确传递
3. 检查 localStorage 中是否存在对应类型的 token 和 userInfo

### 问题4: 请求返回 401

**排查步骤**：
1. 打开浏览器 Network 标签
2. 检查请求头中的 `Authorization` 值
3. 确认 token 与当前登录类型匹配
4. 检查后端日志，查看 token 验证过程

### 问题5: Redis 序列化错误

**错误信息**：`Unrecognized field "active"`

**解决方案**：
1. 确认 `OnlineUser` 实体类已添加 `@JsonIgnoreProperties(ignoreUnknown = true)`
2. 清除 Redis 中的旧数据：`redis-cli FLUSHDB`
3. 重新登录用户

## 性能测试

### 并发登录测试

```bash
# 使用 Apache Bench 或类似工具测试并发登录
ab -n 100 -c 10 -p admin_login.json -T application/json \
   http://localhost:8080/api/auth/admin/login
```

### Token 验证性能测试

监控以下指标：
- 登录响应时间（应该 < 500ms）
- Token 验证时间（应该 < 100ms）
- 在线用户查询时间（应该 < 200ms）

## 安全测试

### 测试1: Token 跨类型访问

1. 使用 admin token 访问 `/auth/user/*` 端点
2. 使用 user token 访问 `/auth/admin/*` 端点

**预期**：返回适当的错误（401/403）

### 测试2: Token 过期

1. 等待 token 过期（默认 30 天）
2. 或在 Redis 中手动删除 session

**预期**：返回 401 未登录错误

### 测试3: 踢出用户

1. 用户A登录
2. 管理员踢出用户A
3. 用户A尝试访问受保护资源

**预期**：返回 401 未登录错误

## 测试检查清单

### 功能测试
- [ ] 管理员登录成功
- [ ] 租户登录成功
- [ ] 普通用户登录成功
- [ ] 不同页签登录状态隔离
- [ ] 刷新页面状态保持
- [ ] 退出登录正常跳转
- [ ] 在线用户显示正确
- [ ] 踢出用户功能正常

### 安全测试
- [ ] Token 验证正常
- [ ] 跨类型访问被拒绝
- [ ] Token 过期处理正确
- [ ] 踢出用户后无法访问

### 性能测试
- [ ] 登录响应时间 < 500ms
- [ ] Token 验证时间 < 100ms
- [ ] 在线用户查询时间 < 200ms

### 兼容性测试
- [ ] Chrome 浏览器正常
- [ ] Firefox 浏览器正常
- [ ] Edge 浏览器正常
- [ ] Safari 浏览器正常（如适用）

## 日志分析

### 前端日志关键词

搜索以下关键词查看相关日志：
- `[Request] 添加 Token` - 查看 token 选择情况
- `[User Store] 从xxx恢复用户信息` - 查看用户信息恢复
- `[路由守卫]` - 查看路由导航过程

### 后端日志关键词

搜索以下关键词查看相关日志：
- `管理员登录成功` - 管理员登录
- `租户登录成功` - 租户登录
- `普通用户登录成功` - 用户登录
- `Sa-Token` - Token 验证相关

## 成功标准

所有测试通过后，系统应满足以下标准：

1. ✅ 三种登录类型可以独立登录
2. ✅ 不同页签保持各自的登录状态
3. ✅ 刷新页面后正确恢复登录状态
4. ✅ 请求自动使用对应类型的 token
5. ✅ 退出登录正确清除对应类型的数据
6. ✅ 在线用户管理正常工作
7. ✅ 无编译错误或运行时错误
8. ✅ 性能满足预期要求

## 下一步优化建议

1. **Token 续期**：实现 token 自动续期机制
2. **单点登录**：支持同账号多设备登录管理
3. **审计日志**：记录所有登录/登出操作
4. **限流保护**：防止暴力破解
5. **验证码**：登录时添加验证码
