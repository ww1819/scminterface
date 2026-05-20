package com.scminterface.framework.datasource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源
 * 
 * @author scminterface
 */
public class DynamicDataSource extends AbstractRoutingDataSource
{
    private final Set<String> registeredKeys;

    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources)
    {
        Set<String> keys = new HashSet<>();
        if (targetDataSources != null)
        {
            for (Object key : targetDataSources.keySet())
            {
                keys.add(String.valueOf(key));
            }
        }
        this.registeredKeys = Collections.unmodifiableSet(keys);
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    public boolean isRegistered(String dataSourceType)
    {
        return dataSourceType != null && registeredKeys.contains(dataSourceType);
    }

    @Override
    protected Object determineCurrentLookupKey()
    {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }

    /**
     * 显式指定了数据源类型但该类型未启用时，不再回退到默认数据源（避免误连或长时间等待）。
     */
    @Override
    protected DataSource determineTargetDataSource()
    {
        String lookupKey = DynamicDataSourceContextHolder.getDataSourceType();
        if (lookupKey != null)
        {
            DataSource target = getResolvedDataSources().get(lookupKey);
            if (target == null)
            {
                throw new CannotGetJdbcConnectionException(
                    "数据源 [" + lookupKey + "] 未启用或未注册，已拒绝连接（请检查 spring.datasource.druid."
                        + lookupKey.toLowerCase() + ".enabled）");
            }
            return target;
        }
        return super.determineTargetDataSource();
    }
}

