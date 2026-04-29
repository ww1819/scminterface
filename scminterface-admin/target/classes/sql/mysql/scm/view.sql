-- ========== SCM 库：接口服务相关视图 ==========
-- 按「/」分段执行
/
DROP VIEW IF EXISTS v_zs_tp_order_active;
/
CREATE VIEW v_zs_tp_order_active AS
SELECT
  id,
  third_party_pk,
  customer,
  sheet_je,
  dh,
  supno,
  sup,
  ckno,
  ck,
  pc,
  oper,
  jsfs,
  scm_sup_code,
  scm_hospital_code,
  scm_hospital_id,
  scm_supplier_id,
  create_time,
  update_time
FROM zs_tp_order
WHERE del_flag = '0';
/
DROP VIEW IF EXISTS v_zs_tp_order_detail_active;
/
CREATE VIEW v_zs_tp_order_detail_active AS
SELECT
  d.id,
  d.order_id,
  d.third_party_pk,
  d.dh,
  d.code,
  d.name,
  d.gg,
  d.dw,
  d.sl,
  d.dj,
  d.je,
  d.sccj,
  d.zcz,
  d.create_time
FROM zs_tp_order_detail d
WHERE d.del_flag = '0';
