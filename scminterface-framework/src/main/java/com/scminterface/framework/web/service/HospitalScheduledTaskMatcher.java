package com.scminterface.framework.web.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.scminterface.customer.hengsuiThird.his.HisBillingTenantConstants;

/**
 * 按 SPD {@code sys_config} 中的租户 ID 判断定时任务是否属于当前医院，避免张冠李戴。
 * <p>
 * 未出现在映射表中的任务类视为通用任务，所有医院均可注册。
 */
@Component
public class HospitalScheduledTaskMatcher
{
    private static final Logger log = LoggerFactory.getLogger(HospitalScheduledTaskMatcher.class);

    private static final Map<String, String> TASK_CLASS_TO_CUSTOMER_ID;

    static
    {
        Map<String, String> map = new HashMap<>();
        map.put("com.scminterface.framework.web.task.HengshuiTask", HisBillingTenantConstants.TENANT_HENGSHUI_THIRD);
        // 预留：枣强等其它医院专属 Task 类在此登记
        // map.put("com.scminterface.framework.web.task.ZaoqiangTcmTask", ZaoqiangTcmTenantConstants.TENANT_ID);
        TASK_CLASS_TO_CUSTOMER_ID = Collections.unmodifiableMap(map);
    }

    @Autowired
    private SpdHospitalContextService spdHospitalContextService;

    /**
     * 当前部署是否匹配指定租户 ID。
     */
    public boolean isCurrentHospital(String expectedCustomerId)
    {
        if (expectedCustomerId == null || expectedCustomerId.isEmpty())
        {
            return false;
        }
        String currentCustomerId = safeGetDefaultCustomerId();
        return expectedCustomerId.equals(currentCustomerId);
    }

    /**
     * 任务类是否允许在当前医院注册/执行。
     */
    public boolean matches(String taskClass)
    {
        if (taskClass == null || taskClass.isEmpty())
        {
            return false;
        }

        String requiredCustomerId = TASK_CLASS_TO_CUSTOMER_ID.get(taskClass);
        if (requiredCustomerId == null)
        {
            return true;
        }

        String currentCustomerId = safeGetDefaultCustomerId();
        if (currentCustomerId == null)
        {
            log.warn("sys_config 未配置 {}，跳过医院专属任务 {}",
                SpdSysConfigKeys.DEFAULT_CUSTOMER_ID, taskClass);
            return false;
        }

        boolean matched = requiredCustomerId.equals(currentCustomerId);
        if (!matched)
        {
            log.info("任务 {} 属于租户 {}，当前医院为 {}，跳过注册",
                taskClass, requiredCustomerId, spdHospitalContextService.describeCurrentHospital());
        }
        return matched;
    }

    private String safeGetDefaultCustomerId()
    {
        try
        {
            return spdHospitalContextService.getDefaultCustomerId();
        }
        catch (Exception e)
        {
            log.warn("读取 sys_config 租户 ID 失败: {}", e.getMessage());
            return null;
        }
    }
}
