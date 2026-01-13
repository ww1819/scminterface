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
import com.scminterface.framework.web.service.HengshuiTaskService;
import com.scminterface.framework.web.service.ScheduledTaskService;

/**
 * 衡水定时任务
 * 
 * @author scminterface
 */
@Component
public class HengshuiTask
{
    private static final Logger log = LoggerFactory.getLogger(HengshuiTask.class);

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @Autowired
    private HengshuiTaskService hengshuiTaskService;

    /**
     * 同步收费项目数据
     * 从HIS数据库的v_charge_item视图读取数据，保存到SPD数据库的his_hc_info表
     */
    @DataSource(DataSourceType.SPD)
    public void syncChargeItem()
    {
        try
        {
            // 获取任务配置
            Map<String, Object> taskConfig = scheduledTaskService.getTaskConfigByClassAndMethod(
                "com.scminterface.framework.web.task.HengshuiTask", "syncChargeItem");
            
            if (taskConfig == null)
            {
                log.warn("衡水定时任务配置不存在");
                return;
            }

            // 检查任务状态
            String status = (String) taskConfig.get("status");
            if (!"0".equals(status))
            {
                log.debug("衡水定时任务已停用，跳过执行");
                return;
            }

            // 检查执行次数限制
            Integer maxExecCount = (Integer) taskConfig.get("maxExecCount");
            Integer currentExecCount = (Integer) taskConfig.get("currentExecCount");
            if (maxExecCount != null && maxExecCount >= 0 && currentExecCount != null && currentExecCount >= maxExecCount)
            {
                log.info("衡水定时任务已达到最大执行次数限制: {}/{}", currentExecCount, maxExecCount);
                return;
            }

            // 执行任务：同步收费项目数据
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTime = sdf.format(new Date());
            log.info("========== 衡水定时任务执行开始 ==========");
            log.info("当前时间: {}", currentTime);
            log.info("执行次数: {}", currentExecCount);

            Map<String, Object> syncResult = hengshuiTaskService.syncChargeItem();
            
            if (syncResult != null && Boolean.TRUE.equals(syncResult.get("success")))
            {
                log.info("同步结果: {}", syncResult.get("message"));
                log.info("总计: {}, 成功: {}, 失败: {}", 
                    syncResult.get("totalCount"), 
                    syncResult.get("successCount"), 
                    syncResult.get("errorCount"));
            }
            else
            {
                log.error("同步失败: {}", syncResult != null ? syncResult.get("message") : "未知错误");
            }

            log.info("===================================");

            // 增加执行次数
            scheduledTaskService.incrementTaskExecCount(
                "com.scminterface.framework.web.task.HengshuiTask", "syncChargeItem");
        }
        catch (Exception e)
        {
            log.error("衡水定时任务执行异常", e);
        }
    }

