-- ========================================
-- 导航管理相关表
-- 创建日期: 2025-01-30
-- 说明: 支持三层菜单结构、租户隔离、权限控制
-- ========================================

-- 菜单表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT NOT NULL COMMENT '菜单ID',
  `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID(默认导航为NULL,所有租户可见)',
  `tenant_code` VARCHAR(50) DEFAULT NULL COMMENT '租户编码',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID,0表示根节点',
  `menu_name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
  `menu_type` TINYINT NOT NULL DEFAULT 1 COMMENT '菜单类型:0=目录,1=菜单,2=按钮',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
  `component` VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
  `permission` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `visible` TINYINT DEFAULT 0 COMMENT '是否可见（0显示 1隐藏）',
  `status` TINYINT DEFAULT 0 COMMENT '状态（0正常 1禁用）',
  `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_menu_type` (`menu_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- 菜单权限关联表
CREATE TABLE IF NOT EXISTS `sys_menu_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `target_type` TINYINT NOT NULL COMMENT '目标类型:0=角色,1=用户,2=部门',
  `target_id` BIGINT NOT NULL COMMENT '目标ID(角色ID/用户ID/部门ID)',
  `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
  `deleted` TINYINT DEFAULT 0 COMMENT '删除标记:0=未删除,1=已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_target` (`menu_id`, `target_type`, `target_id`, `deleted`),
  KEY `idx_menu_id` (`menu_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限关联表';

-- ========================================
-- 初始化默认菜单数据
-- 说明: 默认导航(办公>常用/工具/通讯录)对所有租户可见
-- ========================================

-- 清空现有数据
DELETE FROM `sys_menu_permission` WHERE `menu_id` IN (SELECT `id` FROM `sys_menu` WHERE `menu_name` IN ('办公', '常用', '工具', '通讯录'));
DELETE FROM `sys_menu` WHERE `menu_name` IN ('办公', '常用', '工具', '通讯录');

-- 一级菜单:办公
INSERT INTO `sys_menu` (`id`, `menu_name`, `icon`, `parent_id`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES (1, '办公', 'OfficeBuilding', 0, 0, 0, NULL, NULL, 0, 0);

-- 二级菜单:常用
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES (2, '常用', 1, 0, 0, NULL, NULL, 0, 0);

-- 三级菜单:流程中心、邮件、网盘
INSERT INTO `sys_menu` (`id`, `menu_name`, `icon`, `parent_id`, `path`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES
(3, '流程中心', 'Operation', 2, '/workflow', 0, 1, NULL, NULL, 0, 0),
(4, '邮件', 'Message', 2, '/email', 1, 1, NULL, NULL, 0, 0),
(5, '网盘', 'FolderOpened', 2, '/netdisk', 2, 1, NULL, NULL, 0, 0);

-- 二级菜单:工具
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES (6, '工具', 1, 1, 0, NULL, NULL, 0, 0);

-- 二级菜单:通讯录
INSERT INTO `sys_menu` (`id`, `menu_name`, `icon`, `parent_id`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES (7, '通讯录', 'AddressBook', 1, 2, 0, NULL, NULL, 0, 0);

-- 三级菜单:单位通讯录、个人通讯录
INSERT INTO `sys_menu` (`id`, `menu_name`, `icon`, `parent_id`, `path`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES
(8, '单位通讯录', 'User', 7, '/contacts/company', 0, 1, NULL, NULL, 0, 0),
(9, '个人通讯录', 'UserFilled', 7, '/contacts/personal', 1, 1, NULL, NULL, 0, 0);

-- ========================================
-- 索引优化建议
-- ========================================
-- ALTER TABLE `sys_menu` ADD INDEX `idx_menu_type_tenant` (`menu_type`, `tenant_id`, `status`);
-- ALTER TABLE `sys_menu_permission` ADD INDEX `idx_tenant_deleted` (`tenant_id`, `deleted`);
