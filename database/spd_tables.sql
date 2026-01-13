-- SPD数据库表结构

-- 系统参数配置表
CREATE TABLE IF NOT EXISTS `spd_system_config` (
  `config_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值',
  `config_desc` VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SPD系统参数配置表';

-- 定时任务表
CREATE TABLE IF NOT EXISTS `spd_scheduled_task` (
  `task_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_name` VARCHAR(100) NOT NULL DEFAULT 'SPD定时任务' COMMENT '任务名称',
  `cron_expression` VARCHAR(50) NOT NULL DEFAULT '0 0/5 * * * ?' COMMENT 'Cron表达式',
  `max_exec_count` INT(11) DEFAULT -1 COMMENT '最大执行次数，-1表示无限制',
  `current_exec_count` INT(11) DEFAULT 0 COMMENT '当前执行次数',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态：0-启用，1-停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_task_name` (`task_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SPD定时任务配置表';

-- 初始化定时任务数据
INSERT INTO `spd_scheduled_task` (`task_name`, `cron_expression`, `max_exec_count`, `current_exec_count`, `status`) 
VALUES ('SPD定时任务', '0 0 1 * * ?', -1, 0, '0')
ON DUPLICATE KEY UPDATE `task_name` = `task_name`;
