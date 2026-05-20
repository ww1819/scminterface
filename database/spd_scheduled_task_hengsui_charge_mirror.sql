-- 衡水三院：住院/门诊计费镜像默认定时（每 2 小时；抓取昨天+今天，见 HisChargeMirrorFetchSql）
-- 已有 task_class+task_method 时仅更新 Cron 为每 2 小时（可按需注释掉 UPDATE 保留现场自定义）

INSERT INTO `spd_scheduled_task` (
  `task_name`, `task_class`, `task_method`, `cron_expression`, `max_exec_count`, `current_exec_count`, `status`
) VALUES
(
  '衡水住院收费镜像同步',
  'com.scminterface.framework.web.task.HengshuiTask',
  'syncInpatientCharge',
  '0 0 0/2 * * ?',
  -1, 0, '0'
),
(
  '衡水门诊收费镜像同步',
  'com.scminterface.framework.web.task.HengshuiTask',
  'syncOutpatientCharge',
  '0 0 0/2 * * ?',
  -1, 0, '0'
)
ON DUPLICATE KEY UPDATE
  `cron_expression` = VALUES(`cron_expression`),
  `task_name` = VALUES(`task_name`),
  `status` = '0';
