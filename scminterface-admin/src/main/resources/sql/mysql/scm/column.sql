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
CALL add_table_column('scm_delivery_detail', 'order_id', 'varchar(64)', '来源订单主键 scm_order.order_id（字符串外键，十进制）', NULL);
/
CALL add_table_column('scm_delivery_detail', 'order_no', 'varchar(50)', '来源订单号（冗余自 scm_order）', '');
/
ALTER TABLE scm_delivery_detail MODIFY COLUMN order_id varchar(64) DEFAULT NULL COMMENT '来源订单主键 scm_order.order_id（字符串外键，十进制）';
/
CALL add_table_column('scm_order', 'spd_tenant_id', 'varchar(64)', 'SPD租户ID(sb_customer.customer_id，推送快照)', NULL);
/
CALL add_table_column('scm_order', 'spd_snapshot_hospital_code', 'varchar(64)', '推送时快照：平台医院编码', NULL);
/
CALL add_table_column('scm_order', 'spd_snapshot_supplier_code', 'varchar(64)', '推送时快照：平台供应商编码', NULL);
/
CALL add_table_column('scm_order', 'hs_bind_snapshot', 'varchar(32)', '下单/推送时医院-供应商绑定关系快照（中文：已绑定、未绑定、申请审核中等）', NULL);
/
CALL add_table_column('zs_tp_order', 'hospital_id', 'bigint(20)', '平台医院主键 scm_hospital.hospital_id', NULL);
/
CALL add_table_column('zs_tp_order', 'supplier_id', 'bigint(20)', '平台供应商主键 scm_supplier.supplier_id', NULL);
/
CALL add_table_column('zs_tp_order', 'hs_bind_snapshot', 'varchar(32)', '落库时医院-供应商绑定关系快照（中文：已绑定、未绑定、申请审核中等）', NULL);
/
-- ========== scm_order_detail：冗余订单号（与 scm-admin column.sql 保持一致）==========
CALL add_table_column('scm_order_detail', 'order_no', 'varchar(50)', '订单编号（冗余自 scm_order，便于按单号查明细）', '');
/
DROP PROCEDURE IF EXISTS `add_table_index`;
/
CREATE PROCEDURE `add_table_index`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_columns VARCHAR(512)
)
add_index_block:
BEGIN
    DECLARE v_index_exists INT DEFAULT 0;
    DECLARE v_table_exists INT DEFAULT 0;
    IF p_table_name IS NULL OR p_table_name = ''
        OR p_index_name IS NULL OR p_index_name = ''
        OR p_index_columns IS NULL OR p_index_columns = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '错误：表名、索引名、索引列为必填参数，不能为空！';
    END IF;
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name;
    IF v_table_exists = 0 THEN
        SELECT CONCAT('跳过：表【', p_table_name, '】不存在') AS add_table_index_result;
        LEAVE add_index_block;
    END IF;
    SELECT COUNT(*) INTO v_index_exists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name;
    IF v_index_exists > 0 THEN
        SELECT CONCAT('提示：索引【', p_index_name, '】已存在于表【', p_table_name, '】，无需重复添加') AS add_table_index_result;
        LEAVE add_index_block;
    END IF;
    SET @dynamic_sql = CONCAT(
        'ALTER TABLE `', p_table_name, '` ADD INDEX `', p_index_name, '` (', p_index_columns, ')'
    );
    PREPARE stmt FROM @dynamic_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    SELECT CONCAT('成功：索引【', p_index_name, '】已添加到表【', p_table_name, '】') AS add_table_index_result;
    SET @dynamic_sql = NULL;
END;
/
DROP PROCEDURE IF EXISTS `migrate_table_unique_key`;
/
CREATE PROCEDURE `migrate_table_unique_key`(
    IN p_table_name VARCHAR(64),
    IN p_old_index_name VARCHAR(64),
    IN p_new_index_name VARCHAR(64),
    IN p_new_index_columns VARCHAR(512)
)
migrate_uk_block:
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;
    DECLARE v_old_exists INT DEFAULT 0;
    DECLARE v_new_exists INT DEFAULT 0;
    IF p_table_name IS NULL OR p_table_name = ''
        OR p_new_index_name IS NULL OR p_new_index_name = ''
        OR p_new_index_columns IS NULL OR p_new_index_columns = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '错误：表名、新唯一键名、新唯一键列为必填参数，不能为空！';
    END IF;
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name;
    IF v_table_exists = 0 THEN
        SELECT CONCAT('跳过：表【', p_table_name, '】不存在') AS migrate_table_unique_key_result;
        LEAVE migrate_uk_block;
    END IF;
    IF p_old_index_name IS NOT NULL AND TRIM(p_old_index_name) <> '' THEN
        SELECT COUNT(*) INTO v_old_exists
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_old_index_name;
        IF v_old_exists > 0 THEN
            SET @dynamic_sql = CONCAT('ALTER TABLE `', p_table_name, '` DROP INDEX `', p_old_index_name, '`');
            PREPARE stmt FROM @dynamic_sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
    SELECT COUNT(*) INTO v_new_exists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_new_index_name;
    IF v_new_exists = 0 THEN
        SET @dynamic_sql = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD UNIQUE KEY `', p_new_index_name, '` (', p_new_index_columns, ')'
        );
        PREPARE stmt FROM @dynamic_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('成功：唯一键【', p_new_index_name, '】已添加到表【', p_table_name, '】') AS migrate_table_unique_key_result;
    ELSE
        SELECT CONCAT('提示：唯一键【', p_new_index_name, '】已存在于表【', p_table_name, '】，无需重复添加') AS migrate_table_unique_key_result;
    END IF;
    SET @dynamic_sql = NULL;
