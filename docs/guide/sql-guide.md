# SQL 脚本使用指南

本指南说明项目中所有 SQL 脚本的位置、分类和使用方法。

## 📁 脚本位置

所有 SQL 脚本按功能分类在以下位置：

```
docs/功能文档/sql脚本/
├── README.md                    # SQL脚本详细索引
├── create_login_log_table.sql
├── create_system_log_table.sql
├── create_oper_log_table.sql
├── create_sys_settings.sql
├── init_database.sql
├── check_user.sql
├── reset_admin_password.sql
├── fix_admin_password.sql
├── fix_system_admin_password.sql
└── update_system_admin_password.sql
...以及其他更新脚本
```

## 🗂️ 脚本分类

### 按功能模块分类

| 功能模块 | 脚本目录位置 | 相关文档 |
|---------|--------------|----------|
| **日志管理** | `sql脚本/` | [平台日志处理方案.md](./平台日志处理方案.md) |
| **组织服务** | 功能文档根目录 | [组织服务功能实现总结.md](./组织服务功能实现总结.md) |
| **系统设置** | `sql脚本/` | - |
| **用户管理** | `sql脚本/` | [多用户在线管理实现方案.md](./多用户在线管理实现方案.md) |

### 按脚本类型分类

| 类型 | 前缀 | 说明 |
|------|------|------|
| **创建表** | `create_` | 创建数据库表 |
| **更新表** | `update_` | 更新表结构（添加字段等） |
| **初始化** | `init_` | 系统初始化脚本 |
| **维护** | `fix_`, `reset_`, `check_` | 数据修复和维护脚本 |

## 🚀 快速开始

### 首次安装

完整安装系统数据库，请按以下顺序执行：

```bash
# 1. 初始化数据库
mysql -u root -p < init_database.sql

# 2. 创建日志表
mysql -u root -p < create_login_log_table.sql
mysql -u root -p < create_system_log_table.sql
mysql - u root -p < create_oper_log_table.sql

# 3. 创建系统设置表
mysql -u root -p < create_sys_settings.sql

# 4. 创建组织服务表（可选）
mysql -u root -p < ../组织服务数据库脚本.sql
```

### 单独安装功能模块

如果只需要安装特定功能，请参考对应的功能文档。

## 📖 脚本详细说明

### 1. 日志管理脚本

#### create_login_log_table.sql
- **功能**: 创建登录日志表
- **表名**: `sys_login_log`
- **用途**: 记录用户登录/登出行为
- **执行方式**: `mysql -u root -p < create_login_log_table.sql`

#### create_system_log_table.sql
- **功能**: 创建系统日志表
- **表名**: `sys_system_log`
- **用途**: 记录系统异常和性能问题
- **执行方式**: `mysql -u root -p < create_system_log_table.sql`

#### create_oper_log_table.sql
- **功能**: 创建操作日志表
- **表名**: `sys_operation_log`
- **用途**: 记录用户 CRUD 操作
- **执行方式**: `mysql -u root -p < create_oper_log_table.sql`

#### update_login_log_add_trace_columns.sql
- **功能**: 为登录日志表添加链路追踪字段
- **依赖**: create_login_log_table.sql
- **用途**: 支持 TraceId/SpanId 链路追踪
- **执行方式**: `mysql -u root -p < update_login_log_add_trace_columns.sql`

### 2. 组织服务脚本

#### 组织服务数据库脚本.sql
- **位置**: `docs/功能文档/组织服务数据库脚本.sql`
- **功能**: 创建组织管理相关表
- **包含表**:
  - `sys_org` - 组织表
  - `sys_user_org` - 用户组织关联表
- **执行方式**: `mysql -u root -p < 组织服务数据库脚本.sql`

### 3. 系统设置脚本

#### create_sys_settings.sql
- **功能**: 创建系统设置表
- **表名**: `sys_settings`
- **用途**: 存储系统配置信息
- **执行方式**: `mysql -u root -p < create_sys_settings.sql`

### 4. 维护脚本

#### check_user.sql
- **功能**: 检查用户信息
- **用途**: 排查用户问题时使用
- **执行方式**: `mysql -u root -p < check_user.sql [参数]`

