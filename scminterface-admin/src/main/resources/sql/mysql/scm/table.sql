-- ========== SCM 库（由 scm-admin column.sql 内嵌建表合并而来）==========
-- 与 scm-admin/src/main/resources/sql/mysql/scm/table.sql 末尾四张表定义一致，供 scminterface 单独初始化或对照。
-- 主键 UUID7 均为 VARCHAR(36)，与标准 UUID 字符串（含连字符）一致。
/

CREATE TABLE IF NOT EXISTS `scm_order_detail_delivery_rel` (
  `id`                  VARCHAR(36)  NOT NULL COMMENT '主键UUID7',
  `order_detail_id`     VARCHAR(64)  NOT NULL COMMENT '订单明细ID',
  `order_id`            VARCHAR(64)  NOT NULL COMMENT '订单ID',
  `order_no`            VARCHAR(128) DEFAULT '' COMMENT '订单号',
  `delivery_id`         VARCHAR(64)  NOT NULL COMMENT '配送单ID',
  `delivery_no`         VARCHAR(128) DEFAULT '' COMMENT '配送单号',
  `delivery_detail_id`  VARCHAR(64)  NOT NULL COMMENT '配送单明细ID',
  `create_time`         VARCHAR(32)  DEFAULT NULL COMMENT '添加时间',
  `create_by`           VARCHAR(64)  DEFAULT NULL COMMENT '添加人ID',
  `tenant_id`           VARCHAR(64)  DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_soddr_order_detail` (`order_detail_id`),
  KEY `idx_soddr_order` (`order_id`),
  KEY `idx_soddr_delivery` (`delivery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我方订单明细与配送单明细关联';
/

CREATE TABLE IF NOT EXISTS `zs_tp_order_detail_delivery_rel` (
  `id`                  VARCHAR(36)  NOT NULL COMMENT '主键UUID7',
  `order_detail_id`     VARCHAR(64)  NOT NULL COMMENT '第三方订单明细ID',
  `order_id`            VARCHAR(64)  NOT NULL COMMENT '第三方订单ID',
  `order_no`            VARCHAR(128) DEFAULT '' COMMENT '订单号(DH)',
  `delivery_id`         VARCHAR(64)  NOT NULL COMMENT '配送单ID',
  `delivery_no`         VARCHAR(128) DEFAULT '' COMMENT '配送单号',
  `delivery_detail_id`  VARCHAR(64)  NOT NULL COMMENT '配送单明细ID',
  `create_time`         VARCHAR(32)  DEFAULT NULL COMMENT '添加时间',
  `create_by`           VARCHAR(64)  DEFAULT NULL COMMENT '添加人ID',
  `tenant_id`           VARCHAR(64)  DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_zsoddr_order_detail` (`order_detail_id`),
  KEY `idx_zsoddr_order` (`order_id`),
  KEY `idx_zsoddr_delivery` (`delivery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方订单明细与配送单明细关联';
/

CREATE TABLE IF NOT EXISTS `scm_barcode_seed` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID7',
  `counter_type` char(1) NOT NULL COMMENT 'T=按租户维度 Z=按第三方客户维度',
  `tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户ID',
  `zs_customer_id` varchar(128) NOT NULL DEFAULT '' COMMENT '第三方客户ID(customer)',
  `warehouse_id` varchar(128) NOT NULL DEFAULT '' COMMENT '仓库ID；第三方订单种子暂固定空串仅按高低值区分，保留列便于将来按仓扩展',
  `high_low_flag` char(1) NOT NULL DEFAULT 'L' COMMENT '高低值：H高值 L低值',
  `seed_value` bigint(20) NOT NULL DEFAULT 0 COMMENT '已分配的最大种子序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scm_barcode_seed` (`counter_type`,`tenant_id`,`zs_customer_id`,`warehouse_id`,`high_low_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方条码种子序列表';
/

CREATE TABLE IF NOT EXISTS `scm_delivery_detail_barcode` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID7',
  `delivery_id` bigint(20) NOT NULL COMMENT '配送单ID',
  `delivery_no` varchar(50) NOT NULL DEFAULT '' COMMENT '配送单号',
  `delivery_detail_id` bigint(20) NOT NULL COMMENT '配送单明细ID',
  `seed_num` bigint(20) NOT NULL COMMENT '种子序号',
  `barcode_no` varchar(128) NOT NULL DEFAULT '' COMMENT '条码号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ddbc_delivery` (`delivery_id`),
  KEY `idx_ddbc_detail` (`delivery_detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送单明细条码从表';
/
