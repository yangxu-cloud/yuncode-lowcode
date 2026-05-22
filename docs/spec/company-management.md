# 公司管理功能实现总结

## 概述

公司管理是 SaaS 系统中的核心功能模块，用于支持多租户多公司的业务场景。一个租户可以管理多家公司，每家公司有独立的组织架构和人员管理。

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

> 💾 **数据库脚本**: [公司服务数据库脚本.sql](./公司服务数据库脚本.sql)
>
> 使用方法：在 MySQL 数据库中执行该脚本即可创建相关表和初始数据

### 1. 公司信息表 (sys_company)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT(20) | 主键ID |
| company_name | VARCHAR(100) | 公司名称 |
| company_code | VARCHAR(50) | 公司编码 |
| company_type | TINYINT(1) | 公司类型：1=有限公司，2=股份公司，3=个体工商户，4=其他 |
| credit_code | VARCHAR(50) | 统一社会信用代码 |
| legal_person | VARCHAR(50) | 法定代表人 |
| register_capital | DECIMAL(18,2) | 注册资本（万元） |
| establish_date | DATE | 成立日期 |
| register_address | VARCHAR(500) | 注册地址 |
| business_address | VARCHAR(500) | 经营地址 |
| business_scope | TEXT | 经营范围 |
| contact_phone | VARCHAR(20) | 联系电话 |
| contact_email | VARCHAR(100) | 联系邮箱 |
| business_license | VARCHAR(500) | 营业执照图片URL |
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
- UNIQUE KEY `uk_company_code_tenant` (`company_code`, `tenant_id`)
- KEY `idx_tenant_id` (`tenant_id`)
- KEY `idx_credit_code` (`credit_code`)
- KEY `idx_status` (`status`)

### 2. 关联字段说明

**sys_org 表添加字段：**
```sql
company_id BIGINT(20) DEFAULT NULL COMMENT '所属公司ID'
```

**sys_user 表添加字段：**
```sql
company_id BIGINT DEFAULT NULL COMMENT '所属公司ID'
```

## 设计思路：公司-组织-用户三层架构

### 核心设计原则

1. **sys_company 表**：存储公司基本信息（工商信息、营业执照等）
2. **sys_org 表**：存储组织架构，通过 `company_id` 关联公司
3. **sys_user 表**：存储用户信息，通过 `company_id` 和 `dept_id` 快速查询

### 数据模型关系

```
租户 (sys_tenant)
    │
    ├── 公司A (sys_company, tenant_id=1)
    │   ├── 组织架构1 (sys_org, company_id=1)
    │   │   ├── 技术部
    │   │   └── 市场部
    │   └── 人员 (sys_user, company_id=1, dept_id → sys_org)
    │
    └── 公司B (sys_company, tenant_id=1)
        ├── 组织架构2 (sys_org, company_id=2)
        │   ├── 研发部
        │   └── 销售部
        └── 人员 (sys_user, company_id=2, dept_id → sys_org)
```

### 冗余字段设计

**sys_user 表：**
- `company_id` - 所属公司ID（冗余字段，快速查询）
- `dept_id` - 主部门ID（冗余字段，快速查询）

**优点：**
1. ✅ **性能优化**：80% 的场景只需查询 sys_user 表，无需多次 JOIN
2. ✅ **业务友好**：按公司筛选、统计都很方便
3. ✅ **设计一致**：与 dept_id 设计思路完全一致

**查询示例：**
```sql
-- 查询用户所属公司（只需1次JOIN）
SELECT u.*, c.company_name
FROM sys_user u
LEFT JOIN sys_company c ON u.company_id = c.id
WHERE u.id = ?;

-- 如果没有 company_id，需要2次JOIN
SELECT u.*, c.company_name
FROM sys_user u
LEFT JOIN sys_org o ON u.dept_id = o.id
LEFT JOIN sys_company c ON o.company_id = c.id
WHERE u.id = ?;
```

## 后端实现

### 实体类 (Entity)

**SysCompany.java** - 公司实体类
- 位置: `yuncode-system/src/main/java/com/yuncode/system/entity/SysCompany.java`
- 使用 MyBatis-Plus 注解
- 支持自动填充（create_time, update_time 等）
- 支持逻辑删除

**SysOrg.java** - 组织实体类
- 添加了 `companyId` 字段

**SysUser.java** - 用户实体类
- 添加了 `companyId` 字段

### Mapper 接口

**CompanyMapper.java**
- 位置: `yuncode-system/src/main/java/com/yuncode/system/mapper/CompanyMapper.java`
- 继承 `BaseMapper<SysCompany>`
- 自定义查询方法：
  - `selectByTenantId()` - 查询租户下的公司列表
  - `selectByCode()` - 根据公司编码查询
  - `selectByCreditCode()` - 根据统一社会信用代码查询

### VO/DTO 类

**CompanyVO.java** - 公司视图对象
- 包含公司基本属性
- 包含扩展属性（companyTypeName, statusName, orgCount, userCount 等）

