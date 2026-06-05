-- =============================================================================
-- 众阳 HIS 接口镜像库 — 建库脚本（实施人员手工执行）
-- 建议库名：msun_his_mirror
-- 说明：独立于 SCM/SPD 正式库，仅存接口探针/同步镜像数据，便于联调与对账
-- 执行顺序：00 → 01 → 02 → 03（样本落库，可选）
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `msun_his_mirror`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT '众阳HIS接口镜像库';

USE `msun_his_mirror`;
