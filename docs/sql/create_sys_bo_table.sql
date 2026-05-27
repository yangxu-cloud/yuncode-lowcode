-- 业务对象表（BO Definition）
CREATE TABLE IF NOT EXISTS `sys_bo_table` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` VARCHAR(100) NOT NULL COMMENT '应用ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `category_name` VARCHAR(50) DEFAULT NULL COMMENT '分类名称（冗余）',
  `title_name` VARCHAR(20) NOT NULL COMMENT '标题名称',
  `storage_name` VARCHAR(100) NOT NULL COMMENT '存储名称（含前缀）',
  `storage_type` VARCHAR(20) NOT NULL DEFAULT 'Table' COMMENT '存储类型: Table/View/Structure',
  `biz_code` VARCHAR(50) DEFAULT NULL COMMENT '业务编码',
  `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `indexes` text DEFAULT NULL COMMENT '索引定义(JSON)',
  `design_version` int NOT NULL DEFAULT 1 COMMENT '设计版本号',
  `signature` varchar(128) DEFAULT NULL COMMENT 'XML签名',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-正常,1-删除)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_app_id` (`app_id`) USING BTREE,
  KEY `idx_category_id` (`category_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务对象表定义';

-- 业务对象字段定义
CREATE TABLE IF NOT EXISTS `sys_bo_field` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `table_id` BIGINT NOT NULL COMMENT '所属表ID',
  `field_name` VARCHAR(64) NOT NULL COMMENT '字段名称',
  `field_title` VARCHAR(50) DEFAULT NULL COMMENT '字段标题',
  `field_type` VARCHAR(30) NOT NULL DEFAULT 'varchar' COMMENT '字段类型',
  `field_length` INT DEFAULT 255 COMMENT '字段长度',
  `component` VARCHAR(50) DEFAULT NULL COMMENT '前端组件类型',
  `default_value` VARCHAR(200) DEFAULT NULL COMMENT '默认值',
  `required` TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见',
  `readonly` TINYINT NOT NULL DEFAULT 0 COMMENT '是否只读',
  `copyable` TINYINT NOT NULL DEFAULT 0 COMMENT '是否可复制',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `component_setting` text DEFAULT NULL COMMENT '组件配置JSON',
  `column_width` int DEFAULT 150 COMMENT '列宽',
  `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-正常,1-删除)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_table_id` (`table_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务对象字段定义';
