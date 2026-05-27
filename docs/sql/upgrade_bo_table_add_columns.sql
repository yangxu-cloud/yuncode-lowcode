-- BO 元数据管理升级脚本
-- 新增 columns 以支持 indexes、designVersion、signature、componentSetting、columnWidth

-- sys_bo_table 新增列
ALTER TABLE `sys_bo_table`
    ADD COLUMN `indexes` text DEFAULT NULL COMMENT '索引定义(JSON)' AFTER `update_time`,
    ADD COLUMN `design_version` int NOT NULL DEFAULT 1 COMMENT '设计版本号' AFTER `indexes`,
    ADD COLUMN `signature` varchar(128) DEFAULT NULL COMMENT 'XML签名' AFTER `design_version`;

-- sys_bo_field 新增列
ALTER TABLE `sys_bo_field`
    ADD COLUMN `component_setting` text DEFAULT NULL COMMENT '组件配置JSON' AFTER `sort`,
    ADD COLUMN `column_width` int DEFAULT 150 COMMENT '列宽' AFTER `component_setting`;
