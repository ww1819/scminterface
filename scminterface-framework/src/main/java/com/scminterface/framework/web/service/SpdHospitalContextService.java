package com.scminterface.framework.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.web.mapper.SpdSysConfigMapper;

/**
 * 从 SPD {@code sys_config} 读取当前部署医院上下文。
 */
@Service
public class SpdHospitalContextService
{
    private static final Logger log = LoggerFactory.getLogger(SpdHospitalContextService.class);

    @Autowired
    private SpdSysConfigMapper spdSysConfigMapper;

    @DataSource(DataSourceType.SPD)
    public String getHospitalName()
    {
        return trimToNull(spdSysConfigMapper.selectValueByKey(SpdSysConfigKeys.HOSPITAL_NAME));
    }

    @DataSource(DataSourceType.SPD)
    public String getDefaultCustomerId()
    {
        return trimToNull(spdSysConfigMapper.selectValueByKey(SpdSysConfigKeys.DEFAULT_CUSTOMER_ID));
    }

    /**
     * 日志用：医院名 + 租户 ID。
     */
    public String describeCurrentHospital()
    {
        try
        {
            String name = getHospitalName();
            String customerId = getDefaultCustomerId();
            if (name == null && customerId == null)
            {
                return "未知医院";
            }
            if (name != null && customerId != null)
            {
                return name + "(" + customerId + ")";
            }
            return name != null ? name : customerId;
        }
        catch (Exception e)
        {
            log.debug("读取医院上下文失败: {}", e.getMessage());
            return "未知医院";
        }
    }

    private static String trimToNull(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
