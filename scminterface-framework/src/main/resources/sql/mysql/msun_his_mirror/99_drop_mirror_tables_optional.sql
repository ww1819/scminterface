-- =============================================================================
-- 【可选】删除全部众阳（msun）HIS 镜像表（新客户标准库初始化后清理用）
-- =============================================================================
-- 命名：m_msun_* 。执行前请 USE 目标 SPD 业务库（如 aspt）。按「/」分段执行。
-- 正常运行依赖 auto-schema 自动建表，一般无需手工执行 01/02；仅清理时执行本脚本。
-- =============================================================================

-- USE `aspt`;
/

DROP TABLE IF EXISTS `m_msun_yk_instock_detail`;
/
DROP TABLE IF EXISTS `m_msun_yk_instock`;
/
DROP TABLE IF EXISTS `m_msun_push_log`;
/
DROP TABLE IF EXISTS `m_msun_merge_stock`;
/
DROP TABLE IF EXISTS `m_msun_drug_batch_stock`;
/
DROP TABLE IF EXISTS `m_msun_producer`;
/
DROP TABLE IF EXISTS `m_msun_supplier`;
/
DROP TABLE IF EXISTS `m_msun_dict_category`;
/
DROP TABLE IF EXISTS `m_msun_drug_dict`;
/
DROP TABLE IF EXISTS `m_msun_user_identity_account`;
/
DROP TABLE IF EXISTS `m_msun_user_identity`;
/
DROP TABLE IF EXISTS `m_msun_dept_category_rel`;
/
DROP TABLE IF EXISTS `m_msun_dept`;
/
DROP TABLE IF EXISTS `m_msun_sync_batch`;
/

DROP PROCEDURE IF EXISTS `add_mirror_column`;
/
