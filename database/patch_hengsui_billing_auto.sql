-- 衡水三院：scminterface 调用 SPD 计费自动处理所需配置（在耗材 SPD 主库执行）
-- 系统级参数写入 sys_config（与参数设置一致）；租户计费开关写入 sb_tenant_setting。

-- 系统参数：SPD 服务基址（scminterface 同步镜像后 POST 内部接口，按实际部署修改）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT
  'SPD内部接口基址',
  'spd.internal.base_url',
  'http://127.0.0.1:8080',
  'N',
  'admin',
  NOW(),
  'scminterface 调用 SPD /his/internal/patientCharge/processFetchBatch 的基址（含协议，不含末尾斜杠）。'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'spd.internal.base_url');

-- 系统参数：内部接口密钥（SPD 校验请求头 X-Spd-Internal-Key，请改为强随机串）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT
  'HIS计费内部接口密钥',
  'his.internal.api_key',
  'change-me-internal-key',
  'N',
  'admin',
  NOW(),
  'scminterface 与 SPD 内部计费处理接口共享密钥，请求头 X-Spd-Internal-Key。'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'his.internal.api_key');

-- 租户设置：计费自动开关（与 SPD「HIS计费自动处理」页、sb_tenant_setting 同源）
INSERT INTO sb_tenant_setting (id, tenant_id, setting_key, setting_value, remark, del_flag, create_by, create_time, update_by, update_time)
SELECT REPLACE(UUID(), '-', ''), 'hengsui-third-001', 'dept.billing.lv.auto_consume_enabled', '0', '低值计费抓取后自动生成消耗', 0, 'admin', NOW(), 'admin', NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sb_tenant_setting WHERE tenant_id = 'hengsui-third-001' AND setting_key = 'dept.billing.lv.auto_consume_enabled' AND IFNULL(del_flag, 0) = 0
);

INSERT INTO sb_tenant_setting (id, tenant_id, setting_key, setting_value, remark, del_flag, create_by, create_time, update_by, update_time)
SELECT REPLACE(UUID(), '-', ''), 'hengsui-third-001', 'dept.billing.auto_refund_enabled', '0', '计费退费镜像抓取后自动返还库存', 0, 'admin', NOW(), 'admin', NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sb_tenant_setting WHERE tenant_id = 'hengsui-third-001' AND setting_key = 'dept.billing.auto_refund_enabled' AND IFNULL(del_flag, 0) = 0
);
