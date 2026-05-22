# 启动前端服务查看公共设施菜单

## 问题描述

如果在前端看不到"公共设施"菜单，通常是因为前端开发服务器没有运行或需要重新编译。

## 解决步骤

### 1. 安装前端依赖

```bash
cd yuncode-lowcode-admin
npm install
```

等待安装完成，这可能需要几分钟时间。

### 2. 启动前端开发服务器

```bash
npm run dev
```

启动成功后，终端会显示类似以下信息：
```
VITE v5.x.x  ready in xxx ms

➜  Local:   http://localhost:5173/
➜  Network: use --host to expose
➜  press h + enter to show help
```

### 3. 访问页面

1. 打开浏览器，访问 `http://localhost:5173`
2. 使用管理员账号登录
3. 点击左侧菜单的"系统管理"
4. 在系统管理页面的左侧菜单中，应该能看到"公共设施"菜单项

## 如果仍然看不到菜单

### 方法1：清除浏览器缓存
1. 按 `Ctrl + Shift + Delete` 打开清除缓存对话框
2. 选择"缓存的图片和文件"
3. 点击"清除数据"
4. 刷新页面（`F5`）

### 方法2：硬刷新
- Windows: `Ctrl + F5`
- Mac: `Cmd + Shift + R`

### 方法3：检查浏览器控制台
1. 按 `F12` 打开开发者工具
2. 查看 Console 标签，看是否有错误信息
3. 如果有国际化相关的错误，检查翻译文件

## 验证代码

公共设施菜单已在以下文件中实现：

1. **[system/index.vue](../../yuncode-lowcode-admin/src/views/system/index.vue)** (第19-27行)
   ```vue
   <el-sub-menu index="commons">
     <template #title>
       <el-icon><OfficeBuilding /></el-icon>
       <span>{{ t('menu.commons') }}</span>
     </template>
     <el-menu-item index="/system/commons/org">
       <span>{{ t('menu.org') }}</span>
     </el-menu-item>
   </el-sub-menu>
   ```

2. **[router/index.ts](../../yuncode-lowcode-admin/src/router/index.ts)** (第74-79行)
   ```typescript
   {
     path: "/system/commons/org",
     name: "Org",
     component: () => import("@/views/commons/org/index.vue"),
     meta: { title: "组织管理", requiresAuth: true }
   }
   ```

3. **国际化配置**
   - [zh-CN.ts](../../yuncode-lowcode-admin/src/locales/zh-CN.ts) - 中文翻译
   - [en-US.ts](../../yuncode-lowcode-admin/src/locales/en-US.ts) - 英文翻译

## 菜单结构

```
系统管理
├── 日志管理 (/system/logs)
├── 在线用户 (/system/online-users)
└── 公共设施
    └── 组织管理 (/system/commons/org)
```

## 常见问题

**Q: 为什么菜单在主布局侧边栏中看不到？**

A: "公共设施"菜单不在主布局侧边栏中，而是在"系统管理"页面内部的左侧菜单中。点击主菜单的"系统管理"后，进入系统管理页面，在页面内部的左侧菜单中才能看到"公共设施"。

**Q: 点击"公共设施"后页面空白？**

A: 这是因为组织管理页面使用的是模拟数据，后端API还没有对接。这是正常的，待后端API开发完成后即可正常显示。
