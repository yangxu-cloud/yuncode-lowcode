# 数据库初始化说明

## 概述

本文件夹包含 Yuncode LowCode 平台的所有数据库初始化脚本。

---

## SQL 脚本文件说明

### 1. init_database.sql（推荐使用）⭐
**完整数据库初始化脚本**

包含内容：
- ✅ 系统租户表（sys_tenant）
- ✅ 系统用户表（sys_user）
- ✅ 登录日志表（sys_login_log）
- ✅ 操作日志表（sys_operation_log）
- ✅ 系统日志表（sys_system_log）
- ✅ 系统设置表（sys_settings）
- ✅ 初始数据（默认租户、管理员、系统设置）

**适用场景**：首次部署或完整重置数据库

---

### 2. create_sys_settings.sql
**单独创建系统设置表**

包含内容：
- ✅ 系统设置表结构
- ✅ 默认基础设置数据

**适用场景**：只需更新系统设置表

---

### 3. update_login_log_add_trace_columns.sql
**更新登录日志表添加链路追踪字段**

包含内容：
- ✅ 添加 trace_id、span_id、parent_span_id 字段
- ✅ 创建索引
- ✅ 可选：为历史数据生成 traceId

**适用场景**：已有登录日志表，需要添加链路追踪功能

---

### 4. update_all_log_tables_add_trace_columns.sql
**更新所有日志表添加链路追踪字段**

包含内容：
- ✅ 所有日志表的链路追踪字段更新
- ✅ 验证脚本

**适用场景**：批量更新所有日志表

---

## 快速开始

### 步骤 1：创建数据库

```sql
CREATE DATABASE IF NOT EXISTS yuncode_lowcode
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE yuncode_lowcode;
```

### 步骤 2：执行初始化脚本

```bash
# Windows
mysql -u root -p yuncode_lowcode < c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot\sql\init_database.sql

# Linux/Mac
mysql -u root -p yuncode_lowcode < /path/to/yuncode-lowcode-boot/sql/init_database.sql
```

或者在 MySQL 客户端中：
```sql
SOURCE /path/to/init_database.sql;
```

### 步骤 3：验证安装

```sql
-- 查看所有表
SHOW TABLES;

-- 查看租户
SELECT * FROM sys_tenant;

-- 查看管理员用户
SELECT id, tenant_id, username, nickname, status FROM sys_user;

-- 查看系统设置
SELECT * FROM sys_settings WHERE setting_group = 'basic' ORDER BY sort;
```

---

## 默认账号信息

### 管理员账号
- **用户名**: `admin`
- **密码**: `admin123`
- **租户编码**: `default`

### 安全提示
⚠️ **生产环境请立即修改默认密码！**

```sql
-- 修改管理员密码（新密码需要先用 BCrypt 加密）
UPDATE sys_user
SET password = '$2a$10$新的BCrypt密码'
WHERE username = 'admin';
```

---

## 表结构说明

### sys_tenant（系统租户表）
存储多租户信息。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 租户ID |
| tenant_name | VARCHAR(100) | 租户名称 |
| tenant_code | VARCHAR(50) | 租户编码（唯一） |
| tenant_type | TINYINT | 租户类型（0试用 1标准 2高级 3企业） |
| expire_time | DATETIME | 过期时间 |
| status | TINYINT | 状态（0正常 1禁用） |

### sys_user（系统用户表）
存储用户信息。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户ID |
| tenant_id | BIGINT | 租户ID |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(200) | 密码（BCrypt加密） |
| nickname | VARCHAR(50) | 昵称 |
| status | TINYINT | 状态（0正常 1禁用） |

### sys_login_log（登录日志表）
记录用户登录日志。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 日志ID |
| tenant_id | BIGINT | 租户ID |
| user_id | BIGINT | 用户ID |
| username | VARCHAR(50) | 用户名 |
| login_time | DATETIME | 登录时间 |
| ipaddr | VARCHAR(128) | IP地址 |
| browser | VARCHAR(50) | 浏览器 |
| os | VARCHAR(50) | 操作系统 |
| status | TINYINT | 状态（0成功 1失败） |
| trace_id | VARCHAR(64) | 链路追踪ID |
| span_id | VARCHAR(64) | Span ID |

### sys_operation_log（操作日志表）
记录用户操作日志。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 日志ID |
| tenant_id | BIGINT | 租户ID |
| user_id | BIGINT | 用户ID |
| username | VARCHAR(50) | 用户名 |
| module | VARCHAR(50) | 模块 |
| operation | VARCHAR(100) | 操作 |
| method | VARCHAR(200) | 方法 |
| execute_time | BIGINT | 执行时长(ms) |
| trace_id | VARCHAR(64) | 链路追踪ID |

### sys_system_log（系统日志表）
记录系统运行日志。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 日志ID |
| tenant_id | BIGINT | 租户ID |
| log_type | VARCHAR(20) | 日志类型 |
| log_title | VARCHAR(100) | 日志标题 |
| log_content | TEXT | 日志内容 |
| trace_id | VARCHAR(64) | 链路追踪ID |

### sys_settings（系统设置表）
存储系统配置信息。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 设置ID |
| tenant_id | BIGINT | 租户ID（NULL表示全局设置） |
| setting_group | VARCHAR(50) | 设置分组 |
| setting_key | VARCHAR(100) | 设置键 |
| setting_value | TEXT | 设置值 |
| data_type | VARCHAR(20) | 数据类型 |

---

## 链路追踪功能

所有日志表都支持链路追踪功能，包含以下字段：
- `trace_id` - 链路追踪ID
- `span_id` - Span ID
- `parent_span_id` - 父 Span ID

这些字段会在每个请求中自动填充，用于分布式追踪和日志聚合分析。

---

## 数据库配置

### application.yml 配置示例

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/yuncode_lowcode?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
    username: root
    password: root
```

---

## 备份与恢复

### 备份数据库
```bash
mysqldump -u root -p yuncode_lowcode > yuncode_lowcode_backup_$(date +%Y%m%d).sql
```

### 恢复数据库
```bash
mysql -u root -p yuncode_lowcode < yuncode_lowcode_backup_20240118.sql
```

---

## 常见问题

### Q1: 执行 SQL 脚本报错 "Table already exists"
**A**: 脚本使用了 `CREATE TABLE IF NOT EXISTS`，可以安全地重复执行。如果需要完全重建，先删除表：
```sql
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS sys_system_log;
DROP TABLE IF EXISTS sys_settings;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_tenant;
```

### Q2: 如何重置管理员密码？
**A**: 使用在线工具生成 BCrypt 密码，然后更新：
```sql
UPDATE sys_user
SET password = '$2a$10$新的BCrypt密码'
WHERE username = 'admin';
```

推荐在线工具：https://bcrypt-generator.com/

### Q3: 如何添加新租户？
**A**: 插入新租户记录：
```sql
INSERT INTO sys_tenant (id, tenant_name, tenant_code, contact_name, tenant_type, expire_time, status)
VALUES (2, '新租户', 'tenant001', '张三', 1, '2025-12-31 23:59:59', 0);
```

---

## 更新日志

### 2024-01-18
- ✅ 创建完整的数据库初始化脚本
- ✅ 添加链路追踪字段到所有日志表
- ✅ 添加系统设置表
- ✅ 添加默认租户和管理员

---

## 技术支持

如有问题，请查看：
- [MDC 链路追踪实现文档](../docs/MDC_TRACE_IMPLEMENTATION.md)
- [平台要求](../../.skills/平台要求/SKILL.md)