#### reset_admin_password.sql
- **功能**: 重置管理员密码
- **用途**: 忘记管理员密码时使用
- **执行方式**: `mysql -u root -p < reset_admin_password.sql`

#### fix_admin_password.sql
- **功能**: 修复管理员密码
- **用途**: 密码出现问题时使用
- **执行方式**: `mysql -u root -p < fix_admin_password.sql`

#### fix_system_admin_password.sql
- **功能**: 修复系统管理员密码
- **用途**: 修复系统管理员密码
- **执行方式**: `mysql -u root -p < fix_system_admin_password.sql`

## ⚠️ 重要提示

### 执行前准备

1. **备份数据库**: 执行任何 SQL 前，务必备份数据库
   ```bash
   mysqldump -u root -p --all-databases > backup_$(date +%Y%m%d).sql
   ```

2. **检查脚本**: 执行前先查看脚本内容，了解其作用
   ```bash
   cat create_login_log_table.sql
   ```

3. **测试环境**: 建议先在测试环境验证

### 执行注意事项

1. **字符集**: 确保数据库使用 UTF-8
   ```sql
   ALTER DATABASE your_database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **权限**: 确保有足够的权限
   ```bash
   mysql -u root -p  # 以root用户执行
   ```

3. **顺序**: 有依赖关系的脚本必须按顺序执行

4. **验证**: 执行后验证表是否创建成功
   ```sql
   SHOW TABLES LIKE 'sys_%';
   DESC sys_login_log;
   ```

## 🔍 脚本验证

### 验证表创建

```bash
# 查看所有表
mysql -u root -p -e "USE your_database; SHOW TABLES;"

# 查看表结构
mysql -u root -p -e "USE your_database; DESC sys_login_log;"
```

### 验证数据

```bash
# 查看表记录
mysql -u root -p -e "USE your_database; SELECT * FROM sys_login_log LIMIT 10;"
```

## 📚 相关文档

### 功能文档

- [平台日志处理方案.md](./平台日志处理方案.md) - 日志功能完整说明
- [统一业务日志方案.md](./统一业务日志方案.md) - 业务日志实现
- [组织服务功能实现总结.md](./组织服务功能实现总结.md) - 组织服务功能说明
- [多用户在线管理实现方案.md](./多用户在线管理实现方案.md) - 在线用户管理
- [多账户登陆方案（用户踢出,SSE消息机制.md](./多账户登陆方案（用户踢出,SSE消息机制.md)) - 多账户登录

### 技术文档

- [MDC_TRACE_IMPLEMENTATION_COPY.md](../MDC_TRACE_IMPLEMENTATION_COPY.md) - MDC链路追踪
- [INTERFACE_IMPLEMENTATION_SUMMARY.md](../INTERFACE_IMPLEMENTATION_SUMMARY.md) - 接口实现总结
- [PROJECT_STRUCTURE.md](../PROJECT_STRUCTURE.md) - 项目结构说明

## 🆘 常见问题

### Q1: 脚本执行失败怎么办？

A: 检查以下几点：
1. 数据库服务是否启动
2. 用户名密码是否正确
3. 是否有足够的权限
4. 表是否已存在（如需重新创建，先 DROP TABLE）

### Q2: 如何回滚已执行的脚本？

A:
1. 如果只创建了表，使用 `DROP TABLE table_name;`
2. 如果有数据修改，需要从备份恢复
3. 这就是为什么执行前要备份的原因

### Q3: 可以修改脚本吗？

A: 可以，但要注意：
1. 修改前先备份原脚本
2. 测试环境验证
3. 更新相关文档

### Q4: 脚本在哪个环境下测试过？

A: 本项目在以下环境测试：
- MySQL 8.0+
- Spring Boot 3.x
- MyBatis-Plus 3.x

## 📞 获取帮助

如遇到问题，请：
1. 查看对应功能文档
2. 检查脚本注释
3. 联系开发团队

## 📝 更新记录

- **2025-01-22**: 按功能分类整理SQL脚本
- **2025-01-20**: 添加链路追踪字段
- **2025-01-18**: 创建基础日志表

---

**维护者**: Yuncode LowCode 开发团队
**最后更新**: 2025-01-22
