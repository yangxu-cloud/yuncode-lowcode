# 菜单结构说明

## 菜单架构

根据 skill 文档要求和项目实际配置，系统采用以下菜单结构：

### 1. 公共设施 (Common Facilities)
路径：`/facilities`
图标：`ep:management`
排名：3

**子菜单：**
- 组织管理 - `/facilities/org`
- 导航管理 - `/facilities/navigation`
- 角色管理 - `/facilities/role`
- 应用管理 - `/facilities/application`

### 2. 应用开发 (App Development)
路径：`/app-dev`
图标：`ep:monitor`
排名：4

**子菜单：**
- 应用管理 - `/app-dev/application` （复用同一个页面组件）
- 业务建模 - `/app-dev/modeling` （待开发）
- 定时任务 - `/app-dev/schedule` （待开发）
- 系统服务 - `/app-dev/system-service` （待开发）

## 路由文件

### 1. 公共设施路由
文件：`src/router/modules/facilities.ts`
- 定义公共设施菜单及其子菜单
- 路由使用 `/facilities` 前缀

### 2. 应用开发路由
文件：`src/router/modules/app-dev.ts`
- 定义应用开发菜单及其子菜单
- 路由使用 `/app-dev` 前缀

### 自动注册
路由通过 `import.meta.glob` 自动导入所有 `src/router/modules` 目录下的 `.ts` 文件（除了 `remaining.ts`），无需手动注册。

## 数据库菜单

应用开发菜单需要通过 SQL 脚本初始化：

```bash
mysql -u root -p yuncode_lowcode < sql/init_app_dev_menus.sql
```

该脚本创建：
- 一级菜单：应用开发 (ID: 200)
- 二级菜单：业务建模 (ID: 201)
- 二级菜单：定时任务 (ID: 202)
- 二级菜单：系统服务 (ID: 203)

**注意：** 应用管理菜单不需要在应用开发下重复创建，因为它已经存在于公共设施菜单中。

## 应用管理的双入口

应用管理功能可以从两个入口访问：
1. **公共设施 → 应用管理** - `/facilities/application`
2. **应用开发 → 应用管理** - `/app-dev/application`

两个入口使用同一个页面组件：`src/views/facilities/application/index.vue`

## 当前状态

### 已完成
✅ 公共设施菜单及其子菜单
✅ 应用开发路由配置
✅ 应用开发国际化翻译
✅ 应用管理 3.1 功能开发完成
✅ 业务建模占位页面
✅ 定时任务占位页面
✅ 系统服务占位页面

### 待开发
⏳ 业务建模功能
⏳ 定时任务功能
⏳ 系统服务功能

## 配置文件清单

### 前端文件
- `src/router/modules/facilities.ts` - 公共设施路由
- `src/router/modules/app-dev.ts` - 应用开发路由
- `src/views/facilities/application/index.vue` - 应用管理页面
- `src/views/app-dev/modeling/index.vue` - 业务建模占位页
- `src/views/app-dev/schedule/index.vue` - 定时任务占位页
- `src/views/app-dev/system-service/index.vue` - 系统服务占位页
- `src/locales/zh-CN.ts` - 中文翻译
- `src/locales/en-US.ts` - 英文翻译

### 后端文件
- `yuncode-lowcode-boot/sql/init_app_dev_menus.sql` - 应用开发菜单初始化脚本

## 使用说明

### 初始化应用开发菜单

1. 执行数据库 SQL：
   ```bash
   mysql -u root -p yuncode_lowcode < yuncode-lowcode-boot/sql/init_app_dev_menus.sql
   ```

2. 重启后端服务

3. 刷新前端页面（Ctrl+F5）

### 验证菜单

登录系统后，左侧菜单应该显示：
- 首页
- 运维管理
- 公共设施
  - 组织管理
  - 导航管理
  - 角色管理
  - 应用管理
- 应用开发
  - 应用管理
  - 业务建模
  - 定时任务
  - 系统服务

### 注意事项

1. **菜单权限**：确保当前登录用户有访问所有菜单的权限
2. **浏览器缓存**：如果菜单不显示，尝试清除浏览器缓存
3. **路由优先级**：两个菜单都显示在侧边栏，可以自由切换
4. **页面复用**：应用管理页面在两个菜单下共享，数据互通

## 故障排除

### 应用开发菜单不显示

1. 检查 SQL 是否正确执行
   ```sql
   SELECT * FROM sys_menu WHERE menu_name = '应用开发';
   ```
   应该有一条记录

2. 检查菜单权限
   ```sql
   SELECT * FROM sys_menu_permission WHERE menu_id IN (200, 201, 202, 203);
   ```
   如果没有记录，需要为当前角色分配权限

3. 检查前端路由文件
   确认 `src/router/modules/app-dev.ts` 文件存在

4. 清除浏览器缓存或使用无痕模式

### 公共设施菜单不显示

1. 检查 `src/router/modules/facilities.ts` 文件内容
2. 检查国际化文件中对应的翻译
3. 检查页面组件文件是否存在