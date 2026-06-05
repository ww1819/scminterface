-- =============================================================================
-- 众阳 HIS 镜像 → SPD 主数据同步 — SPD 业务库增量字段与组合唯一键
-- =============================================================================
-- 执行库：SPD 业务库（如 aspt），在 01_table.sql 镜像表建好后执行。
-- 作用：为 fd_* / sys_user 补齐 HIS 对照列与 UPSERT 所需的组合唯一键。
-- 若索引/列已存在，对应语句会报错，可跳过该段。
-- =============================================================================

-- USE `aspt`;
/

-- 科室：tenant + HIS科室ID 唯一
ALTER TABLE `fd_department`
  ADD UNIQUE KEY `uk_fd_department_tenant_his` (`tenant_id`, `his_id`);
/

-- 耗材：众阳HIS产品档案唯一键（m_drug_dict.drug_spec_packing_id）
ALTER TABLE `fd_material`
  ADD COLUMN `his_spec_packing_id` VARCHAR(64) DEFAULT NULL COMMENT '众阳HIS产品档案唯一键（drug_spec_packing_id）' AFTER `his_id`;
/

-- his_spec_packing_id = 众阳HIS产品档案唯一键；his_id 存 drug_id 作辅助对照
ALTER TABLE `fd_material`
  ADD UNIQUE KEY `uk_fd_material_tenant_his_spec` (`tenant_id`, `his_id`, `his_spec_packing_id`);
/

-- 库房分类（2.5.58）：tenant + HIS分类ID 唯一
ALTER TABLE `fd_warehouse_category`
  ADD UNIQUE KEY `uk_fd_wh_cat_tenant_his` (`tenant_id`, `his_id`);
/

-- 计量单位：众阳最小包装单位（m_drug_dict.min_packing_id / min_packing_name）
ALTER TABLE `fd_unit`
  ADD COLUMN `his_unit_id` VARCHAR(64) DEFAULT NULL COMMENT '众阳HIS最小包装单位ID（min_packing_id）' AFTER `unit_name`;
/

ALTER TABLE `fd_unit`
  ADD UNIQUE KEY `uk_fd_unit_tenant_his` (`tenant_id`, `his_unit_id`);
/

-- 用户：HIS 身份ID（众阳 identity_id，一用户多身份）
ALTER TABLE `sys_user`
  ADD COLUMN `his_identity_id` VARCHAR(64) DEFAULT NULL COMMENT 'HIS用户身份ID（众阳 identity_id）' AFTER `his_id`;
/

ALTER TABLE `sys_user`
  ADD UNIQUE KEY `uk_sys_user_customer_his_identity` (`customer_id`, `his_identity_id`);
/

-- 用户-科室关联：防重复插入
ALTER TABLE `sys_user_department`
  ADD UNIQUE KEY `uk_sys_user_dept` (`user_id`, `department_id`);
/
