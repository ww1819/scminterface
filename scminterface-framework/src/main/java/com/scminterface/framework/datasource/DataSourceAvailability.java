package com.scminterface.framework.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.spring.SpringUtils;

/**
 * 判断 SPD/SCM 数据源是否在配置中启用且已成功注册为 Bean。
 * <p>
 * 与 {@code spring.datasource.druid.spd.enabled} / {@code scm.enabled} 一致；
 * 停用后业务代码不应再切换至该数据源，避免回退到默认库后长时间建连超时。
 */
@Component
public class DataSourceAvailability
{
    private static final String SPD_ENABLED = "spring.datasource.druid.spd.enabled";
    private static final String SCM_ENABLED = "spring.datasource.druid.scm.enabled";
    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private DynamicDataSource dynamicDataSource;

    public boolean isEnabled(DataSourceType type)
    {
        if (type == null)
        {
            return false;
        }
        switch (type)
        {
            case SPD:
                return Boolean.TRUE.equals(environment.getProperty(SPD_ENABLED, Boolean.class, false));
            case SCM:
                return Boolean.TRUE.equals(environment.getProperty(SCM_ENABLED, Boolean.class, false));
            default:
                return false;
        }
    }

    public boolean isRegistered(DataSourceType type)
    {
        if (type == null)
        {
            return false;
        }
        if (dynamicDataSource != null)
        {
            return dynamicDataSource.isRegistered(type.name());
        }
        String beanName = type == DataSourceType.SCM ? "scmDataSource" : "spdDataSource";
        return SpringUtils.containsBean(beanName);
    }

    /** 配置启用且动态数据源中已注册（可安全切换） */
    public boolean isAvailable(DataSourceType type)
    {
        return isEnabled(type) && isRegistered(type);
    }

    public void requireAvailable(DataSourceType type)
    {
        if (!isAvailable(type))
        {
            String key = type == DataSourceType.SCM ? "scm" : "spd";
            throw new IllegalStateException(
                "数据源 " + type.name() + " 未启用（请检查 spring.datasource.druid." + key + ".enabled）");
        }
    }
}
