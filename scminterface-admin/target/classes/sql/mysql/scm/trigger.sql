-- ========== SCM 库：ZS 表触发器（逻辑删除时补全 del_time）==========
-- 按「/」分段执行；单段内为完整 CREATE TRIGGER
/
DROP TRIGGER IF EXISTS tr_zs_tp_order_bu_del_time;
/
CREATE TRIGGER tr_zs_tp_order_bu_del_time
BEFORE UPDATE ON zs_tp_order
FOR EACH ROW
SET NEW.del_time = IF(NEW.del_flag = '1' AND NEW.del_time IS NULL, NOW(), NEW.del_time);
/
DROP TRIGGER IF EXISTS tr_zs_tp_order_detail_bu_del_time;
/
CREATE TRIGGER tr_zs_tp_order_detail_bu_del_time
BEFORE UPDATE ON zs_tp_order_detail
FOR EACH ROW
SET NEW.del_time = IF(NEW.del_flag = '1' AND NEW.del_time IS NULL, NOW(), NEW.del_time);
