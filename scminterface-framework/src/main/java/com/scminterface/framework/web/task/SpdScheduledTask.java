package com.scminterface.framework.web.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.web.service.ScheduledTaskService;

/**
 * SPD定时任务
 * 
 * @author scminterface
 */
@Component
public class SpdScheduledTask
{
    private static final Logger log = LoggerFactory.getLogger(SpdScheduledTask.class);

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    /**
     * 执行SPD定时任务
     */
    @DataSource(DataSourceType.SPD)
    public void execute()
    {
        try
        {
            // 获取任务配置
            Map<String, Object> taskConfig = scheduledTaskService.getSpdTaskConfig();
            if (taskConfig == null)
            {
                log.warn("SPD定时任务配置不存在");
                return;
            }

            // 检查任务状态
            String status = (String) taskConfig.get("status");
            if (!"0".equals(status))
            {
                log.debug("SPD定时任务已停用，跳过执行");
                return;
            }

            // 检查执行次数限制
            Integer maxExecCount = (Integer) taskConfig.get("maxExecCount");
            Integer currentExecCount = (Integer) taskConfig.get("currentExecCount");
            if (maxExecCount != null && maxExecCount >= 0 && currentExecCount != null && currentExecCount >= maxExecCount)
            {
                log.info("SPD定时任务已达到最大执行次数限制: {}/{}", currentExecCount, maxExecCount);
                return;
            }

            // 执行任务：输出当前时间到日志
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTime = sdf.format(new Date());
            log.info("========== SPD定时任务执行 ==========");
            log.info("当前时间: {}", currentTime);
            log.info("执行次数: {}", currentExecCount);
            log.info("===================================");

            // 增加执行次数
            scheduledTaskService.incrementSpdExecCount();
        }
        catch (Exception e)
        {
            log.error("SPD定时任务执行异常", e);
        }
    }
}
