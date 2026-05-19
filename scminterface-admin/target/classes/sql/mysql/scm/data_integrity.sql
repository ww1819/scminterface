-- ========== SCM 库：数据完整性（ZS 相关轻量校验）==========
-- 在 table/column/menu 之后执行；按「/」分段执行
-- 若 zs 表尚未创建，以下语句由启动器跳过或失败时仅记录日志（视 fail-on-error 配置）
/
UPDATE zs_tp_order SET del_flag = '0' WHERE del_flag IS NULL OR del_flag = '';
/
UPDATE zs_tp_order_detail SET del_flag = '0' WHERE del_flag IS NULL OR del_flag = '';