END;
/
CALL add_table_index('scm_order_detail', 'idx_order_no', 'order_no');
/
UPDATE scm_order_detail d
INNER JOIN scm_order o ON o.order_id = d.order_id
SET d.order_no = IFNULL(o.order_no, '')
WHERE TRIM(IFNULL(d.order_no, '')) = '' AND IFNULL(o.order_no, '') <> '';
/
-- ========== 第一方订单：平台供应商/医院冗余（与 scm-admin column.sql 保持一致）==========
CALL add_table_column('scm_order', 'supplier_code', 'varchar(64)', '平台供应商编码（scm_supplier.supplier_code，与 spd_snapshot_supplier_code 同源）', '');
/
CALL add_table_index('scm_order', 'idx_supplier_code', 'supplier_code');
/
UPDATE scm_order
SET supplier_code = IFNULL(spd_snapshot_supplier_code, '')
WHERE TRIM(IFNULL(supplier_code, '')) = '' AND IFNULL(spd_snapshot_supplier_code, '') <> '';
/
CALL add_table_column('scm_order', 'warehouse_code', 'varchar(64)', '订单仓库编码（SPD fd_warehouse.code 快照）', '');
/
CALL add_table_column('scm_order', 'order_dept_code', 'varchar(64)', '订单科室编码（SPD fd_department.code 快照）', '');
/
CALL add_table_column('scm_order_detail', 'hospital_id', 'bigint(20)', '医院ID（冗余自 scm_order，便于行级筛选）', NULL);
/
CALL add_table_column('scm_order_detail', 'hospital_code', 'varchar(64)', '平台医院编码（冗余自 scm_order.spd_snapshot_hospital_code）', '');
/
CALL add_table_column('scm_order_detail', 'supplier_id', 'bigint(20)', '平台供应商ID（冗余自 scm_order.supplier_id）', NULL);
/
CALL add_table_column('scm_order_detail', 'supplier_code', 'varchar(64)', '平台供应商编码（冗余自 scm_order）', '');
/
CALL add_table_index('scm_order_detail', 'idx_od_hospital_id', 'hospital_id');
/
CALL add_table_index('scm_order_detail', 'idx_od_supplier_id', 'supplier_id');
/
CALL add_table_index('scm_order_detail', 'idx_od_supplier_code', 'supplier_code');
/
UPDATE scm_order_detail d
INNER JOIN scm_order o ON o.order_id = d.order_id
SET d.hospital_id = o.hospital_id,
    d.hospital_code = IFNULL(NULLIF(TRIM(d.hospital_code), ''), IFNULL(o.spd_snapshot_hospital_code, '')),
    d.supplier_id = COALESCE(d.supplier_id, o.supplier_id),
    d.supplier_code = IFNULL(NULLIF(TRIM(d.supplier_code), ''), IFNULL(NULLIF(TRIM(o.supplier_code), ''), IFNULL(o.spd_snapshot_supplier_code, '')))
WHERE d.hospital_id IS NULL
   OR TRIM(IFNULL(d.hospital_code, '')) = ''
   OR d.supplier_id IS NULL
   OR TRIM(IFNULL(d.supplier_code, '')) = '';
/

-- ========== SCM-X-002：order_no 按 SPD 租户唯一（多医院可同号）==========
UPDATE scm_order
SET spd_tenant_id = tenant_id
WHERE (spd_tenant_id IS NULL OR TRIM(spd_tenant_id) = '')
  AND tenant_id IS NOT NULL
  AND TRIM(tenant_id) <> '';
/
CALL migrate_table_unique_key('scm_order', 'uk_order_no', 'uk_scm_order_tenant_order_no', 'spd_tenant_id, order_no');
/
CALL add_table_index('scm_order', 'idx_order_no', 'order_no');
/

-- ========== SCM-X-003：配送单幂等（明细订单冗余 + 配送单号按租户唯一）==========
UPDATE scm_delivery
SET spd_tenant_id = tenant_id
WHERE (spd_tenant_id IS NULL OR TRIM(spd_tenant_id) = '')
  AND tenant_id IS NOT NULL
  AND TRIM(tenant_id) <> '';
/
UPDATE scm_delivery_detail dd
INNER JOIN scm_order_detail sod ON sod.detail_id = dd.order_detail_id
SET dd.order_id = CAST(sod.order_id AS CHAR),
    dd.order_no = IFNULL(NULLIF(TRIM(dd.order_no), ''), sod.order_no),
    dd.spd_order_entry_id = COALESCE(dd.spd_order_entry_id, sod.spd_entry_id)
WHERE dd.order_detail_id IS NOT NULL
  AND (dd.order_id IS NULL OR TRIM(IFNULL(dd.order_id, '')) = '' OR TRIM(IFNULL(dd.order_no, '')) = '');
/
UPDATE scm_delivery_detail dd
INNER JOIN scm_delivery d ON d.delivery_id = dd.delivery_id
SET dd.order_id = CAST(d.order_id AS CHAR),
    dd.order_no = IFNULL(NULLIF(TRIM(dd.order_no), ''), d.order_no)
WHERE (dd.order_id IS NULL OR TRIM(IFNULL(dd.order_id, '')) = '')
  AND d.order_id IS NOT NULL;
/
CALL migrate_table_unique_key('scm_delivery', 'uk_delivery_no', 'uk_scm_delivery_tenant_no', 'spd_tenant_id, delivery_no');
/
CALL add_table_index('scm_delivery', 'idx_delivery_no', 'delivery_no');
/
CALL add_table_index('scm_delivery_detail', 'idx_scm_dd_order_id', 'order_id');
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
