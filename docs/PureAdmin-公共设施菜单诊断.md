# PureAdmin 公共设施菜单诊断

## 问题现象

用户在前端页面看不到"公共设施"菜单

## 已确认的实现

### 1. 路由配置 ✅
**文件**: [yuncode-pure-admin/src/router/modules/commons.ts](../../yuncode-pure-admin/src/router/modules/commons.ts)

```typescript
const commonsRoutes: RouteRecordRaw = {
  path: "/commons",
  name: "Commons",
  component: Layout,
  redirect: "/commons/org",
  meta: {
    icon: "ep/menu",
    title: "routes.commons",
    i18nKey: "routes.commons",
    rank: 3
  },
  children: [
    {
      path: "/commons/org",
      name: "Org",
      component: () => import("@/views/commons/org/index.vue"),
      meta: {
        icon: "ep/office-building",
        title: "routes.org",
        i18nKey: "routes.org",
        showLink: true
      }
    }
  ]
}
```

**PureAdmin 自动导入机制**：
- [router/index.ts](../../yuncode-pure-admin/src/router/index.ts) 第37-42行使用 `import.meta.glob` 自动导入所有 `modules/**/*.ts` 文件
- `commons.ts` 已被自动导入并注册到路由系统

### 2. 视图文件 ✅
**文件**: [yuncode-pure-admin/src/views/commons/org/index.vue](../../yuncode-pure-admin/src/views/commons/org/index.vue)
- 文件存在
- 文件大小：16480 字节
- 包含完整的组织管理界面

### 3. 国际化配置 ✅
**文件**: [yuncode-pure-admin/src/locales/zh-CN.ts](../../yuncode-pure-admin/src/locales/zh-CN.ts)

```typescript
routes: {
  commons: "公共设施",  // 第34行
  org: "组织管理"       // 第35行
}
```

### 4. 前端服务 ✅
- 服务已启动: `http://localhost:8849/`
- Vite v7.3.1 运行正常

## 可能的原因

### 原因1: 权限过滤 ⚠️

PureAdmin 有权限过滤机制，可能会过滤掉某些菜单。

**检查方法**：
1. 打开浏览器控制台 (F12)
2. 输入以下代码查看所有静态菜单：
```javascript
console.log(window.$stores?.permission?.constantMenus)
```

**预期结果**：应该能看到包含 `commons` 的菜单数组

### 原因2: 菜单被隐藏 ⚠️

检查 `meta.showLink` 是否为 `false`。

**当前配置**：`showLink: true` ✅

### 原因3: Rank 排序问题 ⚠️

当前 `rank: 3`，可能菜单显示在其他位置。

**建议**：检查所有菜单项的 rank 值，确认"公共设施"应该在哪个位置。

### 原因4: 用户角色权限 ⚠️

PureAdmin 可能根据用户角色过滤菜单。

**解决方法**：
1. 检查当前登录用户的角色
2. 确认角色是否有访问"公共设施"的权限

## 调试步骤

### 步骤1: 直接访问路由

在浏览器中直接访问：`http://localhost:8849/#/commons/org`

**如果页面能正常显示**：
- 路由配置正确 ✅
- 视图文件正确 ✅
- 问题在于菜单生成

**如果页面404**：
- 路由没有正确注册 ❌
- 需要重启前端服务

### 步骤2: 检查路由器状态

在浏览器控制台执行：
```javascript
// 查看所有已注册的路由
console.log(router.getRoutes())

// 查找 commons 路由
console.log(router.hasRoute('Commons'))
console.log(router.hasRoute('Org'))
```

### 步骤3: 检查菜单状态

在浏览器控制台执行：
```javascript
// 查看静态菜单
const permissionStore = window.$stores?.permission
console.log('Constant Menus:', permissionStore?.constantMenus)

// 查找公共设施菜单
const commonsMenu = permissionStore?.constantMenus?.find(m => m.path === '/commons')
console.log('Commons Menu:', commonsMenu)
```

### 步骤4: 检查权限过滤

PureAdmin 使用 `filterNoPermissionTree` 过滤菜单。

**检查方法**：
1. 查看 [router/utils.ts](../../yuncode-pure-admin/src/router/utils.ts) 中的 `filterNoPermissionTree` 函数
2. 确认是否有额外的权限逻辑

## 快速修复方案

### 方案1: 移除权限过滤（临时）

如果 `filterNoPermissionTree` 过滤了菜单，可以临时修改路由配置：

**修改** [commons.ts](../../yuncode-pure-admin/src/router/modules/commons.ts):

```typescript
meta: {
  icon: "ep/menu",
  title: "routes.commons",
  i18nKey: "routes.commons",
  rank: 3,
  showLink: true,  // 确保显示
  // 添加以下属性绕过权限检查
  authority: []    // 空数组表示不需要权限
}
```

### 方案2: 检查用户权限配置

查看后端返回的用户权限配置，确保包含"公共设施"的访问权限。

### 方案3: 使用浏览器插件

安装 Vue DevTools 浏览器插件，可以：
- 查看完整的路由树
- 查看 Pinia store 状态
- 调试菜单生成逻辑

## 菜单结构

**预期菜单显示顺序**（根据 rank 值）：
```
1. 首页 (rank: 10)
2. 系统运维 (rank: ?)
3. 公共设施 (rank: 3) ← 可能在中间位置
4. 系统设置 (rank: ?)
```

## 验证清单

- [ ] 前端服务已启动 (http://localhost:8849/)
- [ ] 已登录系统
- [ ] 清除了浏览器缓存
- [ ] 直接访问 /commons/org 路由能正常显示
- [ ] 在控制台能看到 constantMenus 包含 commons
- [ ] 路由器中存在 Commons 和 Org 路由
- [ ] 当前用户角色有访问权限

## 下一步操作

1. **首先**：直接访问 `http://localhost:8849/#/commons/org` 确认路由是否工作
2. **然后**：在浏览器控制台执行调试步骤2-4
3. **最后**：根据调试结果选择对应的修复方案

## 技术支持

如果以上方法都无法解决问题，请提供以下信息：
1. 浏览器控制台的完整错误信息
2. `console.log(router.getRoutes())` 的输出
3. `console.log(permissionStore?.constantMenus)` 的输出
4. 当前登录用户的角色信息
