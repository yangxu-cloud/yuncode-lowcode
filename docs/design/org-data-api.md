# 组织架构树数据接口定义

## 概述

组织架构树接口用于统一返回组织、部门和人员的树形结构数据。该接口需要明确区分三种节点类型：`company`（公司）、`department`（部门）、`user`（用户），并为每种类型定义相应的字段和操作权限。

## 数据类型定义

### 节点类型枚举

```typescript
enum NodeType {
  ROOT = "root",           // 根节点（虚拟节点，不对应数据库记录）
  COMPANY = "company",     // 公司
  DEPARTMENT = "department", // 部门
  USER = "user"            // 用户
}
```

## 树节点接口定义

### TypeScript 接口

```typescript
/**
 * 组织架构树节点
 */
interface OrgTreeNode {
  /** 节点ID */
  id: number;

  /** 节点类型 */
  nodeType: NodeType;

  /** 显示名称 */
  label: string;

  /** 父节点ID */
  parentId: number;

  /** 层级深度（从0开始，根节点为0） */
  level: number;

  /** 排序号 */
  sortOrder: number;

  /** 子节点列表 */
  children?: OrgTreeNode[];

  // ========== 公司/部门 特有字段 ==========
  /** 组织编码（当 nodeType 为 company 或 department 时必填） */
  orgCode?: string;

  /** 组织类型（1=公司 2=部门） */
  orgType?: number;

  /** 是否为公司（0=否 1=是） */
  isCompany?: number;

  /** 状态（0=禁用 1=启用） */
  status?: number;

  /** 备注 */
  remark?: string;

  // ========== 公司 特有字段 ==========
  /** 公司全称 */
  companyName?: string;

  /** 统一社会信用代码 */
  creditCode?: string;

  /** 公司地址 */
  companyAddress?: string;

  /** 公司网址 */
  companyUrl?: string;

  /** 法定代表人 */
  legalPerson?: string;

  /** 成立日期 */
  establishedDate?: string;

  /** 注册资本 */
  registeredCapital?: number;

  // ========== 用户特有字段 ==========
  /** 用户ID（当 nodeType 为 user 时必填） */
  userId?: number;

  /** 用户名 */
  username?: string;

  /** 昵称 */
  nickname?: string;

  /** 头像 */
  avatar?: string;

  /** 邮箱 */
  email?: string;

  /** 手机号 */
  mobile?: string;

  /** 是否为部门负责人（0=否 1=是） */
  isLeader?: number;

  /** 员工编号 */
  employeeNo?: string;

  /** 职位 */
  position?: string;

  /** 入职日期 */
  hireDate?: string;

  /** 用户状态 */
  userStatus?: number;
}
```

## 后端接口定义

### 请求接口

**GET** `/api/org/tree`

### 请求参数

```typescript
interface OrgTreeQuery {
  /** 租户ID（多租户标识，必填） */
  tenantId: number;

  /** 是否包含人员（true=包含 false=不包含，默认 true） */
  includeUsers?: boolean;

  /** 组织状态筛选（可选） */
  status?: number;

  /** 是否只返回启用的组织（可选） */
  enabledOnly?: boolean;
}
```

### 响应数据

```typescript
type OrgTreeResponse = OrgTreeNode[];
```

### 响应示例

```json
[
  {
    "id": 1,
    "nodeType": "root",
    "label": "组织架构",
    "parentId": 0,
    "level": 0,
    "sortOrder": 1,
    "children": [
      {
        "id": 2,
        "nodeType": "company",
        "label": "总公司",
        "parentId": 1,
        "level": 1,
        "sortOrder": 1,
        "orgCode": "HQ",
        "orgType": 1,
        "isCompany": 1,
        "status": 1,
        "companyName": "XX科技有限公司",
        "creditCode": "91110000XXXX",
        "legalPerson": "张三",
        "registeredCapital": 10000000,
        "children": [
          {
            "id": 4,
            "nodeType": "department",
            "label": "技术部",
            "parentId": 2,
            "level": 2,
            "sortOrder": 1,
            "orgCode": "TECH",
            "orgType": 2,
            "isCompany": 0,
            "status": 1,
            "children": [
              {
                "id": 101,
                "nodeType": "user",
                "label": "张三",
                "parentId": 4,
                "level": 3,
                "sortOrder": 1,
                "userId": 101,
                "username": "zhangsan",
                "nickname": "张三",
                "email": "zhangsan@company.com",
                "mobile": "13800138000",
                "isLeader": 1,
                "position": "技术总监",
                "hireDate": "2023-01-01",
                "userStatus": 1
              }
            ]
          }
        ]
      }
    ]
  }
]
```

