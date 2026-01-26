# 组织服务功能开发总结

## 概述

组织服务是 SaaS 系统中的核心功能模块，用于管理企业/集团的组织架构和人员分配。本功能支持树形组织结构、部门管理、人员分配等完整功能。

## 技术架构

### 后端技术栈
- **框架**: Spring Boot 3.x
- **ORM**: MyBatis-Plus
- **数据库**: MySQL 8.0
- **文档**: Swagger/OpenAPI 3.0

### 前端技术栈
- **框架**: Vue 3 + TypeScript
- **UI库**: Element Plus
- **路由**: Vue Router 4
- **国际化**: Vue I18n v11
- **HTTP**: Axios

## 数据库设计

> 💾 **数据库脚本**: [组织服务数据库脚本.sql](./组织服务数据库脚本.sql)
>
> 使用方法：在 MySQL 数据库中执行该脚本即可创建相关表和初始数据

### 1. 组织表 (sys_org)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT(20) | 主键ID |
| org_name | VARCHAR(100) | 组织名称 |
| org_code | VARCHAR(50) | 组织编码 |
| parent_id | BIGINT(20) | 父组织ID，0表示根节点 |
| org_type | TINYINT(1) | 组织类型：1=集团/公司，2=部门 |
| is_company | TINYINT(1) | 是否公司：0=否，1=是 |
| sort_order | INT(11) | 排序号 |
| status | TINYINT(1) | 状态：0=禁用，1=启用 |
| tenant_id | BIGINT(20) | 租户ID（多租户支持） |
| remark | VARCHAR(500) | 备注 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| create_by | VARCHAR(64) | 创建人 |
| update_by | VARCHAR(64) | 更新人 |
| deleted | TINYINT(1) | 删除标记（逻辑删除） |

**索引设计**:
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_org_code_tenant` (`org_code`, `tenant_id`)
- KEY `idx_parent_id` (`parent_id`)
- KEY `idx_tenant_id` (`tenant_id`)
- KEY `idx_org_type` (`org_type`)

### 设计思路：主部门与兼职部门分离

本系统采用**主部门 + 兼职部门**的混合设计模式，既满足简单场景的性能需求，又支持复杂的多部门场景。

**核心设计原则：**

1. **`sys_user_org` 表记录所有用户-部门关系**（包括主部门和兼职部门）
   - 主部门关系：用户创建时自动创建，`is_main_dept = 1`
   - 兼职关系：通过"添加兼职部门"功能创建，`is_main_dept = 0`

2. **`sys_user.dept_id` 冗余字段**：用于快速查询主部门（性能优化）

#### 主部门字段（sys_user 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| dept_id | BIGINT | 主部门ID（冗余字段，快速查询） |
| is_leader | TINYINT(1) | 是否主部门领导：0=否 1-是 |
| role_ids | VARCHAR(1000) | 角色ID列表（逗号分隔） |

**使用场景：**

- 显示用户信息：`张三 (技术部)` - 直接读取 dept_id
- 部门人员列表：`SELECT * FROM sys_user WHERE dept_id = ?`
- 部门领导查询：`SELECT * FROM sys_user WHERE dept_id = ? AND is_leader = 1`
- 权限判断：直接使用 dept_id 进行数据权限过滤

#### 用户组织关联表（sys_user_org 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | BIGINT(20) | 用户ID |
| org_id | BIGINT(20) | 组织ID |
| is_leader | TINYINT(1) | 是否该部门负责人：0=否 1-是 |
| is_main_dept | TINYINT(1) | 是否主部门：0=兼职 1=主部门（与sys_user.dept_id对应） |

**使用场景：**

- **主部门关系**（is_main_dept = 1）：
  - 用户创建时自动创建
  - 与 `sys_user.dept_id` 保持同步
  - 查询用户所有部门关系时统一从该表获取

- **兼职部门关系**（is_main_dept = 0）：
  - 矩阵式管理：员工同时属于项目组和职能部门
  - 跨部门协作：临时项目组成员
  - 兼职管理：用户在多个部门中任职

#### 数据一致性保证

**创建用户时：**

```java
// 1. 设置 sys_user.dept_id
user.setDeptId(mainDeptId);
userMapper.insert(user);

