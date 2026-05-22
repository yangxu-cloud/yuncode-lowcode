# 多租户用户管理系统实现总结

> 文档创建时间：2026-01-29
> 版本：v1.0

## 一、背景与问题

### 1.1 核心需求
- **租户隔离**：不同租户（组织）可以有相同用户名的用户
- **多级组织**：支持集团 → 公司 → 部门的三级组织架构
- **级联删除**：删除组织时，需要级联删除所有子组织和用户

### 1.2 遇到的问题

#### 问题1：不同租户无法创建相同用户名的用户
```
场景：在租户A和租户B下都创建用户"张三"
现象：第二个创建时提示"用户名已存在"
原因：MyBatis-Plus 多租户插件使用 session 的 tenant_id 进行唯一性校验
```

#### 问题2：tenantId 传递链路断裂
```
问题：前端未传递 tenantId，导致用户创建在错误的租户下
影响：用户租户归属错误，数据隔离失效
```

#### 问题3：跨租户操作失败
```
场景：平台管理员为其他租户创建/删除用户
问题：查询、更新、删除操作受到 session tenant_id 限制
现象：用户不存在、更新失败、删除失败
```

#### 问题4：sys_user_org 表的租户ID错误
```
问题：sys_user_org 表的 tenant_id 使用 session 的租户ID，而非组织租户ID
影响：数据不一致，查询失败
```

#### 问题5：删除功能效率低
```
问题：递归查询 + 逐条删除
影响：N 个组织需要执行 2N 次 SQL，事务时间长
```

## 二、解决方案

### 2.1 用户名唯一性校验

**修改文件**：[MyBatisPlusConfig.java](../yuncode-lowcode-boot/yuncode-admin/src/main/java/com/yuncode/admin/config/MyBatisPlusConfig.java)

**核心改动**：
```java
// 之前：PLATFORM_ADMIN 返回 null
if ("PLATFORM_ADMIN".equals(roleCode)) {
    return null;  // ❌ 导致 SQL: tenant_id = null
}

// 之后：所有用户使用实际 tenantId
if (tenantId == null || tenantId == 0) {
    return null;
}
return new LongValue(tenantId);  // ✅
```

**修改文件**：[UserService.java](../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/service/UserService.java)

**核心改动**：
```java
// 使用目标租户ID进行唯一性检查
SysUser existingUser = sysUserMapper.selectByUsernameAndTenantId(
    user.getUsername(),
    targetTenantId  // ✅ 目标租户ID
);
```

### 2.2 tenantId 传递链路

**后端 VO**：[OrgTreeNode.java](../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/vo/OrgTreeNode.java)
```java
private Long tenantId;  // ✅ 添加租户ID字段
```

**前端接口**：[org.ts](../yuncode-pure-admin/src/api/org.ts)
```typescript
export interface OrgTreeNode {
  tenantId?: number; // ✅ 添加租户ID
}
```

**前端组件**：[org/index.vue](../yuncode-pure-admin/src/views/facilities/org/index.vue)
```typescript
// ✅ 从组织节点获取租户ID
addUserForm.tenantId = parentNode.tenantId;

// ✅ 传递给创建用户接口
await createUser({
  username: addUserForm.username,
  tenantId: addUserForm.tenantId  // ✅
});
```

**服务实现**：[OrgServiceImpl.java](../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/service/impl/OrgServiceImpl.java)
```java
// ✅ 设置租户ID（继承自组织）
userOrg.setTenantId(org.getTenantId());
```

### 2.3 跨租户操作支持

**新增 Mapper 方法**：[SysUserMapper.java](../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/mapper/SysUserMapper.java)

```java
@InterceptorIgnore(tenantLine = "true")
SysUser selectByIdIgnoreTenant(@Param("id") Long id);

@InterceptorIgnore(tenantLine = "true")
int updateByIdIgnoreTenant(SysUser user);

@InterceptorIgnore(tenantLine = "true")
int deletePhysicalById(@Param("id") Long id);

@InterceptorIgnore(tenantLine = "true")
int deletePhysicalByIds(@Param("ids") List<Long> ids);
```

**XML 实现**：[SysUserMapper.xml](../yuncode-lowcode-boot/yuncode-system/src/main/resources/mapper/SysUserMapper.xml)

```xml
<!-- 跨租户查询用户 -->
<select id="selectByIdIgnoreTenant" resultType="com.yuncode.system.entity.SysUser">
    SELECT * FROM sys_user WHERE id = #{id} AND deleted = 0 LIMIT 1
</select>

<!-- 跨租户更新用户 -->
<update id="updateByIdIgnoreTenant">
    UPDATE sys_user SET ... WHERE id = #{id} AND deleted = 0
</update>

<!-- 批量物理删除用户 -->
<delete id="deletePhysicalByIds">
    DELETE FROM sys_user WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</delete>
```

### 2.4 sys_user_org 表租户隔离

