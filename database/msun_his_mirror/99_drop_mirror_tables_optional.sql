-- =============================================================================
-- 【可选】删除全部众阳 HIS 镜像表（新客户标准库初始化后清理用）
-- =============================================================================
-- m_* 为【非标准对象】，不属于 SPD 标准库结构。新客户若不需要接口镜像落库，可执行本脚本。
-- 执行前请 USE 目标 SPD 业务库（如 aspt）。按「/」分段执行。
-- =============================================================================

-- USE `aspt`;
/

DROP TABLE IF EXISTS `m_yk_instock_detail`;
/
DROP TABLE IF EXISTS `m_yk_instock`;
/
DROP TABLE IF EXISTS `m_merge_stock`;
DROP TABLE IF EXISTS `m_drug_batch_stock`;
/
DROP TABLE IF EXISTS `m_producer`;
/
DROP TABLE IF EXISTS `m_supplier`;
/
DROP TABLE IF EXISTS `m_dict_category`;
/
DROP TABLE IF EXISTS `m_drug_dict`;
/
DROP TABLE IF EXISTS `m_user_identity_account`;
/
DROP TABLE IF EXISTS `m_user_identity`;
/
DROP TABLE IF EXISTS `m_dept_category_rel`;
/
DROP TABLE IF EXISTS `m_dept`;
/
DROP TABLE IF EXISTS `m_sync_batch`;
/
DROP PROCEDURE IF EXISTS `add_mirror_column`;
/