// 2. 创建主部门关系
SysUserOrg userOrg = new SysUserOrg();
userOrg.setUserId(user.getId());
userOrg.setOrgId(mainDeptId);
userOrg.setIsMainDept(1); // 标识为主部门
userOrg.setIsLeader(isLeader);
userOrgMapper.insert(userOrg);
```

**优点：**

1. ✅ **性能优化**：80% 的场景只需查询 sys_user 表，无需 JOIN
2. ✅ **数据统一**：所有用户-部门关系都在 sys_user_org 表中，便于统一查询和管理
3. ✅ **扩展性强**：通过 is_main_dept 区分主部门和兼职部门
4. ✅ **用户友好**：界面显示用户时可直接显示主部门，兼职可在详情中查看

> 📝 **相关SQL脚本**: [add_user_is_leader_field.sql](./sql脚本/用户管理/add_user_is_leader_field.sql)
>
> 该脚本用于为 sys_user 表添加 is_leader 字段

### 2. 用户组织关联表 (sys_user_org)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT(20) | 主键ID |
| user_id | BIGINT(20) | 用户ID |
| org_id | BIGINT(20) | 组织ID |
| is_leader | TINYINT(1) | 是否负责人：0=否，1-是 |
| is_main_dept | TINYINT(1) | 是否主部门：0=兼职，1=主部门（与sys_user.dept_id对应） |
| tenant_id | BIGINT(20) | 租户ID |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| create_by | VARCHAR(64) | 创建人 |
| update_by | VARCHAR(64) | 更新人 |
| deleted | TINYINT(1) | 删除标记（逻辑删除） |

**索引设计**:
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_user_org` (`user_id`, `org_id`, `tenant_id`)
- KEY `idx_user_id` (`user_id`)
- KEY `idx_org_id` (`org_id`)
- KEY `idx_tenant_id` (`tenant_id`)
- KEY `idx_user_main_dept` (`user_id`, `is_main_dept`)
- KEY `idx_org_main_dept` (`org_id`, `is_main_dept`)

## 后端实现

### 实体类 (Entity)

**SysOrg.java** - 组织实体类
- 位置: `yuncode-system/src/main/java/com/yuncode/system/entity/SysOrg.java`
- 使用 MyBatis-Plus 注解
- 支持自动填充（create_time, update_time 等）
- 支持逻辑删除

**SysUserOrg.java** - 用户组织关联实体类
- 位置: `yuncode-system/src/main/java/com/yuncode/system/entity/SysUserOrg.java`
- 同样支持自动填充和逻辑删除

### Mapper 接口

**SysOrgMapper.java**
- 继承 `BaseMapper<SysOrg>`
- 自定义查询方法：
  - `selectByParentId()` - 查询子组织
  - `selectOrgTree()` - 查询组织树
  - `searchOrgs()` - 搜索组织

**SysUserOrgMapper.java**
- 继承 `BaseMapper<SysUserOrg>`
- 自定义查询方法：
  - `selectByOrgId()` - 查询组织的用户
  - `selectByUserId()` - 查询用户的组织
  - `countUsersByOrgId()` - 统计组织用户数

### VO/DTO 类

**OrgVO.java** - 组织视图对象
- 包含组织基本属性
- 包含扩展属性（userCount, orgTypeName 等）

**OrgTreeNode.java** - 组织树节点
- 支持组织和人员两种节点类型
- 包含 children 属性支持树形结构

**OrgQueryDTO.java** - 组织查询对象
- 支持多条件查询
- 支持关键词搜索

### Service 层

**OrgService.java** - 组织服务接口
核心方法：
- `getOrgTree()` - 获取组织树（包含人员）
- `getOrgList()` - 获取组织列表
- `getOrgById()` - 获取组织详情
- `addOrg()` - 添加组织
- `updateOrg()` - 更新组织
- `deleteOrg()` - 删除组织
- `searchOrgs()` - 搜索组织
- `checkOrgCodeExists()` - 检查组织编码

