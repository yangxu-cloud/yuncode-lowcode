# 数据库设计文档

## 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE yuncode_lowcode CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE yuncode_lowcode;
```

## 表结构设计

### 1. 租户表 (sys_tenant)

```sql
CREATE TABLE `sys_tenant` (
  `id` BIGINT NOT NULL COMMENT '租户ID',
  `tenant_name` VARCHAR(100) NOT NULL COMMENT '租户名称',
  `tenant_code` VARCHAR(50) NOT NULL COMMENT '租户编码',
  `contact_name` VARCHAR(50) COMMENT '联系人',
  `contact_phone` VARCHAR(20) COMMENT '联系电话',
  `contact_email` VARCHAR(100) COMMENT '联系邮箱',
  `address` VARCHAR(200) COMMENT '企业地址',
  `tenant_type` TINYINT DEFAULT 0 COMMENT '租户类型（0试用 1标准 2高级 3企业）',
  `expire_time` DATETIME COMMENT '过期时间',
  `user_limit` INT DEFAULT 100 COMMENT '用户数量限制',
  `storage_limit` INT DEFAULT 10240 COMMENT '存储空间限制（MB）',
  `status` TINYINT DEFAULT 0 COMMENT '状态（0正常 1禁用）',
  `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) COMMENT '创建者',
  `update_by` VARCHAR(50) COMMENT '更新者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统租户表';
```

### 2. 用户表 (sys_user)

```sql
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL COMMENT '用户ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `avatar` VARCHAR(200) COMMENT '头像',
  `email` VARCHAR(100) COMMENT '邮箱',
  `phone` VARCHAR(20) COMMENT '手机号',
  `gender` TINYINT DEFAULT 2 COMMENT '性别（0男 1女 2未知）',
  `status` TINYINT DEFAULT 0 COMMENT '状态（0正常 1禁用）',
  `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) COMMENT '创建者',
  `update_by` VARCHAR(50) COMMENT '更新者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

### 3. 角色表 (sys_role)

```sql
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL COMMENT '角色ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(200) COMMENT '角色描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 0 COMMENT '状态（0正常 1禁用）',
  `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) COMMENT '创建者',
  `update_by` VARCHAR(50) COMMENT '更新者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role_code` (`tenant_id`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';
```

### 4. 菜单表 (sys_menu)

```sql
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL COMMENT '菜单ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID',
  `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `menu_type` TINYINT NOT NULL COMMENT '菜单类型（0目录 1菜单 2按钮）',
  `path` VARCHAR(200) COMMENT '路由地址',
  `component` VARCHAR(200) COMMENT '组件路径',
  `permission` VARCHAR(100) COMMENT '权限标识',
  `icon` VARCHAR(100) COMMENT '菜单图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `visible` TINYINT DEFAULT 0 COMMENT '是否可见（0显示 1隐藏）',
  `status` TINYINT DEFAULT 0 COMMENT '状态（0正常 1禁用）',
  `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) COMMENT '创建者',
  `update_by` VARCHAR(50) COMMENT '更新者',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';
```

### 5. 用户角色关联表 (sys_user_role)

```sql
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

### 6. 角色菜单关联表 (sys_role_menu)

```sql
CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';
```

### 7. 操作日志表 (sys_oper_log)

```sql
CREATE TABLE `sys_oper_log` (
  `id` BIGINT NOT NULL COMMENT '日志ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `module` VARCHAR(50) COMMENT '模块标题',
  `business_type` TINYINT COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` VARCHAR(100) COMMENT '方法名称',
  `request_method` VARCHAR(10) COMMENT '请求方式',
  `oper_name` VARCHAR(50) COMMENT '操作人员',
  `oper_url` VARCHAR(255) COMMENT '请求URL',
  `oper_ip` VARCHAR(128) COMMENT '主机地址',
  `oper_param` TEXT COMMENT '请求参数',
  `json_result` TEXT COMMENT '返回参数',
  `status` TINYINT DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` VARCHAR(2000) COMMENT '错误消息',
  `oper_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_oper_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

## 初始化数据

```sql
-- 插入默认租户
INSERT INTO sys_tenant (id, tenant_name, tenant_code, contact_name, tenant_type, status)
VALUES (1, '默认租户', 'default', 'System', 3, 0);

-- 插入默认管理员 (密码: admin123)
INSERT INTO sys_user (id, tenant_id, username, password, nickname, status)
VALUES (1, 1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE/sW7MpDKqZQq', '管理员', 0);

-- 插入默认角色
INSERT INTO sys_role (id, tenant_id, role_name, role_code, description, status)
VALUES
  (1, 1, '超级管理员', 'admin', '拥有所有权限', 0),
  (2, 1, '普通用户', 'user', '普通用户权限', 0);

-- 插入默认菜单
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_type, path, icon, sort_order, status)
VALUES
  (1, 1, 0, '系统管理', 0, '/system', 'Setting', 1, 0),
  (2, 1, 1, '用户管理', 1, '/system/user', 'User', 1, 0),
  (3, 1, 1, '角色管理', 1, '/system/role', 'UserFilled', 2, 0),
  (4, 1, 1, '菜单管理', 1, '/system/menu', 'Menu', 3, 0),
  (5, 1, 1, '租户管理', 1, '/system/tenant', 'OfficeBuilding', 4, 0);

-- 关联管理员角色
INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (1, 1, 1);

-- 关联角色菜单
INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES
  (1, 1, 1),
  (2, 1, 2),
  (3, 1, 3),
  (4, 1, 4),
  (5, 1, 5);
```

## 索引设计

### 性能优化索引
```sql
-- 用户表索引
CREATE INDEX idx_user_tenant ON sys_user(tenant_id, status);
CREATE INDEX idx_user_username ON sys_user(username);

-- 角色表索引
CREATE INDEX idx_role_tenant ON sys_role(tenant_id, status);

-- 菜单表索引
CREATE INDEX idx_menu_tenant ON sys_menu(tenant_id, parent_id);

-- 操作日志表索引
CREATE INDEX idx_log_tenant_time ON sys_oper_log(tenant_id, oper_time);
```

## 数据字典

### 用户性别
- 0: 男
- 1: 女
- 2: 未知

### 通用状态
- 0: 正常
- 1: 禁用

### 删除标志
- 0: 正常
- 1: 已删除

### 菜单类型
- 0: 目录
- 1: 菜单
- 2: 按钮

### 租户类型
- 0: 试用版
- 1: 标准版
- 2: 高级版
- 3: 企业版

### 业务类型
- 0: 其它
- 1: 新增
- 2: 修改
- 3: 删除
- 4: 授权
- 5: 导出
- 6: 导入
