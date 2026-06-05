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
UPDATE m_drug_batch_stock SET tenant_id = hospital_key WHERE tenant_id = '' OR tenant_id IS NULL;
/
-- ========== 预留：后续 HIS 回参新增字段在此追加 ==========
-- CALL add_mirror_column('m_drug_dict', 'new_field_from_his', 'varchar(128)', 'HIS新增字段说明', 'org_id');
/
