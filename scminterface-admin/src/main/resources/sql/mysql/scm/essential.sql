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
