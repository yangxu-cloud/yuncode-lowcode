# SQL 脚本索引

本目录包含系统各功能模块的数据库脚本，按功能分类组织。

## 📁 脚本分类

### 1. 日志管理

| 脚本文件 | 说明 | 依赖 |
|---------|------|------|
| [create_login_log_table.sql](./create_login_log_table.sql) | 创建登录日志表 | 无 |
| [create_system_log_table.sql](./create_system_log_table.sql) | 创建系统日志表 | 无 |
| [create_oper_log_table.sql](./create_oper_log_table.sql) | 创建操作日志表 | 无 |
| [update_login_log_add_trace_columns.sql](./update_login_log_add_trace_columns.sql) | 更新登录日志表添加链路追踪字段 | create_login_log_table.sql |
| [update_all_log_tables_add_trace_columns.sql](./update_all_log_tables_add_trace_columns.sql) | 批量更新所有日志表添加链路追踪字段 | 所有日志表 |
| [update_login_log_status.sql](./update_login_log_status.sql) | 更新登录日志状态字段 | create_login_log_table.sql |
| [update_system_log_table.sql](./update_system_log_table.sql) | 更新系统日志表结构 | create_system_log_table.sql |

**执行顺序**:
1. create_login_log_table.sql
2. create_system_log_table.sql
3. create_oper_log_table.sql
4. update_login_log_add_trace_columns.sql
5. update_all_log_tables_add_trace_columns.sql
6. update_login_log_status.sql
7. update_system_log_table.sql

**相关文档**: [平台日志处理方案.md](../平台日志处理方案.md)

---

### 2. 系统设置

| 脚本文件 | 说明 | 依赖 |
|---------|------|------|
| [create_sys_settings.sql](./create_sys_settings.sql) | 创建系统设置表 | 无 |

**执行顺序**:
1. create_sys_settings.sql

**相关文档**: 参见系统设置模块

---

### 3. 数据库初始化

| 脚本文件 | 说明 | 依赖 |
|---------|------|------|
| [init_database.sql](./init_database.sql) | 初始化数据库和基础表 | 无 |

**执行顺序**:
1. init_database.sql （首次安装时执行）

**说明**: 该脚本包含数据库创建和基础表结构，仅在全新安装时使用。

---

### 4. 用户管理

| 脚本文件 | 说明 | 依赖 |
|---------|------|------|
| [check_user.sql](./check_user.sql) | 检查用户信息 | 无 |
| [reset_admin_password.sql](./reset_admin_password.sql) | 重置管理员密码 | 无 |
| [fix_admin_password.sql](./fix_admin_password.sql) | 修复管理员密码 | 无 |
| [fix_system_admin_password.sql](./fix_system_admin_password.sql) | 修复系统管理员密码 | 无 |
| [update_system_admin_password.sql](./update_system_admin_password.sql) | 更新系统管理员密码 | 无 |

**执行顺序**:
- check_user.sql - 按需执行（检查用）
- reset_admin_password.sql - 按需执行（忘记密码时）
- fix_admin_password.sql - 按需执行（修复密码）
- fix_system_admin_password.sql - 按需执行（修复系统管理员密码）
- update_system_admin_password.sql - 按需执行（更新密码）

**说明**: 这些是维护脚本，只在特定场景下使用。

---

### 5. 组织服务

| 脚本文件 | 说明 | 依赖 |
|---------|------|------|
| [组织服务数据库脚本.sql](../组织服务数据库脚本.sql) | 组织服务相关表 | 无 |

**执行顺序**:
1. 组织服务数据库脚本.sql

**相关文档**: [组织服务功能实现总结.md](../组织服务功能实现总结.md)

---

## 🚀 使用说明

### 首次安装

首次安装系统时，按照以下顺序执行脚本：

1. **初始化数据库**
   ```bash
   mysql -u root -p < init_database.sql
   ```

2. **创建日志表**
   ```bash
   mysql -u root -p < create_login_log_table.sql
   mysql -u root -p < create_system_log_table.sql
   mysql -u root -p < create_oper_log_table.sql
   ```

3. **创建系统设置表**
   ```bash
   mysql -u root -p < create_sys_settings.sql
   ```

4. **创建组织服务表**
   ```bash
   mysql -u root -p < ../组织服务数据库脚本.sql
   ```

5. **添加链路追踪字段**
   ```bash
   mysql -u root -p < update_login_log_add_trace_columns.sql
   mysql -u root -p < update_all_log_tables_add_trace_columns.sql
   ```

6. **更新表结构**
   ```bash
   mysql -u root -p < update_login_log_status.sql
   mysql -u root -p < update_system_log_table.sql
   ```

### 功能模块单独安装

如果只需要安装特定功能模块，请参考对应分类下的脚本说明。

### 升级脚本

更新脚本（update_*.sql）用于升级现有数据库结构，请按照依赖顺序执行。

## ⚠️ 注意事项

1. **备份数据**: 执行任何 SQL 脚本前，请先备份数据库
2. **检查依赖**: 某些脚本有前置依赖，必须按顺序执行
3. **测试环境**: 建议先在测试环境验证脚本
4. **权限要求**: 确保有足够的权限执行 DDL/DML 语句
5. **字符集**: 确保数据库使用 UTF-8 字符集

## 📝 脚本命名规范

- `create_*.sql` - 创建表或数据库
- `update_*.sql` - 更新表结构
- `init_*.sql` - 初始化脚本
- `fix_*.sql` - 修复脚本
- `reset_*.sql` - 重置脚本
- `check_*.sql` - 检查脚本

## 🔗 相关文档

- [平台日志处理方案](../平台日志处理方案.md) - 日志功能完整说明
- [组织服务功能实现总结](../组织服务功能实现总结.md) - 组织服务功能说明
- [统一业务日志方案](../统一业务日志方案.md) - 业务日志实现

## 📞 支持

如有脚本问题，请查看对应功能文档或联系开发团队。