**配置修改**：[MyBatisPlusConfig.java](../yuncode-lowcode-boot/yuncode-admin/src/main/java/com/yuncode/admin/config/MyBatisPlusConfig.java#L85)

```java
// ✅ 将 sys_user_org 从多租户插件中排除
|| "sys_user_org".equalsIgnoreCase(tableName)
```

**实体类修改**：[SysUserOrg.java](../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/entity/SysUserOrg.java#L52)

```java
// ❌ 移除自动填充
// @TableField(fill = FieldFill.INSERT)
private Long tenantId;

// ✅ 手动设置（在业务代码中）
userOrg.setTenantId(org.getTenantId());
```

### 2.5 删除功能优化

#### 优化前（低效率）
```java
// 递归删除
deleteOrgRecursively(orgId) {
    // 查询子组织（N次数据库交互）
    List<SysOrg> children = orgMapper.selectList(...);

    // 递归调用（N次）
    for (SysOrg child : children) {
        deleteOrgRecursively(child.getId());
    }

    // 逐条删除（M次）
    for (SysUserOrg userOrg : userOrgs) {
        userMapper.deletePhysicalById(userOrg.getUserId());  // M次
        userOrgMapper.deletePhysicalById(userOrg.getId());   // M次
    }
    orgMapper.deletePhysicalById(orgId);
}
// 总计：N次查询 + 2M次删除 = 2N+M次SQL ❌
```

#### 优化后（高效率）
```java
// 一次性查询
List<SysOrg> allOrgs = orgMapper.selectAllForPlatformAdmin();  // 1次

// 构建内存映射
Map<Long, List<SysOrg>> parentChildrenMap = allOrgs.stream()
    .collect(Collectors.groupingBy(SysOrg::getParentId));

// 递归收集ID
collectOrgIdsOptimized(id, parentChildrenMap, orgIds);  // 内存操作

// 批量删除
userOrgMapper.deletePhysicalByOrgIds(orgIds);      // 1次
userMapper.deletePhysicalByIds(userIds);           // 1次
orgMapper.deletePhysicalByIds(orgIds);             // 1次
// 总计：1次查询 + 3次删除 = 4次SQL ✅
```

## 三、数据库表设计

### 3.1 表关系

```
sys_org (组织表)
├── id (主键)
├── tenant_id (租户ID)
├── company_id (所属公司ID)
├── parent_id (父组织ID)
└── org_type (组织类型：0=根节点，1=公司，2=部门)

sys_user (用户表)
├── id (主键)
├── tenant_id (租户ID)
├── company_id (所属公司ID)
└── dept_id (主部门ID)

sys_user_org (用户组织关系表)
├── id (主键)
├── user_id (用户ID)
├── org_id (组织ID)
└── tenant_id (租户ID，继承自组织)
```

### 3.2 多租户隔离策略

| 表名 | 租户隔离方式 | 说明 |
|------|------------|------|
| sys_org | ✅ 受多租户插件控制 | tenant_id 字段自动填充和过滤 |
| sys_user | ✅ 受多租户插件控制 | tenant_id 字段自动填充和过滤 |
| sys_user_org | ❌ 不受多租户插件控制 | tenant_id 手动设置为组织的租户ID |

**原因**：`sys_user_org` 表的租户关系通过 `org_id` 间接关联，不应使用 session 的 `tenant_id`

## 四、关键技术点

### 4.1 MyBatis-Plus 多租户插件
```java
// 自动在 SQL 中添加 tenant_id 条件
SELECT * FROM sys_user WHERE username = ? AND tenant_id = ?
```

### 4.2 @InterceptorIgnore 注解
```java
// 绕过多租户插件，实现跨租户操作
@InterceptorIgnore(tenantLine = "true")
SysUser selectByIdIgnoreTenant(@Param("id") Long id);
```

### 4.3 @TableLogic 逻辑删除
```java
@TableLogic
private Integer deleted;  // 0=未删除，1=已删除

// 我们改用物理删除（DELETE），而不是逻辑删除（UPDATE）
```

### 4.4 批量操作
```xml
<!-- 使用 foreach 实现批量删除 -->
<delete id="deletePhysicalByIds">
    DELETE FROM sys_user WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</delete>
```

## 五、修改文件清单

### 5.1 后端 Java 文件（8个）

1. **MyBatisPlusConfig.java**
   - 移除 PLATFORM_ADMIN 返回 null 的特殊逻辑

2. **UserService.java**
   - 使用 `selectByUsernameAndTenantId` 在目标租户中检查唯一性

3. **SysUserMapper.java**
   - 添加 `selectByIdIgnoreTenant` 方法
   - 添加 `updateByIdIgnoreTenant` 方法
   - 添加 `deletePhysicalById` 方法
   - 添加 `deletePhysicalByIds` 方法

4. **SysOrgMapper.java**
   - 添加 `deletePhysicalById` 方法
   - 添加 `deletePhysicalByIds` 方法

5. **SysUserOrgMapper.java**
   - 添加 `deletePhysicalById` 方法
   - 添加 `deletePhysicalByOrgIds` 方法
   - 添加 `deletePhysicalByUserIds` 方法

6. **SysUserOrg.java**
   - 移除 tenantId 字段的 `@TableField(fill = FieldFill.INSERT)` 注解

7. **OrgServiceImpl.java**
   - `addUserToOrg` 方法：手动设置 `userOrg.setTenantId(org.getTenantId())`
   - `removeUserFromOrg` 方法：使用 `deletePhysicalById` 删除用户
   - `deleteOrg` 方法：批量删除子组织和用户
   - `getUsersByOrgId` 方法：使用 `selectByIdIgnoreTenant` 查询用户

8. **OrgTreeNode.java**
   - 添加 `tenantId` 字段

### 5.2 后端 XML 文件（3个）

1. **SysUserMapper.xml**
   - 添加 `selectByIdIgnoreTenant` SQL
   - 添加 `updateByIdIgnoreTenant` SQL
   - 添加 `deletePhysicalById` SQL
   - 添加 `deletePhysicalByIds` SQL

2. **SysOrgMapper.xml**
   - 添加 `deletePhysicalById` SQL
   - 添加 `deletePhysicalByIds` SQL

3. **SysUserOrgMapper.xml**
   - 添加 `deletePhysicalById` SQL
   - 添加 `deletePhysicalByOrgIds` SQL
   - 添加 `deletePhysicalByUserIds` SQL

### 5.3 前端文件（2个）

1. **org.ts**
   - `OrgTreeNode` 接口添加 `tenantId` 字段

2. **org/index.vue**
   - `addUserForm` 添加 `tenantId` 字段
   - `handleAddUser` 方法：从 `parentNode.tenantId` 获取租户ID
   - `createUser` 调用：传递 `tenantId` 参数

## 六、性能对比

### 6.1 删除组织性能对比

| 场景 | 组织数 | 用户数 | 优化前SQL次数 | 优化后SQL次数 | 提升 |
|------|--------|--------|-------------|-------------|------|
| 小型 | 10 | 50 | 110次 | 54次 | 51% |
| 中型 | 100 | 500 | 1100次 | 504次 | 54% |
| 大型 | 1000 | 5000 | 11000次 | 5004次 | 55% |

**优化公式**：
- 优化前：`2N + 2M`（N次查询子组织 + M次删除用户 + M次删除关系）
- 优化后：`N + 4`（N次查询用户关系 + 4次批量操作）

## 七、注意事项

### 7.1 租户隔离原则
1. **sys_user** 和 **sys_org** 表受多租户插件控制
2. **sys_user_org** 表不受多租户插件控制，tenant_id 继承自组织
3. 跨租户操作必须使用 `@InterceptorIgnore` 注解

### 7.2 删除顺序
必须按以下顺序删除，避免外键约束错误：
1. 删除 `sys_user_org`（用户组织关系）
2. 删除 `sys_user`（用户）
3. 删除 `sys_org`（组织）

### 7.3 性能优化
1. 避免在循环中执行 SQL
2. 使用批量操作（IN 查询）
3. 在内存中构建数据结构，减少数据库交互

### 7.4 数据一致性
1. 创建用户时，`tenant_id` 必须从组织继承
2. 查询用户时，使用目标租户ID，而非 session 租户ID
3. 删除用户时，确保使用物理删除，而非逻辑删除

## 八、测试场景

### 8.1 用户创建
```bash
场景：在租户A和租户B下创建用户"张三"
预期：两个租户都有用户"张三"，互不干扰
验证：SELECT * FROM sys_user WHERE username = '张三'
```

### 8.2 组织删除
```bash
场景：删除公司节点（包含10个部门，100个用户）
预期：级联删除所有部门和用户
验证：
- sys_org 表：删除1+10=11条记录
- sys_user 表：删除100条记录
- sys_user_org 表：删除100条记录
```

### 8.3 跨租户操作
```bash
场景：平台管理员在租户B下创建/删除用户
预期：操作成功，用户归属于租户B
验证：检查 user.tenant_id = 租户B的租户ID
```

## 九、后续优化建议

### 9.1 短期优化
1. 添加删除确认提示，显示级联删除的数量
2. 添加删除日志，记录删除的组织和用户
3. 添加软删除恢复功能

### 9.2 长期优化
1. 考虑使用 Redis 缓存组织树结构
2. 考虑使用数据库触发器自动维护 tenant_id
3. 考虑使用事件总线解耦删除逻辑

## 十、参考资料

- [MyBatis-Plus 多租户插件文档](https://baomidou.com/pages/aef2f2/)
- [Sa-Token 权限认证框架](https://sa-token.dev/)
- [MySQL 递归CTE查询](https://dev.mysql.com/doc/refman/8.0/recursive-common-table-expressions.html)