    /**
     * 同步住院收费明细数据
     * 从HIS数据库的v_inpatient_consumable_charge视图读取最近3天的数据，保存到SPD数据库的his_zy_sfmx表
     */
    @DataSource(DataSourceType.SPD)
    public void syncInpatientCharge()
    {
        try
        {
            // 获取任务配置
            Map<String, Object> taskConfig = scheduledTaskService.getTaskConfigByClassAndMethod(
                "com.scminterface.framework.web.task.HengshuiTask", "syncInpatientCharge");
            
            if (taskConfig == null)
            {
                log.warn("衡水住院收费明细定时任务配置不存在");
                return;
            }

            // 检查任务状态
            String status = (String) taskConfig.get("status");
            if (!"0".equals(status))
            {
                log.debug("衡水住院收费明细定时任务已停用，跳过执行");
                return;
            }

            // 检查执行次数限制
            Integer maxExecCount = (Integer) taskConfig.get("maxExecCount");
            Integer currentExecCount = (Integer) taskConfig.get("currentExecCount");
            if (maxExecCount != null && maxExecCount >= 0 && currentExecCount != null && currentExecCount >= maxExecCount)
            {
                log.info("衡水住院收费明细定时任务已达到最大执行次数限制: {}/{}", currentExecCount, maxExecCount);
                return;
            }

            // 执行任务：同步住院收费明细数据
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTime = sdf.format(new Date());
            log.info("========== 衡水住院收费明细定时任务执行开始 ==========");
            log.info("当前时间: {}", currentTime);
            log.info("执行次数: {}", currentExecCount);

            Map<String, Object> syncResult = hengshuiTaskService.syncInpatientCharge();
            
            if (syncResult != null && Boolean.TRUE.equals(syncResult.get("success")))
            {
                log.info("同步结果: {}", syncResult.get("message"));
                log.info("总计: {}, 新增: {}, 重复: {}, 成功: {}, 失败: {}", 
                    syncResult.get("totalCount"),
                    syncResult.get("newCount"),
                    syncResult.get("duplicateCount"),
                    syncResult.get("successCount"), 
                    syncResult.get("errorCount"));
            }
            else
            {
                log.error("同步失败: {}", syncResult != null ? syncResult.get("message") : "未知错误");
            }

            log.info("===================================");

            // 增加执行次数
            scheduledTaskService.incrementTaskExecCount(
                "com.scminterface.framework.web.task.HengshuiTask", "syncInpatientCharge");
        }
        catch (Exception e)
        {
            log.error("衡水住院收费明细定时任务执行异常", e);
        }
    }

    /**
     * 同步门诊收费明细数据
     * 从HIS数据库的v_outpatient_consumable_charge视图读取最近3天的数据，保存到SPD数据库的his_mz_sfmx表
     */
    @DataSource(DataSourceType.SPD)
    public void syncOutpatientCharge()
    {
        try
        {
            // 获取任务配置
            Map<String, Object> taskConfig = scheduledTaskService.getTaskConfigByClassAndMethod(
                "com.scminterface.framework.web.task.HengshuiTask", "syncOutpatientCharge");
            
            if (taskConfig == null)
            {
                log.warn("衡水门诊收费明细定时任务配置不存在");
                return;
            }

            // 检查任务状态
            String status = (String) taskConfig.get("status");
            if (!"0".equals(status))
            {
                log.debug("衡水门诊收费明细定时任务已停用，跳过执行");
                return;
            }

            // 检查执行次数限制
            Integer maxExecCount = (Integer) taskConfig.get("maxExecCount");
            Integer currentExecCount = (Integer) taskConfig.get("currentExecCount");
            if (maxExecCount != null && maxExecCount >= 0 && currentExecCount != null && currentExecCount >= maxExecCount)
            {
                log.info("衡水门诊收费明细定时任务已达到最大执行次数限制: {}/{}", currentExecCount, maxExecCount);
                return;
            }

            // 执行任务：同步门诊收费明细数据
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTime = sdf.format(new Date());
            log.info("========== 衡水门诊收费明细定时任务执行开始 ==========");
            log.info("当前时间: {}", currentTime);
            log.info("执行次数: {}", currentExecCount);

            Map<String, Object> syncResult = hengshuiTaskService.syncOutpatientCharge();
            
            if (syncResult != null && Boolean.TRUE.equals(syncResult.get("success")))
            {
                log.info("同步结果: {}", syncResult.get("message"));
                log.info("总计: {}, 新增: {}, 重复: {}, 成功: {}, 失败: {}", 
                    syncResult.get("totalCount"),
                    syncResult.get("newCount"),
                    syncResult.get("duplicateCount"),
                    syncResult.get("successCount"), 
                    syncResult.get("errorCount"));
            }
            else
            {
                log.error("同步失败: {}", syncResult != null ? syncResult.get("message") : "未知错误");
            }

            log.info("===================================");

            // 增加执行次数
            scheduledTaskService.incrementTaskExecCount(
                "com.scminterface.framework.web.task.HengshuiTask", "syncOutpatientCharge");
        }
        catch (Exception e)
        {
            log.error("衡水门诊收费明细定时任务执行异常", e);
        }
    }
}
