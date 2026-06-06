-- =============================================================================
-- 众阳 HIS 接口镜像表 — 增量字段脚本（SPD 业务库，如 aspt）
-- 【非标准对象】仅用于已手工创建的 m_* 镜像表，非 SPD 标准库脚本。
-- 在 01_table.sql 之后执行；按「/」分段执行。
-- 新增 HIS 回参字段时：在本文件末尾追加 CALL add_mirror_column(...)
-- =============================================================================

-- USE `aspt`;
/

DROP PROCEDURE IF EXISTS `add_mirror_column`;
/

CREATE PROCEDURE `add_mirror_column`(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_type VARCHAR(128),
  IN p_column_comment VARCHAR(256),
  IN p_after_column VARCHAR(64)
)
add_column_block:
BEGIN
  DECLARE v_column_exists INT DEFAULT 0;
  DECLARE v_after_exists INT DEFAULT 0;
  SET @dynamic_sql = '';

  IF p_table_name IS NULL OR p_table_name = ''
      OR p_column_name IS NULL OR p_column_name = ''
      OR p_column_type IS NULL OR p_column_type = ''
      OR p_column_comment IS NULL OR p_column_comment = '' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '错误：表名、字段名、字段类型、字段注释为必填参数';
  END IF;

  SELECT COUNT(*) INTO v_column_exists
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table_name
    AND COLUMN_NAME = p_column_name;

  IF v_column_exists > 0 THEN
    SELECT CONCAT('提示：字段【', p_column_name, '】已存在于表【', p_table_name, '】，跳过') AS 执行结果;
    LEAVE add_column_block;
  END IF;

  SET @dynamic_sql = CONCAT(
    'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_type,
    ' NULL COMMENT ', QUOTE(p_column_comment)
  );

  IF p_after_column IS NOT NULL AND p_after_column != '' THEN
    SELECT COUNT(*) INTO v_after_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_after_column;
    IF v_after_exists > 0 THEN
      SET @dynamic_sql = CONCAT(@dynamic_sql, ' AFTER `', p_after_column, '`');
    END IF;
  END IF;

  PREPARE stmt FROM @dynamic_sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;

  SELECT CONCAT('成功：字段【', p_column_name, '】已添加到表【', p_table_name, '】') AS 执行结果;
  SET @dynamic_sql = '';
END;
/

-- ========== tenant_id（已建表环境增量追加）==========
CALL add_mirror_column('m_sync_batch', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_dept', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_dept_category_rel', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_user_identity', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_user_identity_account', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_drug_dict', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_dict_category', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_supplier', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_producer', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_yk_instock', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_yk_instock_detail', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_merge_stock', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
CALL add_mirror_column('m_drug_batch_stock', 'tenant_id', 'varchar(64) NOT NULL DEFAULT ''''', 'SPD租户ID，枣强=zaoqiang-tcm-001', 'hospital_key');
/
UPDATE m_sync_batch SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_dept SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_dept_category_rel SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_user_identity SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_user_identity_account SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_drug_dict SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_dict_category SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_supplier SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_producer SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_yk_instock SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_yk_instock_detail SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_merge_stock SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
UPDATE m_drug_batch_stock SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
-- ========== insert_time / update_time（由 mirror_time 迁移）==========
CALL add_mirror_column('m_sync_batch', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'remark');
/
CALL add_mirror_column('m_sync_batch', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_dept', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'mirror_source');
/
CALL add_mirror_column('m_dept', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_dept_category_rel', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'category_id');
/
CALL add_mirror_column('m_dept_category_rel', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_user_identity', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'mirror_source');
/
CALL add_mirror_column('m_user_identity', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_user_identity_account', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'account_no');
/
CALL add_mirror_column('m_user_identity_account', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_drug_dict', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'mirror_source');
/
CALL add_mirror_column('m_drug_dict', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_dict_category', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'mirror_source');
/
CALL add_mirror_column('m_dict_category', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_supplier', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'mirror_source');
/
CALL add_mirror_column('m_supplier', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_producer', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'mirror_source');
/
CALL add_mirror_column('m_producer', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_yk_instock', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'mirror_source');
/
CALL add_mirror_column('m_yk_instock', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_yk_instock_detail', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'storage_instock_id');
/
CALL add_mirror_column('m_yk_instock_detail', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_merge_stock', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'mirror_source');
/
CALL add_mirror_column('m_merge_stock', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
CALL add_mirror_column('m_drug_batch_stock', 'insert_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP', '插入时间', 'mirror_source');
/
CALL add_mirror_column('m_drug_batch_stock', 'update_time', 'datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', '更新时间', 'insert_time');
/
UPDATE m_sync_batch SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_dept SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_dept_category_rel SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_user_identity SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_user_identity_account SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_drug_dict SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_dict_category SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_supplier SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_producer SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_yk_instock SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_yk_instock_detail SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_merge_stock SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
UPDATE m_drug_batch_stock SET insert_time = mirror_time, update_time = mirror_time WHERE mirror_time IS NOT NULL;
/
-- ========== 2.5.43 镜像列与 API 对齐（评估文档附录 B）==========
CALL add_mirror_column('m_drug_batch_stock', 'pharmacy_stock_id', 'varchar(64)', '2.5.43 pharmacyStockId', 'stock_id');
/
CALL add_mirror_column('m_drug_batch_stock', 'stock_amount', 'decimal(18,4)', '2.5.43 stockAmount', 'quantity');
/
CALL add_mirror_column('m_drug_batch_stock', 'yc_stock_query_id', 'varchar(64)', '2.5.43 ycStockQueryId', 'stock_amount');
/
