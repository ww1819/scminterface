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
