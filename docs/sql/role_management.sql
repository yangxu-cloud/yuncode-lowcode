-- =============================================
-- 角色管理模块数据库表
-- =============================================

-- ----------------------------
-- 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id` BIGINT NOT NULL COMMENT '角色ID',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID（0表示根级分类）',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_type` TINYINT NOT NULL DEFAULT 2 COMMENT '角色类型：1-分类，2-具体角色',
    `description` VARCHAR(500) COMMENT '角色描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-禁用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0-正常，1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(50) COMMENT '创建者',
    `update_by` VARCHAR(50) COMMENT '更新者',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_parent` (`tenant_id`, `parent_id`),
    KEY `idx_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- 角色用户关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_user`;
CREATE TABLE `sys_role_user` (
    `id` BIGINT NOT NULL COMMENT 'ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_user` (`role_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色用户关联表';

-- ----------------------------
-- 角色部门关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
    `id` BIGINT NOT NULL COMMENT 'ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `dept_id` BIGINT NOT NULL COMMENT '部门ID',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_dept` (`role_id`, `dept_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色部门关联表';

-- ----------------------------
-- 角色权限关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `id` BIGINT NOT NULL COMMENT 'ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID（菜单ID）',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ----------------------------
-- 初始化默认数据（可选，也可以通过 Java 初始化器实现）
-- ----------------------------
-- INSERT INTO `sys_role` (`id`, `tenant_id`, `parent_id`, `role_name`, `role_code`, `role_type`, `description`, `sort_order`, `status`) VALUES
-- (1, 0, 0, '用户', 'user_category', 1, '用户角色分类', 1, 0),
-- (2, 0, 1, '普通用户', 'common_user', 2, '系统普通用户，拥有基本权限', 1, 0),
-- (3, 0, 1, '系统管理员', 'system_admin', 2, '系统管理员，拥有所有权限', 2, 0),
-- (4, 0, 1, '租户管理员', 'tenant_admin', 2, '租户管理员，管理租户内的用户和角色', 3, 0);