**CompanyQueryDTO.java** - 公司查询对象
- 支持多条件查询
- 支持关键词搜索

### Service 层

**CompanyService.java** - 公司服务接口
核心方法：
- `getCompanyList()` - 获取公司列表
- `getCompanyById()` - 获取公司详情
- `addCompany()` - 添加公司
- `updateCompany()` - 更新公司
- `deleteCompany()` - 删除公司
- `searchCompanies()` - 搜索公司
- `checkCompanyCodeExists()` - 检查公司编码是否存在

**CompanyServiceImpl.java** - 公司服务实现类
- 业务逻辑实现
- 数据校验
- 类型名称转换

### Controller 层

**CompanyController.java**
- 位置: `yuncode-system/src/main/java/com/yuncode/system/controller/CompanyController.java`
- RESTful API 接口
- Swagger 文档注解

**API 接口列表：**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /company/list | 获取公司列表 |
| GET | /company/{id} | 获取公司详情 |
| POST | /company | 添加公司 |
| PUT | /company | 更新公司 |
| DELETE | /company/{id} | 删除公司 |
| GET | /company/search | 搜索公司 |
| GET | /company/check-code | 检查公司编码 |

## 使用场景示例

### 场景1：集团公司
```
租户：XX集团（tenant_id=1）
  ↓
  公司1：XX科技有限公司（company_id=1, tenant_id=1）
  公司2：XX贸易有限公司（company_id=2, tenant_id=1）
  公司3：XX投资有限公司（company_id=3, tenant_id=1）
```

### 场景2：代理记账
```
租户：某代理记账公司（tenant_id=2）
  ↓
  客户公司A：某餐饮公司（company_id=10, tenant_id=2）
  客户公司B：某建筑公司（company_id=11, tenant_id=2）
  ...（可能管理几百家公司）
```

### 场景3：企业孵化器
```
租户：某创业孵化器（tenant_id=3）
  ↓
  入驻企业A：某科技公司（company_id=20, tenant_id=3）
  入驻企业B：某文化传媒公司（company_id=21, tenant_id=3）
  ...（服务几十家创业公司）
```

## 数据一致性保证

### 创建用户时
```java
// 通过选择的部门，获取公司信息
SysOrg dept = orgMapper.selectById(deptId);
Long companyId = dept.getCompanyId();

// 创建用户
SysUser user = new SysUser();
user.setCompanyId(companyId);  // 设置公司ID
user.setDeptId(deptId);        // 设置部门ID
userMapper.insert(user);

// 创建主部门关系
SysUserOrg userOrg = new SysUserOrg();
userOrg.setUserId(user.getId());
userOrg.setOrgId(deptId);
userOrg.setIsMainDept(1);
userOrgMapper.insert(userOrg);
```

### 变更部门时
```java
SysOrg newDept = orgMapper.selectById(newDeptId);

// 检查是否跨公司
if (!user.getCompanyId().equals(newDept.getCompanyId())) {
    throw new BusinessException("不能跨公司调动员工");
}

// 更新部门
user.setDeptId(newDeptId);
userMapper.updateById(user);
```

## 文件清单

### 后端文件

**实体类：**
- [SysCompany.java](../../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/entity/SysCompany.java)
- [SysOrg.java](../../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/entity/SysOrg.java) - 已添加 companyId
- [SysUser.java](../../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/entity/SysUser.java) - 已添加 companyId

**Mapper：**
- [CompanyMapper.java](../../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/mapper/CompanyMapper.java)

**Service：**
- [CompanyService.java](../../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/service/CompanyService.java)
- [CompanyServiceImpl.java](../../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/service/impl/CompanyServiceImpl.java)

**Controller：**
- [CompanyController.java](../../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/controller/CompanyController.java)

**VO/DTO：**
- [CompanyVO.java](../../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/vo/CompanyVO.java)
- [CompanyQueryDTO.java](../../yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/dto/CompanyQueryDTO.java)

### 数据库脚本

- [公司服务数据库脚本.sql](./公司服务数据库脚本.sql)

## 扩展功能

### 待实现功能

1. **前端管理界面**
   - 公司列表页面
   - 公司详情页面
   - 公司添加/编辑表单
   - 营业执照上传

2. **数据统计**
   - 统计各公司组织数量
   - 统计各公司人员数量
   - 公司维度的数据报表

3. **权限控制**
   - 按公司隔离数据权限
   - 管理员可查看所有公司
   - 普通用户只能查看所属公司

4. **高级功能**
   - 公司切换功能（多公司管理员）
   - 跨公司数据查询（集团管理员）
   - 公司数据导入导出

## 总结

公司管理功能为系统提供了完善的多公司支持，通过合理的表结构设计和冗余字段策略，既保证了查询性能，又确保了数据一致性。该设计适用于各种多租户、多公司的业务场景，如集团公司管理、代理记账、企业孵化器等。
