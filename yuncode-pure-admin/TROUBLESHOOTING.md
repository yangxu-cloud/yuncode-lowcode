# 数据源切换问题排查指南

## 🔍 问题：切换按钮没显示或不工作

### 步骤 1：检查开发模式

打开浏览器控制台（F12），查看是否有以下日志：
```
[DataSourceSwitcher] 组件已挂载
[DataSourceSwitcher] 是否开发模式: true
[DataSourceSwitcher] 允许切换: true
```

**如果没有这些日志**：说明组件没有正确加载

**如果 `是否开发模式: false`**：说明当前不是开发环境

### 步骤 2：检查 import.meta.env.DEV

在浏览器控制台执行：
```javascript
console.log('import.meta.env.DEV:', import.meta.env.DEV);
console.log('MODE:', import.meta.env.MODE);
```

- 如果显示 `false`，需要检查 Vite 配置
- 确保 `npm run dev` 启动的是开发模式

### 步骤 3：手动测试配置

在浏览器控制台执行以下代码：

```javascript
// 1. 测试配置文件
import('/src/config/app.js').then(m => {
  console.log('配置模块:', m);
});

// 2. 手动切换数据源
localStorage.setItem('data-source', 'mock');
console.log('已设置 Mock 模式，请刷新页面');

// 3. 查看当前配置
console.log('当前数据源:', localStorage.getItem('data-source'));
```

### 步骤 4：检查切换按钮

切换按钮应该在页面**右侧边缘**，是一个**垂直的长条按钮**：
- 🟣 紫色渐变 = Mock 模式
- 🔴 红色渐变 = API 模式

**如果没有看到按钮**：
1. 检查浏览器控制台是否有错误
2. 检查 [layout/index.vue](yuncode-pure-admin/src/layout/index.vue) 是否正确导入组件

### 步骤 5：测试 Mock 数据

手动切换到 Mock 模式：

```javascript
// 在浏览器控制台执行
localStorage.setItem('data-source', 'mock');
window.location.reload();
```

刷新后，导航管理页面应该显示 Mock 数据（办公、基础管理等）。

## 🔧 快速修复方案

### 方案 1：强制显示切换按钮

临时修改 [DataSourceSwitcher.vue](yuncode-pure-admin/src/components/DataSourceSwitcher.vue)，移除开发模式限制：

```vue
<div class="data-source-switcher">
  <!-- 移除 v-if="allowSwitch" -->
</div>
```

### 方案 2：直接修改配置

在浏览器控制台执行：
```javascript
// 切换到 Mock 模式
localStorage.setItem('data-source', 'mock');
location.reload();

// 切换回 API 模式
localStorage.setItem('data-source', 'api');
location.reload();
```

### 方案 3：检查组件导入

检查 [layout/index.vue](yuncode-pure-admin/src/layout/index.vue) 的第 32 行附近：

```typescript
import DataSourceSwitcher from "@/components/DataSourceSwitcher.vue";
```

确保这行代码存在且没有错误。

## 📸 截图需求

如果问题仍未解决，请提供以下截图：
1. 浏览器控制台的日志（包含所有错误和警告）
2. 页面右侧边缘的截图
3. 组件目录结构

## 💡 临时测试 Mock 数据

如果切换器暂时无法使用，可以直接使用 Mock 数据：

1. 打开 [.env.development](.env.development)
2. 修改：`VITE_DATA_SOURCE = mock`
3. 重启开发服务器：`npm run dev`
