# 多租户隔离实现文档

## 📋 文档信息

- **版本**：v1.0
- **创建时间**：2025-01-28
- **作者**：Yuncode Team
- **状态**：已实现

---

## 📌 目录

1. [背景与目标](#1-背景与目标)
2. [技术方案](#2-技术方案)
3. [核心配置](#3-核心配置)
4. [代码示例](#4-代码示例)
5. [使用指南](#5-使用指南)
6. [注意事项](#6-注意事项)

---

## 1. 背景与目标

### 1.1 业务背景

系统采用 **多租户 SaaS 架构**，不同租户的数据需要完全隔离：
- 租户 A 的用户只能访问租户 A 的数据
- 租户 B 的用户只能访问租户 B 的数据
- 系统数据按 `tenant_id` 字段进行逻辑隔离

### 1.2 技术目标

- ✅ **自动隔离**：所有 SQL 自动添加租户条件，无需手动传递
- ✅ **安全可靠**：防止跨租户数据泄露，无法手动篡改租户ID
- ✅ **代码简洁**：减少重复代码，统一隔离策略
- ✅ **易于维护**：统一的租户管理逻辑

---

## 2. 技术方案

### 2.1 隔离策略

采用 **共享数据库 + 共享表 + 字段隔离** 策略：

```
表：sys_user
+----+----------+-----------+
| id | username | tenant_id |
+----+----------+-----------+
| 1  | user_a   | 1         |  ← 租户 A
| 2  | user_b   | 2         |  ← 租户 B
| 3  | user_c   | 1         |  ← 租户 A
+----+----------+-----------+
```

### 2.2 核心技术栈

| 技术 | 说明 |
|------|------|
| **MyBatis-Plus** | ORM 框架，提供多租户插件 |
| **Sa-Token** | 认证授权框架，管理用户 Session |
| **TenantLineInnerInterceptor** | MyBatis-Plus 多租户拦截器 |
| **@TableLogic** | 逻辑删除注解 |

### 2.3 工作流程

```
┌─────────────┐
│ 用户登录     │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ 从租户编码获取   │
│ tenantId        │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│ 手动查询用户     │
│ (登录场景例外)  │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│ 存入 Sa-Token   │
│ Session        │
│ tenantId=2      │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│ 后续所有请求    │
│ 自动添加条件：  │
│ WHERE tenant_id=2│
└─────────────────┘
```

---

## 3. 核心配置

### 3.1 MyBatis-Plus 多租户插件配置

**文件**: `yuncode-admin/src/main/java/com/yuncode/admin/config/MyBatisPlusConfig.java`

```java
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 多租户插件（必须放在分页插件之前）
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                // 从 Sa-Token Session 获取当前租户ID
                if (!StpUtil.isLogin()) {
                    return null;  // 未登录时不进行租户过滤
                }
                Long tenantId = StpUtil.getSession().get("tenantId", 0L);
                if (tenantId == null || tenantId == 0) {
                    return null;
                }
                return new LongValue(tenantId);
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 忽略不需要租户隔离的表
                return "sys_tenant".equalsIgnoreCase(tableName)
                        || "sys_dict".equalsIgnoreCase(tableName)
                        || "sys_dict_data".equalsIgnoreCase(tableName)
                        || "sys_settings".equalsIgnoreCase(tableName);
            }
        });
        interceptor.addInnerInterceptor(tenantInterceptor);

        // 2. 分页插件
        PaginationInnerInterceptor paginationInnerInterceptor =
            new PaginationInnerInterceptor(DbType.MYSQL);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;
    }
}
```

**关键点**：
- 多租户插件必须放在分页插件之前
- `getTenantId()` 从 Sa-Token Session 获取租户ID
- `ignoreTable()` 指定不需要租户隔离的系统表

### 3.2 自动填充 tenantId

**文件**: `yuncode-admin/src/main/java/com/yuncode/admin/config/MyBatisPlusConfig.java`

```java
@Bean
public MetaObjectHandler metaObjectHandler() {
    return new MetaObjectHandler() {
        @Override
        public void insertFill(MetaObject metaObject) {
            // 自动填充租户ID
            if (StpUtil.isLogin()) {
                Long tenantId = StpUtil.getSession().get("tenantId", 0L);
                this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
            }
            // 其他字段...
        }
    };
}
```

### 3.3 实体类注解配置

**SysUser.java**:
```java
public class SysUser {
    // ...

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
```

**@TableField(fill = FieldFill.INSERT)** 的作用：
- 插入数据时自动填充 `tenant_id`
- 从 Sa-Token Session 获取当前租户ID

---

## 4. 代码示例

### 4.1 Controller 层

**改造前**：
```java
@GetMapping("/tree")
public Result<List<OrgTreeNode>> getOrgTree(
        @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
    List<OrgTreeNode> tree = orgService.getOrgTree(tenantId);
    return Result.success(tree);
}
```

**改造后**：
```java
@GetMapping("/tree")
public Result<List<OrgTreeNode>> getOrgTree() {
    // 多租户插件会自动添加 tenant_id 条件
    List<OrgTreeNode> tree = orgService.getOrgTree();
    return Result.success(tree);
}
```

### 4.2 Service 层

**改造前**：
```java
public List<OrgTreeNode> getOrgTree(Long tenantId) {
    LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(SysOrg::getTenantId, tenantId);  // 手动添加
    List<SysOrg> allOrgs = orgMapper.selectOrgTree(tenantId);
    ...
}
```

**改造后**：
```java
public List<OrgTreeNode> getOrgTree() {
    // 多租户插件会自动添加 tenant_id 条件
    List<SysOrg> allOrgs = orgMapper.selectList(null);
    ...
}
```

### 4.3 Mapper 层

**改造前**：
```java
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {
    @Select("SELECT * FROM sys_org WHERE tenant_id = #{tenantId}")
    List<SysOrg> selectOrgTree(@Param("tenantId") Long tenantId);
}
```

**改造后**：
```java
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {
    // 统一使用 MyBatis-Plus 标准方法
    // 多租户插件会自动添加 WHERE tenant_id = ?
}
```

### 4.4 登录场景（特殊处理）

**AbstractLoginStrategy.java**:
```java
public LoginVO login(LoginDTO loginDTO, HttpServletRequest request) {
    // 1. 获取租户
    SysTenant tenant = sysTenantMapper.selectByTenantCode(loginDTO.getTenantCode());
    Long tenantId = tenant.getId();

    // 2. 手动查询用户（登录时还未建立 Session）
    SysUser user = sysUserMapper.selectByUsernameAndTenantId(
        loginDTO.getUsername(),
        tenantId
    );

    // 3. 登录成功后存入 Session
    StpUtil.login(user.getId());
    StpUtil.getSession().set("tenantId", tenantId);
    ...
}
```

**注意**: `selectByUsernameAndTenantId` 方法仅用于登录场景，因为登录时无法自动获取租户ID。

---

## 5. 使用指南

### 5.1 开发规范

#### ✅ 应该做的

```java
// 1. Controller - 无需传递 tenantId
@GetMapping("/list")
public Result<List<SysOrg>> getList() {
    return Result.success(orgService.getList());
}

// 2. Service - 使用标准查询
public List<SysOrg> getList() {
    // 多租户插件会自动添加 WHERE tenant_id = ?
    return orgMapper.selectList(null);
}

// 3. Mapper - 使用标准方法
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {
    // 只保留标准 CRUD 方法
}
```

#### ❌ 不应该做的

```java
// 1. 手动添加租户条件（会重复）
wrapper.eq(SysOrg::getTenantId, tenantId);  // ❌

// 2. 手动设置租户ID（自动填充）
org.setTenantId(tenantId);  // ❌

// 3. 自定义 SQL 包含 tenant_id
@Select("SELECT * FROM sys_org WHERE tenant_id = #{tenantId}")  // ❌
```

### 5.2 登录场景处理

**适用场景**：登录时还未建立 Session

```java
// ✅ 正确做法
SysUser user = sysUserMapper.selectByUsernameAndTenantId(username, tenantId);
```

**原因**：
- 登录时 `StpUtil.isLogin() = false`
- 无法从 Session 获取 `tenantId`
- 必须手动指定租户ID 进行用户查询

### 5.3 查询场景

**登录后**：
```java
// ✅ 完全自动
@GetMapping("/list")
public List<SysOrg> getList() {
    return orgMapper.selectList(null);
}
```

**SQL 自动转换为**：
```sql
SELECT * FROM sys_org
WHERE tenant_id = 2  -- 多租户插件自动添加
AND deleted = 0       -- @TableLogic 自动添加
```

---

## 6. 注意事项

### 6.1 插件顺序

```java
MyBatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

// ⚠️ 顺序很重要！
interceptor.addInnerInterceptor(tenantInterceptor);       // 1. 多租户插件
interceptor.addInnerInterceptor(paginationInterceptor);    // 2. 分页插件
```

**错误示例**：
```java
// ❌ 分页插件在多租户插件之前 - 会出错！
interceptor.addInnerInterceptor(paginationInterceptor);
interceptor.addInnerInterceptor(tenantInterceptor);
```

### 6.2 忽略表配置

某些表不需要租户隔离，需要在 `ignoreTable()` 中配置：

```java
@Override
public boolean ignoreTable(String tableName) {
    return "sys_tenant".equalsIgnoreCase(tableName)      // 租户表本身
            || "sys_dict".equalsIgnoreCase(tableName)        // 字典表
            || "sys_dict_data".equalsIgnoreCase(tableName) // 字典数据表
            || "sys_settings".equalsIgnoreCase(tableName); // 系统配置表
}
```

### 6.3 实体类字段配置

确保 `tenant_id` 字段有正确的注解：

```java
@TableField(fill = FieldFill.INSERT)
private Long tenantId;
```

**作用**：
- 插入时自动填充 `tenant_id` 值
- 使用 `FieldFill.INSERT` 确保只在插入时填充，更新时不覆盖

### 6.4 前端处理

**不再需要**手动设置 `tenantId` 请求头：

```typescript
// ❌ 旧代码（已删除）
const userStore = useUserStoreHook();
if (userStore && userStore.tenantId) {
  config.headers["tenantId"] = userStore.tenantId;
}

// ✅ 新代码（不需要）
// 多租户插件自动处理，前端无需传递
```

### 6.5 Long 类型序列化

**JavaScript 精度问题**：
```javascript
// ❌ 有精度丢失
userId: 2016180715091513346  // 后端生成
userId: 2016180715091513300  // 前端接收（不一致！）
```

**解决方案**：配置 Jackson 将 Long 序列化为 String

**文件**: `yuncode-admin/src/main/java/com/yuncode/admin/config/JacksonConfig.java`

```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            builder.serializerByType(long.class, ToStringSerializer.instance);
            builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
        };
    }
}
```

---

## 7. 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         前端层                              │
│  ┌────────────┐  ┌─────────────┐  ┌──────────────┐                │
│  │ Vue 组件     │  │ API 调用     │  │ HTTP 拦截器  │                │
│  └────────────┘  └─────────────┘  └──────────────┘                │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                         网关层                              │
│  ┌────────────┐  ┌─────────────┐                                     │
│  │  Sa-Token   │  │ Filter       │                                     │
│  │  Session    │  │ Interceptor  │                                     │
│  │  tenantId=2 │  │              │                                     │
│  └────────────┘  └─────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                        应用层                               │
│  ┌────────────────────────────────────────────────┐           │
│  │    TenantLineInnerInterceptor                   │           │
│  │  ├─ getTenantId() → Sa-Token Session          │           │
│  │  ├─ ignoreTable() → 系统配置表               │           │
│  │  └─ 自动添加 WHERE tenant_id = ?               │           │
│  └────────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                         数据层                               │
│  ┌────────────────────────────────────────────────┐           │
│  │         MyBatis-Plus                           │           │
│  │  ├─ BaseMapper                                │           │
│  │  ├─ @TableLogic                              │           │
│  │  └─ 自动添加租户条件                           │           │
│  └────────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      数据库                                 │
│  ┌────────────────────────────────────────────────┐           │
│  │  sys_org                                   │           │
│  │  sys_user                                  │           │
│  │  sys_user_org                              │           │
│  │  WHERE tenant_id = 2  ← 自动添加                 │           │
│  └────────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. 测试验证

### 8.1 测试用例

| 测试场景 | 验证点 | 预期结果 |
|---------|--------|---------|
| 用户登录 | 不同租户用户登录 | Session 正确存储 tenantId |
| 查询组织树 | 只返回当前租户的组织 | 自动过滤其他租户数据 |
| 添加用户 | userId 正确关联当前租户 | 自动填充当前 tenantId |
| 跨租户访问 | 尝试访问其他租户数据 | 查询结果为空 |
| 数据插入 | 新数据自动关联当前租户 | tenant_id 自动填充 |

### 8.2 验证方法

1. **查看 SQL 日志**：确认自动添加的 `tenant_id` 条件
2. **多租户测试**：创建多个租户，验证数据隔离
3. **安全测试**：尝试手动篡改 tenantId，验证被拦截

---

## 9. 常见问题

### Q1: 为什么登录时需要手动查询？

**A**: 登录时 Sa-Token Session 还未建立，无法自动获取租户ID。因此需要手动从租户编码获取 tenantId，然后查询用户。

### Q2: 如何临时禁用多租户插件？

**A**: 在 `TenantLineHandler.getTenantId()` 中返回 `null`：
```java
@Override
public Expression getTenantId() {
    return null;  // 临时禁用
}
```

### Q3: 如何添加新的忽略表？

**A**: 在 `ignoreTable()` 方法中添加：
```java
return "sys_tenant".equalsIgnoreCase(tableName)
        || "sys_dict".equalsIgnoreCase(tableName)
        || "your_table_name".equalsIgnoreCase(tableName);  // 新增
```

### Q4: 前端还需要传递 tenantId 吗？

**A**: 不需要！后端多租户插件会自动从 Sa-Token Session 获取租户ID。

### Q5: Long 类型如何避免精度丢失？

**A**: 已配置 Jackson 将 Long 序列化为 String，前端会收到字符串类型的 ID。

---

## 10. 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2025-01-28 | 初始版本，实现多租户自动隔离 |

---

## 11. 相关文档

- [MyBatis-Plus 多租户插件文档](https://baomidou.com/pages/guides/multi-tenancy.html)
- [Sa-Token 认证授权框架文档](https://sa-token.dev/)
- [Jackson 序列化配置文档](https://docs.spring.io/spring-boot/docs/reference/web/serialization.html)

---

## 12. 附录

### A. 完整配置文件

参见：
- `yuncode-admin/src/main/java/com/yuncode/admin/config/MyBatisPlusConfig.java`
- `yuncode-admin/src/main/java/com/yuncode/admin/config/JacksonConfig.java`

### B. 实体类配置示例

参见：
- `yuncode-system/src/main/java/com/yuncode/system/entity/SysOrg.java`
- `yuncode-system/src/main/java/com/yuncode/system/entity/SysUser.java`
- `yuncode-system/src/main/java/com/yuncode/system/entity/SysUserOrg.java`

### C. Mapper 接口示例

参见：
- `yuncode-system/src/main/java/com/yuncode/system/mapper/SysUserMapper.java`（包含登录专用方法）

---

**文档结束**