## 节点类型与操作权限矩阵

### 根节点（root）

**特征**：
- `nodeType = "root"`
- `parentId = 0`
- `level = 0`

**允许的操作**：
- ✅ 添加公司（添加子节点，nodeType 固定为 company）
- ❌ 添加人员
- ❌ 编辑
- ❌ 删除

**业务规则**：
- 虚拟节点，不存储在数据库
- 仅用于树形结构的顶层展示

### 公司节点（company）

**特征**：
- `nodeType = "company"`
- `orgType = 1`
- `isCompany = 1`

**允许的操作**：
- ✅ 添加部门（添加子节点，nodeType 为 department）
- ✅ 添加人员
- ✅ 编辑公司信息
- ✅ 删除公司（级联删除所有子节点和人员）
- ❌ 移动到其他公司下

**业务规则**：
- 只能在根节点（root）下创建
- 不能作为其他公司的子部门
- 可以有多个部门和人员

### 部门节点（department）

**特征**：
- `nodeType = "department"`
- `orgType = 2`
- `isCompany = 0`

**允许的操作**：
- ✅ 添加子部门
- ✅ 添加人员
- ✅ 编辑部门信息
- ✅ 删除部门（级联删除所有子节点和人员）
- ✅ 移动到其他公司/部门下

**业务规则**：
- 可以在公司或其他部门下创建
- 可以有多个子部门和人员
- 不能作为根节点的直接子节点

### 用户节点（user）

**特征**：
- `nodeType = "user"`
- 有 `userId` 字段

**允许的操作**：
- ✅ 查看详情
- ✅ 编辑用户信息
- ✅ 移除组织关系（删除该用户与当前组织的关联）
- ✅ 移动到其他部门（修改所属部门）

**业务规则**：
- 属于叶子节点，不能有子节点
- 可以属于多个部门（通过 sys_user_org 表关联）
- 其中一个部门为主部门（is_main_dept = 1）

## 前端使用规范

### 1. 图标映射

```typescript
const getNodeIcon = (nodeType: NodeType, orgType?: number) => {
  switch (nodeType) {
    case 'root':
      return 'Folder';           // 文件夹图标
    case 'company':
      return 'Location';         // 地球图标
    case 'department':
      return 'OfficeBuilding';   // 办公楼图标
    case 'user':
      return 'User';              // 用户图标
    default:
      return 'Document';
  }
};
```

### 2. 节点类型判断

```typescript
// 判断是否为根节点
const isRootNode = (node: OrgTreeNode): boolean => {
  return node.nodeType === 'root';
};

// 判断是否为公司
const isCompany = (node: OrgTreeNode): boolean => {
  return node.nodeType === 'company';
};

// 判断是否为部门
const isDepartment = (node: OrgTreeNode): boolean => {
  return node.nodeType === 'department';
};

// 判断是否为用户
const isUser = (node: OrgTreeNode): boolean => {
  return node.nodeType === 'user';
};
```

### 3. 操作按钮显示逻辑

```typescript
interface NodeOperations {
  canAddSubOrg: boolean;    // 可添加子组织
  canAddUser: boolean;      // 可添加人员
  canEdit: boolean;         // 可编辑
  canDelete: boolean;       // 可删除
  canMove: boolean;         // 可移动
}

const getNodeOperations = (node: OrgTreeNode): NodeOperations => {
  const ops: NodeOperations = {
    canAddSubOrg: false,
    canAddUser: false,
    canEdit: false,
    canDelete: false,
    canMove: false
  };

  switch (node.nodeType) {
    case 'root':
      ops.canAddSubOrg = true; // 只能添加公司
      break;

    case 'company':
      ops.canAddSubOrg = true; // 可添加部门
      ops.canAddUser = true;
      ops.canEdit = true;
      ops.canDelete = true;
      break;

    case 'department':
      ops.canAddSubOrg = true; // 可添加子部门
      ops.canAddUser = true;
      ops.canEdit = true;
      ops.canDelete = true;
      ops.canMove = true;
      break;

    case 'user':
      ops.canEdit = true;      // 可编辑用户信息
      ops.canMove = true;      // 可移动到其他部门
      break;
  }

  return ops;
};
```

