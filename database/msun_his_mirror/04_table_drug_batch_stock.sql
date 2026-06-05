-- 2.5.43 药房批次库存镜像表（增量脚本，在 01_table.sql 之后执行）
USE `msun_his_mirror`;
/

CREATE TABLE IF NOT EXISTS `m_drug_batch_stock` (
  `mirror_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '镜像自增主键',
  `hospital_key` VARCHAR(64) NOT NULL COMMENT '医院客户键',
  `active_env` VARCHAR(16) NOT NULL DEFAULT 'prod' COMMENT '环境',
  `api_code` VARCHAR(32) NOT NULL DEFAULT '2.5.43' COMMENT '接口编号',
  `sync_batch_no` VARCHAR(64) DEFAULT NULL COMMENT '同步批次号',
  `his_trace_id` VARCHAR(64) DEFAULT NULL COMMENT 'HIS traceId',
  `request_params_json` TEXT COMMENT '请求入参JSON',
  `raw_item_json` MEDIUMTEXT COMMENT '原始行JSON备份',
  `mirror_source` VARCHAR(32) DEFAULT 'api' COMMENT '镜像来源',
  `mirror_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '镜像入库时间',
  `dept_id` VARCHAR(64) DEFAULT NULL COMMENT '药房科室ID',
  `drug_id` VARCHAR(64) DEFAULT NULL COMMENT '药品/材料ID',
  `drug_spec_packing_id` VARCHAR(64) DEFAULT NULL COMMENT '规格包装ID',
  `batch_number` VARCHAR(128) DEFAULT NULL COMMENT '批号',
  `stock_id` VARCHAR(64) DEFAULT NULL COMMENT '库存ID',
  `quantity` DECIMAL(18,4) DEFAULT NULL COMMENT '库存数量',
  `buy_price` DECIMAL(18,4) DEFAULT NULL COMMENT '进价',
  `retail_price` DECIMAL(18,4) DEFAULT NULL COMMENT '零售价',
  `effective` VARCHAR(32) DEFAULT NULL COMMENT '有效期',
  `produce_date` VARCHAR(32) DEFAULT NULL COMMENT '生产日期',
  `producer_id` VARCHAR(64) DEFAULT NULL COMMENT '生产厂商ID',
  `producer_name` VARCHAR(500) DEFAULT NULL COMMENT '生产厂商',
  `supplier_id` VARCHAR(64) DEFAULT NULL COMMENT '供应商ID',
  `supplier_name` VARCHAR(500) DEFAULT NULL COMMENT '供应商',
  `packing_id` VARCHAR(64) DEFAULT NULL COMMENT '包装ID',
  `packing_name` VARCHAR(64) DEFAULT NULL COMMENT '包装单位',
  `hospital_id` VARCHAR(64) DEFAULT NULL COMMENT '医院ID',
  `org_id` VARCHAR(64) DEFAULT NULL COMMENT '机构ID',
  PRIMARY KEY (`mirror_id`),
  UNIQUE KEY `uk_batch_stock` (`dept_id`, `drug_id`, `drug_spec_packing_id`, `batch_number`, `hospital_key`, `active_env`),
  KEY `idx_drug_id` (`drug_id`),
  KEY `idx_sync_batch` (`sync_batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='镜像-2.5.43药房批次库存';
/
