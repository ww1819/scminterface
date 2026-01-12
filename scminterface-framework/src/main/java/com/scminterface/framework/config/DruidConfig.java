package com.scminterface.framework.config;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.spring.SpringUtils;
import com.scminterface.framework.config.properties.DruidProperties;
import com.scminterface.framework.datasource.DynamicDataSource;

/**
 * druid 配置多数据源
 * 
 * @author scminterface
 */
@Configuration
public class DruidConfig
{
    private static final Logger log = LoggerFactory.getLogger(DruidConfig.class);

    @Bean
    @ConfigurationProperties("spring.datasource.druid.spd")
    @ConditionalOnProperty(prefix = "spring.datasource.druid.spd", name = "enabled", havingValue = "true", matchIfMissing = false)
    public DataSource spdDataSource(DruidProperties druidProperties)
    {
        try
        {
            DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
            DruidDataSource configuredDataSource = druidProperties.dataSource(dataSource);
            // 设置延迟初始化，避免启动时连接失败导致启动失败
            configuredDataSource.setInitialSize(0);
            configuredDataSource.setTestOnBorrow(false);
            configuredDataSource.setTestWhileIdle(true);
            log.info("SPD数据源配置成功");
            return configuredDataSource;
        }
        catch (Exception e)
        {
            log.warn("SPD数据源配置失败: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    @ConfigurationProperties("spring.datasource.druid.scm")
    @ConditionalOnProperty(prefix = "spring.datasource.druid.scm", name = "enabled", havingValue = "true", matchIfMissing = false)
    public DataSource scmDataSource(DruidProperties druidProperties)
    {
        try
        {
            DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
            DruidDataSource configuredDataSource = druidProperties.dataSource(dataSource);
            // 设置延迟初始化，避免启动时连接失败导致启动失败
            configuredDataSource.setInitialSize(0);
            configuredDataSource.setTestOnBorrow(false);
            configuredDataSource.setTestWhileIdle(true);
            log.info("SCM数据源配置成功");
            return configuredDataSource;
        }
        catch (Exception e)
        {
            log.warn("SCM数据源配置失败: {}", e.getMessage());
            return null;
        }
    }

    @Bean(name = "dynamicDataSource")
    @Primary
    public DynamicDataSource dataSource()
    {
        Map<Object, Object> targetDataSources = new HashMap<>();
        DataSource defaultDataSource = null;
        
        // 尝试获取SPD数据源
        DataSource spdDs = null;
        try
        {
            if (SpringUtils.containsBean("spdDataSource"))
            {
                spdDs = SpringUtils.getBean("spdDataSource");
                if (spdDs != null)
                {
                    targetDataSources.put(DataSourceType.SPD.name(), spdDs);
                    if (defaultDataSource == null)
                    {
                        defaultDataSource = spdDs;
                    }
                    log.info("SPD数据源已添加到动态数据源");
                }
            }
        }
        catch (Exception e)
        {
            log.warn("SPD数据源不可用，已跳过: {}", e.getMessage());
        }
        
        // 尝试获取SCM数据源
        DataSource scmDs = null;
        try
        {
            if (SpringUtils.containsBean("scmDataSource"))
            {
                scmDs = SpringUtils.getBean("scmDataSource");
                if (scmDs != null)
                {
                    targetDataSources.put(DataSourceType.SCM.name(), scmDs);
                    if (defaultDataSource == null)
                    {
                        defaultDataSource = scmDs;
                    }
                    log.info("SCM数据源已添加到动态数据源");
                }
            }
        }
        catch (Exception e)
        {
            log.warn("SCM数据源不可用，已跳过: {}", e.getMessage());
        }
        
        // 如果没有任何数据源可用，抛出异常
        if (targetDataSources.isEmpty())
        {
            throw new RuntimeException("至少需要配置一个可用的数据源（SPD或SCM）");
        }
        
        // 如果只有一个数据源，使用它作为默认数据源
        if (defaultDataSource == null && !targetDataSources.isEmpty())
        {
            defaultDataSource = (DataSource) targetDataSources.values().iterator().next();
        }
        
        log.info("动态数据源初始化完成，可用数据源数量: {}", targetDataSources.size());
        return new DynamicDataSource(defaultDataSource, targetDataSources);
    }
}

