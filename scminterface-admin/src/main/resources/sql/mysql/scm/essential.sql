-- ========== SCM 必要结构（前置机 scm.enabled 时每次启动幂等补全）==========
-- 与全量 bootstrap 分离：仅 CREATE IF NOT EXISTS / 安全索引，避免院内默认跑完整 column 链。
-- 分段符：单独一行 /

CREATE TABLE IF NOT EXISTS `scm_supplier_export_log` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID7（36位）',
  `hospital_code` varchar(64) NOT NULL COMMENT '平台医院编码',
  `supplier_code` varchar(64) NOT NULL COMMENT '平台供应商编码',
  `export_scope` varchar(16) NOT NULL COMMENT '导出范围 FULL全量 LIMITED脱敏',
  `spd_tenant_id` varchar(64) DEFAULT NULL COMMENT 'SPD租户ID（前置机透传）',
  `request_ip` varchar(64) DEFAULT NULL COMMENT '请求来源IP',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '操作者（系统/接口）',
  PRIMARY KEY (`id`),
  KEY `idx_scm_supplier_export_hospital` (`hospital_code`),
  KEY `idx_scm_supplier_export_supplier` (`supplier_code`),
  KEY `idx_scm_supplier_export_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院侧经前置机拉取平台供应商信息审计日志';
/

CREATE TABLE IF NOT EXISTS `scm_bridge_inbox` (
  `id`             varchar(36)  NOT NULL COMMENT '主键UUID7',
  `hospital_code`  varchar(64)  NOT NULL COMMENT '平台医院编码',
  `tenant_id`      varchar(64)  DEFAULT NULL COMMENT 'SPD租户ID（可选）',
  `msg_type`       varchar(64)  NOT NULL COMMENT '消息类型',
  `payload_json`   mediumtext   COMMENT '消息体JSON',
  `status`         char(1)      NOT NULL DEFAULT '0' COMMENT '0待消费 1已确认',
  `create_by`      varchar(64)  DEFAULT NULL COMMENT '创建者',
  `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `ack_time`       datetime     DEFAULT NULL COMMENT '确认时间',
  PRIMARY KEY (`id`),
  KEY `idx_bridge_inbox_pull` (`hospital_code`, `status`, `create_time`),
  KEY `idx_bridge_inbox_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云端到院内稳态桥收件箱（院内pull/ack）';
/

-- MAT hospital material archive (essential)
CREATE TABLE IF NOT EXISTS `scm_hospital_material_archive` (
  `id` varchar(36) NOT NULL, `hospital_code` varchar(64) NOT NULL, `scm_supplier_code` varchar(64) NOT NULL,
  `spd_tenant_id` varchar(64) DEFAULT NULL, `spd_material_id` varchar(64) NOT NULL, `spd_material_code` varchar(64) DEFAULT NULL,
  `material_name` varchar(200) NOT NULL, `pinyin_code` varchar(100) DEFAULT NULL, `specification` varchar(200) DEFAULT NULL,
  `model` varchar(200) DEFAULT NULL, `unit_name` varchar(64) DEFAULT NULL, `price` decimal(18,6) DEFAULT NULL,
  `sale_price` decimal(18,6) DEFAULT NULL, `register_no` varchar(128) DEFAULT NULL, `register_name` varchar(200) DEFAULT NULL,
  `manufacturer_name` varchar(200) DEFAULT NULL, `udi_code` varchar(128) DEFAULT NULL, `medical_name` varchar(200) DEFAULT NULL,
  `medical_no` varchar(128) DEFAULT NULL, `brand` varchar(100) DEFAULT NULL, `useto` varchar(200) DEFAULT NULL,
  `quality` varchar(100) DEFAULT NULL, `function_desc` varchar(500) DEFAULT NULL, `is_way` varchar(32) DEFAULT NULL,
  `country_no` varchar(64) DEFAULT NULL, `country_name` varchar(128) DEFAULT NULL, `description` varchar(1000) DEFAULT NULL,
  `period_date` date DEFAULT NULL, `package_speci` varchar(128) DEFAULT NULL, `min_package_qty` decimal(18,6) DEFAULT NULL,
  `is_gz` char(1) DEFAULT NULL, `is_billing` char(1) DEFAULT NULL, `ext_json` mediumtext,
  `archive_status` char(1) NOT NULL DEFAULT '0', `last_push_batch_id` varchar(36) DEFAULT NULL, `last_apply_id` varchar(36) DEFAULT NULL,
  `del_flag` char(1) NOT NULL DEFAULT '0', `create_by` varchar(64) DEFAULT '', `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '', `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP, `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_hma_hospital_spd_mat` (`hospital_code`, `spd_material_id`),
  KEY `idx_hma_supplier` (`scm_supplier_code`), KEY `idx_hma_tenant` (`spd_tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院归属产品档案正式表';
