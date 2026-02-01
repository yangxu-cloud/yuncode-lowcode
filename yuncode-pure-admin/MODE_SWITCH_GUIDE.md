# 数据源切换系统使用指南

## 📖 概述

本系统实现了**Mock 数据源**和**真实 API**的无缝切换，方便开发、测试和演示。

## ✨ 功能特性

- ✅ **一键切换**：在 Mock 和 API 之间自由切换
- ✅ **持久化配置**：通过 localStorage 保存选择
- ✅ **环境变量支持**：通过 `.env` 文件配置默认值
- ✅ **开发模式专属**：切换器仅在开发模式下显示
- ✅ **自动刷新**：切换后自动刷新页面应用配置

## 🚀 快速开始

### 1. 使用 API 适配器

在需要支持 Mock 切换的 API 调用中，使用适配器代替直接的 HTTP 请求：

```typescript
// ❌ 原来的方式（不支持切换）
import { getMenuTree } from '@/api/menu';
const { data } = await getMenuTree();

// ✅ 新方式（支持 Mock 切换）
import { getMenuTreeAdapter } from '@/api/menu-adapter';
const { data } = await getMenuTreeAdapter();
```

### 2. 创建 Mock 数据

在 `src/mock/` 目录下创建 Mock 数据文件：

```typescript
// src/mock/example.ts
export async function mockGetExampleData() {
  await new Promise(resolve => setTimeout(resolve, 300));
  return { data: [...] };
}
```

### 3. 使用适配器包装 API

```typescript
import { adapterGet } from '@/utils/request';
import { mockGetExampleData } from '@/mock/example';

export const getExampleData = () => {
  return adapterGet(
    '/api/example',
    mockGetExampleData
  );
};
```

## ⚙️ 配置方式

### 方式 1：环境变量（推荐用于构建）

在项目根目录创建 `.env` 文件：

```bash
# 开发环境使用 Mock 数据
VITE_DATA_SOURCE=mock

# 或使用真实 API
VITE_DATA_SOURCE=api
```

### 方式 2：运行时切换（仅开发模式）

1. 启动开发服务器后，在页面右侧会看到一个**垂直切换按钮**
2. 点击按钮，确认切换
3. 页面自动刷新，应用新配置

**颜色标识：**
- 🟣 **紫色渐变** = Mock 数据模式
- 🔴 **红色渐变** = API 模式

## 📁 文件结构

```
src/
├── config/
│   └── app.ts                    # 应用配置
├── mock/
│   ├── menu.ts                   # 菜单 Mock 数据
│   └── org.ts                    # 组织 Mock 数据
├── utils/
│   └── request.ts                # API 适配器
├── api/
│   └── menu-adapter.ts           # 适配器版 API
└── components/
    └── DataSourceSwitcher.vue    # 数据源切换组件
```

## 🎯 使用场景

### 开发阶段
```bash
# .env.development
VITE_DATA_SOURCE=mock
```
前端开发不依赖后端，独立完成功能开发。

### 联调阶段
```bash
# .env.development
VITE_DATA_SOURCE=api
```
切换到真实 API，与后端联调测试。

### GitHub 展示
```bash
# .env.production
VITE_DATA_SOURCE=mock
```
配置使用 Mock 数据，访客可直接体验界面效果。

## 🔧 API 适配器说明

适配器提供了 5 个常用方法：

| 方法 | 说明 | Mock 参数 |
|------|------|----------|
| `adapterGet` | GET 请求 | `mockFn?: () => Promise` |
| `adapterPost` | POST 请求 | `mockFn?: () => Promise` |
| `adapterPut` | PUT 请求 | `mockFn?: () => Promise` |
| `adapterDelete` | DELETE 请求 | `mockFn?: () => Promise` |
| `adapterRequest` | 通用请求 | `mockFn?: () => Promise` |

### 使用示例

```typescript
import { adapterGet, adapterPost } from '@/utils/request';

// GET 请求
export const getUserList = () => {
  return adapterGet('/api/user', async () => {
    return { data: [{ id: 1, name: '张三' }] };
  });
};

// POST 请求
export const createUser = (data: any) => {
  return adapterPost('/api/user', data, async () => {
    return { code: 200, message: '创建成功' };
  });
};
```

## 📝 注意事项

1. **生产环境禁用**：切换器仅在开发模式（`import.meta.env.DEV`）下显示
2. **数据一致性**：Mock 数据结构应与真实 API 保持一致
3. **延迟模拟**：Mock 函数中添加 `setTimeout` 模拟网络延迟（建议 200-500ms）
4. **类型定义**：复用 `@/api` 中的类型定义，确保类型安全

## 🎨 扩展 Mock 数据

### 添加新的 Mock 数据文件

```typescript
// src/mock/user.ts
export interface User {
  id: number;
  name: string;
  email: string;
}

export const mockUsers: User[] = [
  { id: 1, name: '张三', email: 'zhangsan@example.com' },
  { id: 2, name: '李四', email: 'lisi@example.com' }
];

export async function mockGetUsers() {
  await new Promise(resolve => setTimeout(resolve, 300));
  return { data: mockUsers };
}
```

### 创建对应的 API 适配器

```typescript
// src/api/user-adapter.ts
import { adapterGet } from '@/utils/request';
import { mockGetUsers } from '@/mock/user';

export const getUsersAdapter = () => {
  return adapterGet('/api/users', mockGetUsers);
};
```

## 🐛 调试技巧

1. **查看当前数据源**：打开浏览器控制台，查看 localStorage 中的 `data-source` 字段
2. **Mock 请求标识**：Mock 模式下的请求会在控制台输出 `[Mock]` 前缀
3. **强制刷新**：切换数据源后会自动刷新，也可手动按 `Ctrl + Shift + R`

## 📚 相关文档

- [PureAdmin 文档](https://yiming_chang.gitee.io/pure-admin-doc/)
- [Vite 环境变量](https://cn.vitejs.dev/guide/env-and-mode.html)
- [Element Plus](https://element-plus.org/)

---

**Created by:** Yuncode Team
**Last Updated:** 2025-02-01
