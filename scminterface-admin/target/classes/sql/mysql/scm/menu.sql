-- ========== SCM 库：sys_menu 菜单（需已存在 sys_menu 表）==========
-- 按「/」分段执行；INSERT IGNORE 可重复执行
-- 列与 scm-admin sql/mysql/scm/menu.sql 保持一致
/
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, url, target, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
('2910', '接口与对接', '0', '20', '#', '', 'M', '0', '1', '', 'fa fa-plug', 'admin', NOW(), '', NULL, 'SCMInterface 第三方对接目录');
/
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, url, target, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
('2911', '第三方推送订单', '2910', '1', '/interface/zsTpOrder', '', 'C', '0', '1', 'interface:zsTp:view', 'fa fa-cloud-download', 'admin', NOW(), '', NULL, 'ZS 推送订单（占位路由，前端可按需实现）');
/
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, url, target, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
('2912', '推送订单查询', '2911', '1', '#', '', 'F', '0', '1', 'interface:zsTp:query', '#', 'admin', NOW(), '', NULL, '');