### 4. 级联选择器过滤

```typescript
// 上级组织选择时，只能选择组织和部门，不能选择用户
const buildParentOptions = (tree: OrgTreeNode[]): any[] => {
  const filterNode = (nodes: OrgTreeNode[]): any[] => {
    return nodes
      .filter(node => node.nodeType !== 'user') // 过滤用户节点
      .map(node => ({
        ...node,
        children: node.children ? filterNode(node.children) : []
      }));
  };

  return filterNode(tree);
};
```

## 数据库查询逻辑

### SQL 查询示例

```sql
-- 递归查询组织树（包含人员）
WITH RECURSIVE org_tree AS (
  -- 根节点（虚拟）
  SELECT
    0 AS id,
    'root' AS node_type,
    '组织架构' AS label,
    0 AS parent_id,
    0 AS level,
    1 AS sort_order,
    NULL AS org_code,
    NULL AS org_type,
    NULL AS is_company,
    1 AS status

  UNION ALL

  -- 组织节点
  SELECT
    o.id,
    CASE WHEN o.org_type = 1 THEN 'company' ELSE 'department' END AS node_type,
    o.org_name AS label,
    o.parent_id,
    p.level + 1 AS level,
    o.sort_order,
    o.org_code,
    o.org_type,
    o.is_company,
    o.status
  FROM sys_org o
  INNER JOIN org_tree p ON o.parent_id = p.id

  UNION ALL

  -- 用户节点（可选）
  SELECT
    u.id,
    'user' AS node_type,
    u.nickname AS label,
    uo.org_id AS parent_id,
    p.level + 1 AS level,
    0 AS sort_order,
    NULL AS org_code,
    NULL AS org_type,
    NULL AS is_company,
    u.status AS status
  FROM sys_user u
  INNER JOIN sys_user_org uo ON u.id = uo.user_id
  INNER JOIN org_tree p ON uo.org_id = p.id
  WHERE uo.is_main_dept = 1  -- 只显示主部门关系
)
SELECT * FROM org_tree
ORDER BY level, sort_order;
```

## 字段验证规则

### 创建/更新组织

```typescript
interface CreateOrgDTO {
  nodeType: 'company' | 'department';
  orgName: string;           // 必填，2-100字符
  orgCode: string;           // 必填，2-50字符，全局唯一
  parentId: number;          // 必填，父节点ID
  orgType: number;           // 1=公司 2=部门
  isCompany: number;         // 0=否 1=是
  sortOrder: number;         // 0-9999
  status: number;            // 0=禁用 1=启用
  remark?: string;           // 可选，0-500字符

  // 公司特有字段（当 nodeType=company 时）
  companyName?: string;
  creditCode?: string;
  legalPerson?: string;
  registeredCapital?: number;
}

interface UpdateOrgDTO extends CreateOrgDTO {
  id: number;                // 必填，组织ID
}
```

### 移动组织

```typescript
interface MoveOrgDTO {
  orgId: number;             // 必填，要移动的组织ID
  newParentId: number;       // 必填，新父节点ID

  // 业务规则验证：
  // 1. 公司不能移动到其他组织下
  // 2. 移动时需保持树形结构合法
}
```

## 错误码定义

| 错误码 | 说明 |
|-------|------|
| 1001 | 组织编码已存在 |
| 1002 | 父组织不存在 |
| 1003 | 不能移动到自身或子节点下 |
| 1004 | 公司不能作为其他组织的子部门 |
| 1005 | 不能删除根节点 |
| 1006 | 组织下还有子节点，不能删除 |
| 1007 | 组织下还有人员，不能删除 |
| 1008 | 用户不存在 |
| 1009 | 部门不存在 |

## 扩展功能

### 1. 节点展开状态管理

前端可根据 `level` 和 `children` 数量决定节点的展开策略：

```typescript
const shouldExpandNode = (node: OrgTreeNode): boolean => {
  // 自动展开规则
  return node.level < 2 ||  // 前2层自动展开
         (node.children?.length || 0) <= 5;  // 子节点少于5个时展开
};
```

### 2. 节点搜索高亮

