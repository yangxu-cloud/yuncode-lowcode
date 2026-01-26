# 组织管理菜单实现

## 概述

组织管理功能已集成到系统管理模块中，作为"公共设施"子菜单的一部分。

## 菜单结构

### 主布局菜单
位置：[yuncode-lowcode-admin/src/views/layout/index.vue](../../yuncode-lowcode-admin/src/views/layout/index.vue)

```
主菜单
├── 首页 (/home)
├── 系统管理 (/system)
└── 系统设置 (/settings)
```

### 系统管理内部菜单
位置：[yuncode-lowcode-admin/src/views/system/index.vue](../../yuncode-lowcode-admin/src/views/system/index.vue)

```
系统管理
├── 日志管理 (/system/logs)
├── 在线用户 (/system/online-users)
└── 公共设施
    └── 组织管理 (/system/commons/org)
```

## 路由配置

位置：[yuncode-lowcode-admin/src/router/index.ts](../../yuncode-lowcode-admin/src/router/index.ts)

```typescript
{
  path: "/system",
  name: "System",
  component: () => import("@/views/system/index.vue"),
  redirect: "/system/logs",
  meta: {
    title: "系统管理",
    requiresAuth: true
  },
  children: [
    {
      path: "/system/logs",
      name: "SystemLogs",
      component: () => import("@/views/system/LogManage.vue"),
      meta: { title: "日志管理", requiresAuth: true }
    },
    {
      path: "/system/online-users",
      name: "OnlineUsers",
      component: () => import("@/views/system/OnlineUsers.vue"),
      meta: { title: "在线用户", requiresAuth: true }
    },
    {
      path: "/system/commons/org",
      name: "Org",
      component: () => import("@/views/commons/org/index.vue"),
      meta: { title: "组织管理", requiresAuth: true }
    }
  ]
}
```

## 国际化配置

### 中文 (zh-CN.ts)
```typescript
menu: {
  home: "首页",
  system: "系统管理",
  settings: "系统设置",
  logs: "日志管理",
  onlineUsers: "在线用户",
  commons: "公共设施",
  org: "组织管理"
}
```

### 英文 (en-US.ts)
```typescript
menu: {
  home: "Home",
  system: "System",
  settings: "Settings",
  logs: "Logs",
  onlineUsers: "Online Users",
  commons: "Common Facilities",
  org: "Organization Management"
}
```

## 页面组件

### 组织管理页面
位置：[yuncode-lowcode-admin/src/views/commons/org/index.vue](../../yuncode-lowcode-admin/src/views/commons/org/index.vue)

功能特性：
- 左侧组织架构树
- 右侧详细信息展示
- 支持组织和人员节点
- 搜索功能

当前状态：使用模拟数据，待接入后端 API

## 使用说明

1. 访问路径：点击"系统管理" → "公共设施" → "组织管理"
2. 直接访问 URL：`/system/commons/org`
3. 默认路由：`/system` 会自动重定向到 `/system/logs`

## 待完成功能

1. **后端 API 集成**
   - 获取组织树接口
   - 组织详情接口
   - 组织增删改查接口

2. **功能完善**
   - 添加组织功能
   - 编辑组织功能
   - 删除组织功能
   - 人员管理功能

3. **数据关联**
   - 关联公司信息
   - 关联部门信息
   - 关联用户信息

## 相关文档

- [公司管理功能实现总结](./公司管理功能实现总结.md)
- [组织服务数据库脚本](./组织服务数据库脚本.sql)
- [公司服务数据库脚本](./公司服务数据库脚本.sql)