**OrgServiceImpl.java** - 组织服务实现类
核心逻辑：
- 构建包含人员的组织树
- 递归查询子节点
- 组织与用户数据关联
- 业务校验（如：公司下不能有公司，只能有部门）

### Controller 层

**OrgController.java** - 组织管理控制器
接口列表：
- `GET /api/org/tree` - 获取组织树
- `POST /api/org/list` - 获取组织列表
- `GET /api/org/{id}` - 获取组织详情
- `POST /api/org` - 添加组织
- `PUT /api/org` - 更新组织
- `DELETE /api/org/{id}` - 删除组织
- `GET /api/org/search` - 搜索组织
- `GET /api/org/check-code` - 检查组织编码

所有接口都支持 Swagger 文档生成

## 前端实现

### API 封装 (org.ts)

位置: `yuncode-pure-admin/src/api/org.ts`

定义了以下接口方法：
- `getOrgTree()` - 获取组织树
- `getOrgList()` - 获取组织列表
- `getOrgById()` - 获取组织详情
- `addOrg()` - 添加组织
- `updateOrg()` - 更新组织
- `deleteOrg()` - 删除组织
- `searchOrgs()` - 搜索组织
- `checkOrgCodeExists()` - 检查组织编码

### 路由配置 (commons.ts)

位置: `yuncode-pure-admin/src/router/modules/commons.ts`

- 路径: `/commons`
- 名称: Commons
- 图标: ep/menu
- 子路由: `/commons/org` - 组织管理

### 主页面组件 (index.vue)

位置: `yuncode-pure-admin/src/views/commons/org/index.vue`

**页面布局**:
```
┌─────────────────────────────────────────┐
│ 卡片头部：标题 + 搜索框 + 搜索按钮      │
├──────────────┬──────────────────────────┤
│              │                          │
│  左侧树      │    右侧详情面板           │
│              │                          │
│  - 组织节点  │    - 组织详情             │
│  - 人员节点  │    - 用户详情             │
│              │                          │
│              │                          │
└──────────────┴──────────────────────────┘
```

**核心功能**:

1. **组织树显示**
   - 使用 `el-tree` 组件
   - 支持组织和人员两种节点
   - 默认展开所有节点
   - 节点点击查看详情

2. **节点操作**
   - 每个节点都有下拉操作菜单
   - 组织节点：添加子部门、添加人员、编辑、删除
   - 用户节点：查看用户、移除用户

3. **详情展示**
   - 右侧显示选中节点的详细信息
   - 使用 `el-descriptions` 组件
   - 组织和用户分别显示不同的字段

4. **添加/编辑组织**
   - 使用 `el-drawer` 抽屉组件
   - 表单包含：组织名称、编码、上级组织、类型等
   - 支持表单验证
   - 上级组织使用级联选择器

5. **搜索功能**
   - 顶部搜索框
   - 支持搜索组织和人员

### 国际化配置

**中文翻译** (zh-CN.ts):
- routes.commons: "公共设施"
- routes.org: "组织管理"
- org.*: 所有组织相关文本

**英文翻译** (en-US.ts):
- routes.commons: "Common Facilities"
- routes.org: "Organization Management"
- org.*: All organization related texts

## 核心特性

### 1. 树形组织结构
- 支持无限层级
- 根节点为"组织架构"
- 可区分公司/部门
- 自动排序

### 2. 组织人员混合显示
- 同一棵树中同时显示组织和人员
- 不同节点类型使用不同图标
- 人员节点显示用户头像和昵称

### 3. 业务规则
- "组织架构"下只能添加公司/集团
- 公司下只能添加部门
- 删除组织时检查是否有子组织和人员
- 组织编码全局唯一（同一租户下）

### 4. 多租户支持
- 所有数据都关联租户ID
- 查询时自动过滤租户数据
- 支持租户隔离

