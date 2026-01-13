-- SPD数据库 - HIS耗材信息表
CREATE TABLE IF NOT EXISTS `his_hc_info` (
  `charge_item_id` VARCHAR(50) NOT NULL COMMENT '收费项目ID',
  `item_code` VARCHAR(100) DEFAULT NULL COMMENT '项目编码',
  `item_name` VARCHAR(500) DEFAULT NULL COMMENT '项目名称',
  `item_type` VARCHAR(100) DEFAULT NULL COMMENT '项目类型',
  `consumable_type` VARCHAR(100) DEFAULT NULL COMMENT '耗材类型',
  `spec_model` VARCHAR(500) DEFAULT NULL COMMENT '规格型号',
  `unit` VARCHAR(50) DEFAULT NULL COMMENT '单位',
  `price` DECIMAL(18, 4) DEFAULT NULL COMMENT '价格',
  `manufacturer` VARCHAR(500) DEFAULT NULL COMMENT '生产厂家',
  `register_no` VARCHAR(200) DEFAULT NULL COMMENT '注册证号',
  `is_active` VARCHAR(20) DEFAULT NULL COMMENT '是否启用',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`charge_item_id`),
  KEY `idx_item_code` (`item_code`),
  KEY `idx_item_name` (`item_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HIS耗材信息表';
