# 应用开发菜单设置指南

## 问题描述
用户反馈看不到应用开发菜单。根据 skill 文档要求，应用开发应该作为一级菜单，包含应用管理、业务建模、定时任务、系统服务等四个业务。

## 解决方案

### 1. 数据库菜单初始化

执行以下 SQL 文件来初始化应用开发菜单：

```bash
mysql -u root -p yuncode_lowcode < sql/init_app_dev_menus.sql
```

该文件会：
- 创建一级菜单"应用开发"（ID: 100）
- 创建二级菜单"应用管理"（ID: 101）
- 创建二级菜单"业务建模"（ID: 102，预留）
- 创建二级菜单"定时任务"（ID: 103，预留）
- 创建二级菜单"系统服务"（ID: 104，预留）

### 2. 前端路由配置

路由文件：`src/router/modules/facilities.ts`

- 路由路径从 `/facilities` 改为 `/app-dev`
- 路由名称从 `Facilities` 改为 `AppDev`
- 路由图标从 `ep:management` 改为 `ep:monitor`
- 路由标题从 `routes.facilities` 改为 `routes.appDev`

### 3. 国际化配置

**中文 (zh-CN.ts):**
```typescript
appDev: "应用开发",
application: "应用管理",
modeling: "业务建模",
schedule: "定时任务",
systemService: "系统服务"
```

**英文 (en-US.ts):**
```typescript
appDev: "App Development",
application: "Application Management",
modeling: "Business Modeling",
schedule: "Scheduled Tasks",
systemService: "System Services"
```

### 4. 页面组件

已创建以下页面组件：
- `src/views/app-dev/modeling/index.vue` - 业务建模（占位页）
- `src/views/app-dev/schedule/index.vue` - 定时任务（占位页）
- `src/views/app-dev/system-service/index.vue` - 系统服务（占位页）

注意：应用管理页面仍使用 `src/views/facilities/application/index.vue`

## 菜单结构

```
应用开发 (App Development)
├── 应用管理 (Application Management)      /app-dev/application
├── 业务建模 (Business Modeling)          /app-dev/modeling      [待开发]
├── 定时任务 (Scheduled Tasks)            /app-dev/schedule       [待开发]
└── 系统服务 (System Services)            /app-dev/system-service [待开发]
```

## 使用步骤

1. **执行数据库 SQL**
   ```bash
   mysql -u root -p yuncode_lowcode < yuncode-lowcode-boot/sql/init_app_dev_menus.sql
   ```

2. **重启后端服务**
   - 重新启动 Spring Boot 应用

3. **刷新前端**
   - 清除浏览器缓存或使用 Ctrl+F5 强制刷新
   - 重新登录系统

4. **验证菜单显示**
   - 左侧菜单栏应该出现"应用开发"菜单
   - 展开后应该看到四个子菜单

## 故障排除

### 如果菜单仍不显示

1. **检查数据库**
   ```sql
   SELECT * FROM sys_menu WHERE parent_id = 100 ORDER BY sort_order;
   ```
   应该看到 4 条记录

2. **检查菜单权限**
   ```sql
   SELECT * FROM sys_menu_permission WHERE menu_id IN (100, 101, 102, 103, 104);
   ```
   如果没有权限记录，可能需要为当前角色分配权限

3. **检查用户权限**
   确认当前登录用户有访问这些菜单的权限

4. **清除前端缓存**
   - 删除 `node_modules/.vite` 目录
   - 重新启动前端开发服务

### 如果路由错误

1. **检查路由文件**
   确认 `src/router/modules/facilities.ts` 已正确更新

2. **检查导入路径**
   确认页面组件路径正确

3. **重启前端服务**
   - 停止当前服务 (Ctrl+C)
   - 重新运行 `npm run dev`

## 后续开发

### 业务建模 (待开发)
- 实体模型管理
- 数据模型设计
- 字段定义
- 关系定义

### 定时任务 (待开发)
- 任务调度
- Cron 表达式
- 任务执行历史
- 任务日志

### 系统服务 (待开发)
- 服务注册
- 服务发现
- 服务监控
- 配置管理

## 注意事项

1. **数据库菜单优先级**：如果系统使用数据库菜单，必须先执行 SQL 脚本
2. **菜单权限**：需要确保当前角色有访问应用开发菜单的权限
3. **路由一致性**：路由路径应与数据库中的 path 字段保持一致
4. **图标配置**：图标名称应与 Element Plus 图标库一致

## 相关文件

- SQL: `yuncode-lowcode-boot/sql/init_app_dev_menus.sql`
- 路由: `yuncode-pure-admin/src/router/modules/facilities.ts`
- 中文翻译: `yuncode-pure-admin/src/locales/zh-CN.ts`
- 英文翻译: `yuncode-pure-admin/src/locales/en-US.ts`