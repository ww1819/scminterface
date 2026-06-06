-- =============================================================================
-- 众阳 HIS 接口镜像表 — 增量字段脚本（SPD 业务库，如 aspt）
-- 表名规范：m_msun_*（见 01_table.sql）；按「/」分段执行。
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

-- ========== 2.5.43 镜像列与 API 对齐（评估文档附录 B）==========
CALL add_mirror_column('m_msun_drug_batch_stock', 'pharmacy_stock_id', 'varchar(64)', '2.5.43 pharmacyStockId', 'stock_id');
/
CALL add_mirror_column('m_msun_drug_batch_stock', 'stock_amount', 'decimal(18,4)', '2.5.43 stockAmount', 'quantity');
/
CALL add_mirror_column('m_msun_drug_batch_stock', 'yc_stock_query_id', 'varchar(64)', '2.5.43 ycStockQueryId', 'stock_amount');
/
