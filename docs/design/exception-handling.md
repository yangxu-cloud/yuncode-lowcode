# 统一异常处理使用指南

## 概述

本项目实现了 **完善的SaaS系统统一异常处理方案**，包括：
- ✅ 统一错误码管理（ErrorCode 枚举）
- ✅ 分层异常体系（BaseException、BusinessException、ParamException）
- ✅ 自动日志记录（ExceptionLogUtil 工具类）
- ✅ 全局异常处理（GlobalExceptionHandler）
- ✅ 支持多租户日志（记录租户ID、用户信息）
- ✅ 环境区分（开发环境显示详细错误，生产环境隐藏敏感信息）

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller 层                            │
│  不需要 try-catch，抛出异常即可，由全局处理器统一处理         │
└───────────────────────────┬─────────────────────────────────┘
                            │ 抛出异常
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  GlobalExceptionHandler                       │
│  统一捕获所有异常，记录日志，返回标准响应格式                 │
└───────────────────────────┬─────────────────────────────────┘
                            │ 处理异常
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   ExceptionLogUtil                            │
│  记录异常日志（请求信息、用户信息、堆栈跟踪）                 │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ↓
                    返回 Result<T>
                    { code, message, data }
```

## 异常类说明

### 1. ErrorCode - 错误码枚举

统一管理系统所有错误码，按功能模块分类。

**错误码规则**：
- `1xx` - 信息提示
- `2xx` - 操作成功
- `4xx` - 客户端错误（参数错误、权限不足等）
- `5xx` - 服务端错误（系统异常、业务异常等）
- `6xxx` - 业务错误（用户、租户、登录、数据、操作、文件、导入导出等）

**使用示例**：
```java
throw new BusinessException(ErrorCode.USER_NOT_FOUND);
throw new BusinessException(ErrorCode.TENANT_DISABLED, "租户已禁用");
throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作失败，请稍后重试");
```

### 2. BaseException - 基础异常类

所有自定义异常的父类，提供统一的异常结构。

```java
// 使用错误码枚举
new BaseException(ErrorCode errorCode)
new BaseException(ErrorCode errorCode, String message)
new BaseException(ErrorCode errorCode, String message, String detail)
new BaseException(ErrorCode errorCode, Throwable cause)

// 直接指定错误码和消息
new BaseException(Integer code, String message)
new BaseException(Integer code, String message, String detail)
```

### 3. BusinessException - 业务异常

继承自 BaseException，用于处理业务逻辑异常。

**使用场景**：
- 业务规则校验失败
- 数据不存在
- 数据冲突
- 权限不足
- 状态不允许等业务异常

**使用示例**：
```java
// 1. 使用错误码枚举（推荐）
throw new BusinessException(ErrorCode.USER_NOT_FOUND);

// 2. 自定义消息
throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");

// 3. 静态工厂方法
throw BusinessException.notFound("用户不存在");
throw BusinessException.alreadyExists("用户名已存在");
throw BusinessException.failed("删除用户失败");
throw BusinessException.forbidden("无权访问此资源");
```

### 4. ParamException - 参数校验异常

继承自 BaseException，专门用于处理参数校验异常。

**使用场景**：
- @Valid 注解校验失败
- 手动参数校验失败
- 参数格式错误、类型错误等

**使用示例**：
```java
// 1. 使用错误码枚举
throw new ParamException(ErrorCode.PARAM_MISSING, "username");

// 2. 指定字段名和消息
throw new ParamException("username", "用户名不能为空");

// 3. 静态工厂方法（推荐）
throw ParamException.missing("username");
throw ParamException.invalid("email", "邮箱格式不正确");
throw ParamException.formatError("phone", "手机号");
throw ParamException.typeError("age", "Integer");
```

### 5. ExceptionLogUtil - 异常日志工具类

统一记录异常日志，包含：
- 异常信息（类型、错误码、错误消息）
- 用户信息（loginId、username、tenantId）
- 请求信息（method、uri、queryString、remoteAddr）

**使用示例**：
```java
// 记录业务异常（不记录堆栈）
ExceptionLogUtil.logBusinessException(e, "业务异常");

// 记录系统异常（记录完整堆栈）
ExceptionLogUtil.logSystemException(e);

