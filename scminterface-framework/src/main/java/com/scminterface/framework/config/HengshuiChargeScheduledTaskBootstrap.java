package com.scminterface.framework.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.scminterface.customer.hengsuiThird.his.HisBillingTenantConstants;
import com.scminterface.framework.datasource.DataSourceAvailability;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.web.service.HospitalScheduledTaskMatcher;
import com.scminterface.framework.web.service.ScheduledTaskService;
import com.scminterface.framework.web.service.SpdHospitalContextService;

/**
 * 衡水三院：住院/门诊计费镜像定时任务默认注册（每 2 小时；抓取昨天+今天，见 {@link com.scminterface.customer.hengsuiThird.his.HisChargeMirrorFetchSql}）。
 * 仅当库中尚无对应 task_class+task_method 时插入，不覆盖现场已改过的 Cron。
 */
@Component
@Order(100)
public class HengshuiChargeScheduledTaskBootstrap implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(HengshuiChargeScheduledTaskBootstrap.class);

    /** 每 2 小时整点执行（0:00、2:00、4:00…） */
    public static final String DEFAULT_CRON_EVERY_2_HOURS = "0 0 0/2 * * ?";

    private static final String TASK_CLASS = "com.scminterface.framework.web.task.HengshuiTask";

    private final DataSourceAvailability dataSourceAvailability;
    private final ScheduledTaskService scheduledTaskService;
    private final HospitalScheduledTaskMatcher hospitalScheduledTaskMatcher;
    private final SpdHospitalContextService spdHospitalContextService;

    public HengshuiChargeScheduledTaskBootstrap(
        DataSourceAvailability dataSourceAvailability,
        ScheduledTaskService scheduledTaskService,
        HospitalScheduledTaskMatcher hospitalScheduledTaskMatcher,
        SpdHospitalContextService spdHospitalContextService)
    {
        this.dataSourceAvailability = dataSourceAvailability;
        this.scheduledTaskService = scheduledTaskService;
        this.hospitalScheduledTaskMatcher = hospitalScheduledTaskMatcher;
        this.spdHospitalContextService = spdHospitalContextService;
    }

    @Override
    public void run(ApplicationArguments args)
    {
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            log.debug("SPD 数据源未启用，跳过衡水计费镜像定时任务默认注册");
            return;
        }
        if (!hospitalScheduledTaskMatcher.isCurrentHospital(HisBillingTenantConstants.TENANT_HENGSHUI_THIRD))
        {
            log.info("当前医院为 {}，非衡水三院，跳过衡水计费镜像默认定时任务注册",
                spdHospitalContextService.describeCurrentHospital());
            return;
        }
        try
        {
            ensureChargeMirrorTask("衡水住院收费镜像同步", "syncInpatientCharge");
            ensureChargeMirrorTask("衡水门诊收费镜像同步", "syncOutpatientCharge");
        }
        catch (Exception e)
        {
            log.warn("注册衡水计费镜像默认定时任务失败: {}", e.getMessage());
        }
    }

    private void ensureChargeMirrorTask(String taskName, String taskMethod)
    {
        if (scheduledTaskService.getTaskConfigByClassAndMethod(TASK_CLASS, taskMethod) != null)
        {
            return;
        }
        scheduledTaskService.insertSpdTask(
            taskName, TASK_CLASS, taskMethod, DEFAULT_CRON_EVERY_2_HOURS, -1, "0");
        log.info("已注册默认定时任务: {}.{} cron={}", TASK_CLASS, taskMethod, DEFAULT_CRON_EVERY_2_HOURS);
    }
}