```typescript
const searchInTree = (
  tree: OrgTreeNode[],
  keyword: string
): OrgTreeNode[] => {
  const results: OrgTreeNode[] = [];

  const search = (nodes: OrgTreeNode[]) => {
    for (const node of nodes) {
      // 检查当前节点是否匹配
      const isMatch = node.label.toLowerCase().includes(keyword.toLowerCase());

      // 递归搜索子节点
      const matchedChildren = node.children ? search(node.children) : [];

      if (isMatch || matchedChildren.length > 0) {
        results.push({
          ...node,
          children: matchedChildren
        });
      }
    }
    return results;
  };

  return search(tree);
};
```

### 3. 节点统计信息

```typescript
interface NodeStats {
  totalCompanies: number;
  totalDepartments: number;
  totalUsers: number;
}

const getTreeStats = (tree: OrgTreeNode[]): NodeStats => {
  const stats: NodeStats = {
    totalCompanies: 0,
    totalDepartments: 0,
    totalUsers: 0
  };

  const count = (nodes: OrgTreeNode[]) => {
    for (const node of nodes) {
      if (node.nodeType === 'company') stats.totalCompanies++;
      if (node.nodeType === 'department') stats.totalDepartments++;
      if (node.nodeType === 'user') stats.totalUsers++;

      if (node.children) {
        count(node.children);
      }
    }
  };

  count(tree);
  return stats;
};
```

## 前端组件示例

### TreeNode 组件

```vue
<template>
  <el-tree
    :data="treeData"
    :props="treeProps"
    node-key="id"
    @node-click="handleNodeClick"
  >
    <template #default="{ node, data }">
      <div class="custom-tree-node">
        <div class="node-info">
          <!-- 图标 -->
          <el-icon class="node-icon">
            <component :is="getNodeIcon(data)" />
          </el-icon>

          <!-- 名称 -->
          <span class="node-label">{{ data.label }}</span>

          <!-- 标签 -->
          <el-tag v-if="data.nodeType !== 'root'" size="small" type="info">
            {{ getNodeTypeLabel(data.nodeType) }}
          </el-tag>

          <!-- 负责人标识 -->
          <el-tag v-if="data.nodeType === 'user' && data.isLeader === 1"
                 size="small" type="warning">
            负责人
          </el-tag>
        </div>

        <!-- 操作按钮 -->
        <div class="node-actions">
          <el-dropdown @command="handleAction">
            <el-icon><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <!-- 根据节点类型显示不同操作 -->
                <template v-if="getOperations(data).canAddSubOrg">
                  <el-dropdown-item command="addSubOrg">添加子组织</el-dropdown-item>
                </template>

                <template v-if="getOperations(data).canAddUser">
                  <el-dropdown-item command="addUser">添加人员</el-dropdown-item>
                </template>

                <template v-if="getOperations(data).canEdit">
                  <el-dropdown-item command="edit" divided>编辑</el-dropdown-item>
                </template>

                <template v-if="getOperations(data).canDelete">
                  <el-dropdown-item command="delete">删除</el-dropdown-item>
                </template>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </template>
  </el-tree>
</template>

<script setup lang="ts">
import type { OrgTreeNode, NodeType } from '@/api/org';

const getNodeIcon = (node: OrgTreeNode): string => {
  const iconMap = {
    root: 'Folder',
    company: 'Location',
    department: 'OfficeBuilding',
    user: 'User'
  };
  return iconMap[node.nodeType];
};

const getNodeTypeLabel = (nodeType: NodeType): string => {
  const labelMap = {
    root: '根节点',
    company: '公司',
    department: '部门',
    user: '用户'
  };
  return labelMap[nodeType];
};

const getOperations = (node: OrgTreeNode) => {
  // 实现上面定义的操作权限矩阵逻辑
  // ...
};
</script>
```

## 总结

### 关键要点

1. **节点类型明确区分**：使用 `nodeType` 字段明确标识节点类型
2. **操作权限控制**：根据节点类型动态控制可用操作
3. **图标映射规范**：不同节点类型使用不同图标，提升用户体验
4. **业务规则验证**：前后端都需要验证业务规则的合法性
5. **扩展性设计**：接口设计考虑了未来功能扩展的需求

### 后续优化

1. 添加节点搜索和过滤功能
2. 支持节点的批量操作
3. 实现节点的拖拽排序功能
4. 添加组织架构导入导出功能
5. 支持组织架构版本管理