// 记录参数校验异常
ExceptionLogUtil.logParamError("username", null, "用户名不能为空");
```

## 业务代码中的使用规范

### Service 层使用示例

```java
@Service
public class UserServiceImpl implements UserService {

    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    public void createUser(User user) {
        // 校验用户名
        if (StringUtils.isEmpty(user.getUsername())) {
            throw ParamException.missing("username");
        }

        // 检查用户名是否存在
        User existUser = userMapper.selectByUsername(user.getUsername());
        if (existUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 保存用户
        userMapper.insert(user);
    }

    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 检查用户是否正在使用
        if (user.isActive()) {
            throw BusinessException.notAllowed("该用户正在使用中，无法删除");
        }

        // 删除用户
        userMapper.deleteById(id);
    }
}
```

### Controller 层使用示例

```java
@RestController
public class UserController {

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        // 不需要 try-catch，直接抛出异常即可
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @PostMapping
    public Result<Void> createUser(@Valid @RequestBody UserDTO userDTO) {
        userService.createUser(userDTO);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
```

## 常见使用场景

### 场景1：数据不存在

```java
// ❌ 不推荐：返回 null
public User getUserById(Long id) {
    return userMapper.selectById(id);
}

// ✅ 推荐：抛出异常
public User getUserById(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    return user;
}
```

### 场景2：参数校验

```java
// ❌ 不推荐：在 Controller 层手动校验
@PostMapping
public Result<Void> createUser(@RequestBody UserDTO userDTO) {
    if (StringUtils.isEmpty(userDTO.getUsername())) {
        return Result.error("用户名不能为空");
    }
    // ...
}

// ✅ 推荐：使用 @Valid 自动校验 + 异常
@PostMapping
public Result<Void> createUser(@Valid @RequestBody UserDTO userDTO) {
    userService.createUser(userDTO);
    return Result.success();
}
```

### 场景3：业务规则校验

```java
// ❌ 不推荐：返回 boolean
public boolean deleteUser(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {
        return false;
    }
    userMapper.deleteById(id);
    return true;
}

// ✅ 推荐：抛出异常
public void deleteUser(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {
        throw BusinessException.notFound("用户不存在");
    }
    userMapper.deleteById(id);
}
```

### 场景4：权限校验

```java
// ❌ 不推荐：返回 boolean
public boolean hasPermission(String permission) {
    if (!StpUtil.hasPermission(permission)) {
        return false;
    }
    return true;
}

// ✅ 推荐：抛出异常（Sa-Token 会自动拦截，这里只是示例）
public void checkPermission(String permission) {
    if (!StpUtil.hasPermission(permission)) {
        throw new BusinessException(ErrorCode.NO_PERMISSION, "权限不足: " + permission);
    }
}
```

## 最佳实践

### ✅ DO（推荐做法）

1. **使用 ErrorCode 枚举**：统一使用 ErrorCode 枚举管理错误码
2. **抛出具体异常**：使用 BusinessException 或其子类
3. **不吞异常**：不要 catch 异常后不处理，要么记录日志，要么抛出新异常
4. **使用静态工厂方法**：利用异常类的静态工厂方法，代码更简洁
5. **Service 层抛异常**：Service 层负责抛出异常，Controller 层不处理异常

### ❌ DON'T（不推荐做法）

1. **不要返回 null**：数据不存在时抛出异常，而不是返回 null
2. **不要使用返回码**：不要用 boolean 或 int 返回码表示成功/失败
3. **不要在 Controller 捕获异常**：GlobalExceptionHandler 会统一处理
4. **不要硬编码错误码**：使用 ErrorCode 枚举，不要直接写 404、500 等数字
5. **不要暴露敏感信息**：生产环境不要返回数据库错误、堆栈跟踪等敏感信息

## 扩展指南

### 添加新的错误码

在 `ErrorCode.java` 中添加新的错误码：

```java
// 在合适的分组下添加新错误码
// 6080 订单相关
ORDER_NOT_FOUND(6080, "订单不存在"),
ORDER_STATUS_ERROR(6081, "订单状态错误"),
ORDER_PAID(6082, "订单已支付，无法取消");
```

### 创建自定义异常类

如果需要特殊类型的异常，继承 BaseException：

```java
public class OrderException extends BaseException {

    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }

    public OrderException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // 静态工厂方法
    public static OrderException notFound() {
        return new OrderException(ErrorCode.ORDER_NOT_FOUND);
    }

    public static OrderException statusError() {
        return new OrderException(ErrorCode.ORDER_STATUS_ERROR);
    }
}
```

## 注意事项

⚠️ **前后端必须保持一致**: 前端的 `VITE_TOKEN_NAME` 必须与后端的 `sa-token.token-name` 一致

⚠️ **重启服务**: 修改配置后需要重启前后端服务

⚠️ **清除缓存**: 修改前端环境变量后，建议清除浏览器缓存重新构建

⚠️ **统一日志格式**: 所有异常日志会自动包含用户信息、请求信息等，便于排查问题

## 总结

本统一异常处理方案提供了：

1. **统一性**：所有异常统一处理，返回格式一致
2. **自动化**：自动记录日志，不需要手动写 try-catch
3. **规范性**：统一的错误码管理和异常使用规范
4. **易于维护**：修改配置后重启即可生效
5. **开发友好**：代码简洁，不需要记住复杂的异常处理逻辑

遵循这个规范，团队成员可以轻松写出高质量、易维护的异常处理代码！