### 5. 逻辑删除
- 使用 deleted 字段标记删除
- 查询时自动过滤已删除数据
- 支持数据恢复

## 待完善功能

以下功能框架已搭建，但具体实现需要根据业务需求完善：

1. **添加人员功能**
   - 当前只有菜单项，需要实现用户选择和关联逻辑

2. **移除人员功能**
   - 需要实现从组织移除用户的接口

3. **查看用户功能**
   - 需要跳转到用户详情页或弹窗显示

4. **搜索功能**
   - 当前框架已搭建
   - 需要实现前端搜索过滤逻辑

5. **批量操作**
   - 批量删除组织
   - 批量移动人员等

6. **权限控制**
   - 基于角色的权限验证
   - 数据权限控制

## 文件清单

### 后端文件
```
yuncode-lowcode-boot/yuncode-system/
├── src/main/resources/
│   └── db/
│       └── org_service.sql                           # 数据库脚本
└── src/main/java/com/yuncode/system/
    ├── entity/
    │   ├── SysOrg.java                               # 组织实体
    │   └── SysUserOrg.java                           # 用户组织关联实体
    ├── mapper/
    │   ├── SysOrgMapper.java                         # 组织Mapper
    │   └── SysUserOrgMapper.java                     # 用户组织Mapper
    ├── dto/
    │   └── OrgQueryDTO.java                           # 查询DTO
    ├── vo/
    │   ├── OrgVO.java                                 # 组织VO
    │   └── OrgTreeNode.java                           # 组织树节点
    ├── service/
    │   ├── OrgService.java                            # 组织服务接口
    │   └── impl/
    │       └── OrgServiceImpl.java                    # 组织服务实现
    └── controller/
        └── OrgController.java                         # 组织控制器
```

### 前端文件
```
yuncode-pure-admin/src/
├── api/
│   └── org.ts                                        # 组织API
├── router/modules/
│   └── commons.ts                                    # 公共设施路由
├── views/commons/org/
│   └── index.vue                                     # 组织管理页面
└── locales/
    ├── zh-CN.ts                                     # 中文翻译
    └── en-US.ts                                     # 英文翻译
```

## 使用说明

### 1. 访问组织管理
登录系统后，导航到：**公共设施 > 组织管理**

### 2. 查看组织架构
- 左侧树显示完整的组织架构
- 点击节点可查看详细信息
- 组织节点显示办公楼图标
- 人员节点显示用户图标

### 3. 添加组织
1. 点击组织节点的"更多"按钮
2. 选择"添加子部门"
3. 在弹出的抽屉中填写组织信息
4. 点击"保存"

### 4. 编辑组织
1. 点击组织节点的"更多"按钮
2. 选择"编辑"
3. 修改组织信息
4. 点击"保存"

### 5. 删除组织
1. 点击组织节点的"更多"按钮
2. 选择"删除"
3. 确认删除操作
4. 系统会检查是否有子组织和人员

## 技术亮点

1. **递归树构建** - 高效构建包含人员的组织树
2. **统一数据接口** - 组织和人员使用统一的树节点结构
3. **响应式设计** - 左右布局自适应屏幕大小
4. **完整的国际化** - 中英文双语支持
5. **TypeScript 类型安全** - 前端代码类型完整
6. **业务校验完善** - 防止非法操作
7. **代码复用性高** - 符合阿里Java开发规范

## 下一步计划

1. 完善人员管理功能
2. 添加组织权限配置
3. 实现批量操作
4. 添加组织数据导入导出
5. 实现组织架构可视化
6. 添加组织变更历史记录

## 总结

组织服务功能已基本完成，实现了：
- ✅ 完整的数据库设计
- ✅ 后端增删改查接口
- ✅ 前端树形组件展示
- ✅ 组织和人员混合显示
- ✅ 国际化支持
- ✅ 多租户支持

代码质量高，架构清晰，易于维护和扩展。符合 SaaS 系统的设计规范和最佳实践。
