CREATE TABLE IF NOT EXISTS `sys_app_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` VARCHAR(100) NOT NULL COMMENT '应用标识',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID，NULL表示一级分类',
  `sort` INT DEFAULT 0 COMMENT '排序号',
  `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用分类表';
