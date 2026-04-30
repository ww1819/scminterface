-- ========== SCM 库：增量字段（含 add_table_column 存储过程）==========
-- 在 table.sql 之后执行；按「/」分段执行。
-- 可在此追加：CALL add_table_column('zs_tp_order', '新列', 'varchar(64)', '注释', NULL);
-- 先删除再创建，保证可重复执行
/
DROP PROCEDURE IF EXISTS `add_table_column`;
/
CREATE PROCEDURE `add_table_column`(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_type VARCHAR(64),
  IN p_column_comment VARCHAR(256),
  IN p_default_value VARCHAR(256)
)
add_column_block:
BEGIN
  DECLARE v_column_exists INT DEFAULT 0;
  SET p_default_value = IFNULL(p_default_value, NULL);
  SET @dynamic_sql = '';
  IF p_table_name IS NULL OR p_table_name = ''
      OR p_column_name IS NULL OR p_column_name = ''
      OR p_column_type IS NULL OR p_column_type = ''
      OR p_column_comment IS NULL OR p_column_comment = '' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '错误：表名、字段名、字段类型、字段注释为必填参数，不能为空！';
  END IF;
  SELECT COUNT(*) INTO v_column_exists
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table_name
    AND COLUMN_NAME = p_column_name;
  IF v_column_exists > 0 THEN
    SELECT CONCAT('提示：字段【', p_column_name, '】已存在于表【', p_table_name, '】，无需重复添加') AS 执行结果;
    LEAVE add_column_block;
  END IF;
  SET @dynamic_sql = CONCAT(
    'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_type, ' '
  );
  IF p_default_value IS NOT NULL AND p_default_value != '' THEN
    SET @dynamic_sql = CONCAT(@dynamic_sql, 'DEFAULT ', QUOTE(p_default_value), ' ');
  END IF;
  SET @dynamic_sql = CONCAT(@dynamic_sql, 'COMMENT ', QUOTE(p_column_comment));
  PREPARE stmt FROM @dynamic_sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
  SELECT CONCAT('成功：字段【', p_column_name, '】已成功添加到表【', p_table_name, '】') AS 执行结果;
  SET @dynamic_sql = '';
END;
/
-- ========== 以下为 ZS 表后续增量列示例（无需求可保持注释）==========
-- CALL add_table_column('zs_tp_order', 'ext_json', 'varchar(2000)', '扩展JSON', NULL);
/
CALL add_table_column('zs_tp_order', 'scm_sup_code', 'varchar(64)', '接口 SCMSUPCODE：SCM平台供应商编码（客户端随单传递）', NULL);
/
CALL add_table_column('zs_tp_order', 'scm_hospital_code', 'varchar(64)', '入参NEWCUSTOMER：SCM医院编码', NULL);
/
CALL add_table_column('zs_tp_order', 'scm_hospital_id', 'varchar(64)', '由hospital_code解析的hospital_id', NULL);
/
CALL add_table_column('zs_tp_order', 'scm_supplier_id', 'varchar(64)', '由scm_sup_code解析的supplier_id', NULL);
/
-- ========== 订单/配送：SPD 第一方对账扩展列（与 scm-admin column.sql 保持一致）==========
CALL add_table_column('scm_order', 'spd_order_id', 'bigint(20)', 'SPD院内采购订单主键 purchase_order.id（第一方推送对账）', NULL);
/
CALL add_table_column('scm_order', 'source_system', 'varchar(32)', '订单来源系统编码：SPD第一方推送等', NULL);
/
CALL add_table_column('scm_order_detail', 'spd_entry_id', 'bigint(20)', 'SPD采购订单明细主键 purchase_order_entry.id（行级对账）', NULL);
/
CALL add_table_column('scm_delivery', 'spd_tenant_id', 'varchar(64)', 'SPD租户ID（同 sb_customer.customer_id）', NULL);
/
CALL add_table_column('scm_delivery', 'spd_ref_no', 'varchar(128)', 'SPD侧引用/业务流水号（审计）', '');
/
CALL add_table_column('scm_delivery_detail', 'spd_order_entry_id', 'bigint(20)', 'SPD采购订单明细ID purchase_order_entry.id', NULL);
/
CALL add_table_column('scm_order', 'spd_tenant_id', 'varchar(64)', 'SPD租户ID(sb_customer.customer_id，推送快照)', NULL);
/
CALL add_table_column('scm_order', 'spd_snapshot_hospital_code', 'varchar(64)', '推送时快照：平台医院编码', NULL);
/
CALL add_table_column('scm_order', 'spd_snapshot_supplier_code', 'varchar(64)', '推送时快照：平台供应商编码', NULL);
/

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