/
CREATE TABLE IF NOT EXISTS `scm_hospital_material_push_batch` (
  `id` varchar(36) NOT NULL, `hospital_code` varchar(64) NOT NULL, `spd_tenant_id` varchar(64) DEFAULT NULL,
  `push_by` varchar(64) DEFAULT NULL, `push_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `item_count` int NOT NULL DEFAULT 0, `success_count` int NOT NULL DEFAULT 0, `fail_count` int NOT NULL DEFAULT 0,
  `field_whitelist_json` varchar(2000) DEFAULT NULL, `request_id` varchar(64) DEFAULT NULL,
  `result_status` char(1) NOT NULL DEFAULT '0', `result_msg` varchar(500) DEFAULT NULL,
  `del_flag` char(1) NOT NULL DEFAULT '0', `create_by` varchar(64) DEFAULT '', `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '', `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP, `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_hmpb_hospital_time` (`hospital_code`, `push_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院产品档案推送批次';
/
CREATE TABLE IF NOT EXISTS `scm_hospital_material_push_item` (
  `id` varchar(36) NOT NULL, `batch_id` varchar(36) NOT NULL, `archive_id` varchar(36) DEFAULT NULL,
  `spd_material_id` varchar(64) NOT NULL, `scm_supplier_code` varchar(64) DEFAULT NULL, `action_type` varchar(16) NOT NULL,
  `payload_json` mediumtext, `before_json` mediumtext, `after_json` mediumtext, `error_msg` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_hmpi_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院产品档案推送明细';
/
CREATE TABLE IF NOT EXISTS `scm_hospital_material_change_log` (
  `id` varchar(36) NOT NULL, `archive_id` varchar(36) NOT NULL, `hospital_code` varchar(64) NOT NULL,
  `change_source` varchar(32) NOT NULL, `source_id` varchar(36) DEFAULT NULL,
  `before_json` mediumtext, `after_json` mediumtext, `changed_fields` varchar(1000) DEFAULT NULL,
  `oper_by` varchar(64) DEFAULT NULL, `oper_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_hmcl_archive_time` (`archive_id`, `oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院产品档案正式档变更历史';
/
CREATE TABLE IF NOT EXISTS `scm_hospital_material_modify_apply` (
  `id` varchar(36) NOT NULL, `archive_id` varchar(36) NOT NULL, `hospital_code` varchar(64) NOT NULL,
  `scm_supplier_code` varchar(64) NOT NULL, `apply_status` char(1) NOT NULL DEFAULT '0',
  `propose_json` mediumtext NOT NULL, `base_json` mediumtext, `changed_fields` varchar(1000) DEFAULT NULL,
  `apply_by` varchar(64) DEFAULT NULL, `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `audit_by` varchar(64) DEFAULT NULL, `audit_time` datetime DEFAULT NULL, `audit_remark` varchar(500) DEFAULT NULL,
  `del_flag` char(1) NOT NULL DEFAULT '0', `create_by` varchar(64) DEFAULT '', `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '', `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP, `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_hmma_archive_status` (`archive_id`, `apply_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院产品档案供应商修改申请';
/