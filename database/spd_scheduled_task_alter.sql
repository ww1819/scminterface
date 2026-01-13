-- 修改SPD定时任务表结构，添加task_class和task_method字段
ALTER TABLE `spd_scheduled_task` 
ADD COLUMN `task_class` VARCHAR(200) DEFAULT NULL COMMENT '任务类全限定名' AFTER `task_name`,
ADD COLUMN `task_method` VARCHAR(100) DEFAULT NULL COMMENT '任务方法名' AFTER `task_class`;

-- 修改唯一索引，支持同一个类下的不同方法
ALTER TABLE `spd_scheduled_task` 
DROP INDEX `uk_task_name`;

-- 添加新的唯一索引，确保同一个类的方法组合唯一
ALTER TABLE `spd_scheduled_task` 
ADD UNIQUE KEY `uk_task_class_method` (`task_class`, `task_method`);

-- 更新现有数据，设置默认值
UPDATE `spd_scheduled_task` 
SET `task_class` = 'com.scminterface.framework.web.task.SpdScheduledTask',
    `task_method` = 'execute'
WHERE `task_name` = 'SPD定时任务' AND (`task_class` IS NULL OR `task_method` IS NULL);
