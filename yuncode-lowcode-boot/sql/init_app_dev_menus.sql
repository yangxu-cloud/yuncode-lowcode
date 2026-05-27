-- ========================================
-- 应用开发菜单初始化
-- 创建日期: 2026-05-09
-- 说明: 根据skill文档，创建应用开发菜单结构
-- ========================================

USE yuncode_lowcode;

-- 清空旧的应用开发相关菜单
DELETE FROM `sys_menu_permission` WHERE `menu_id` IN (SELECT `id` FROM `sys_menu` WHERE `menu_name` IN ('应用开发', '业务建模', '定时任务', '系统服务'));
DELETE FROM `sys_menu` WHERE `menu_name` IN ('应用开发', '业务建模', '定时任务', '系统服务');

-- 注意：应用管理菜单保留在公共设施下，不在这里创建

-- 一级菜单: 应用开发
INSERT INTO `sys_menu` (`id`, `menu_name`, `icon`, `parent_id`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES (200, '应用开发', 'Monitor', 0, 4, 1, NULL, NULL, 0, 0);

-- 二级菜单: 业务建模（预留，待开发）
INSERT INTO `sys_menu` (`id`, `menu_name`, `icon`, `parent_id`, `path`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES (201, '业务建模', 'Operation', 200, '/app-dev/modeling', 1, 1, NULL, NULL, 0, 0);

-- 二级菜单: 定时任务（预留，待开发）
INSERT INTO `sys_menu` (`id`, `menu_name`, `icon`, `parent_id`, `path`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES (202, '定时任务', 'Timer', 200, '/app-dev/schedule', 2, 1, NULL, NULL, 0, 0);

-- 二级菜单: 系统服务（预留，待开发）
INSERT INTO `sys_menu` (`id`, `menu_name`, `icon`, `parent_id`, `path`, `sort_order`, `menu_type`, `tenant_id`, `tenant_code`, `visible`, `status`)
VALUES (203, '系统服务', 'Tools', 200, '/app-dev/system-service', 3, 1, NULL, NULL, 0, 0);

-- 验证插入结果
SELECT '应用开发菜单初始化完成' AS message;
SELECT * FROM `sys_menu` WHERE `parent_id` = 200 ORDER BY `sort_order`;