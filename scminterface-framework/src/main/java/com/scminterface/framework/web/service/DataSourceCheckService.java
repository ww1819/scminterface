package com.scminterface.framework.web.service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.spring.SpringUtils;

/**
 * 数据源连接检测服务
 * 
 * @author scminterface
 */
@Service
public class DataSourceCheckService
{
    private static final Logger log = LoggerFactory.getLogger(DataSourceCheckService.class);
    
    // 数据源连接检测超时时间（秒），设置较短避免登录卡顿
    private static final int CHECK_TIMEOUT_SECONDS = 2;

    /**
     * 检测所有数据源连接状态（并行检测，快速返回）
     * 
     * @return 可用数据源列表
     */
    public List<Map<String, Object>> checkDataSourceStatus()
    {
        List<Map<String, Object>> availableDataSources = new ArrayList<>();

        // 并行检测两个数据源，提高效率
        CompletableFuture<Boolean> spdFuture = CompletableFuture.supplyAsync(() -> 
            checkDataSource("spdDataSource", DataSourceType.SPD.name()));
        CompletableFuture<Boolean> scmFuture = CompletableFuture.supplyAsync(() -> 
            checkDataSource("scmDataSource", DataSourceType.SCM.name()));

        // 分别等待每个数据源的检测结果，如果超时则视为不可用
        Boolean spdAvailable = getResultWithTimeout(spdFuture, "SPD");
        Boolean scmAvailable = getResultWithTimeout(scmFuture, "SCM");

        if (Boolean.TRUE.equals(spdAvailable))
        {
            Map<String, Object> spdInfo = new HashMap<>();
            spdInfo.put("name", "SPD");
            spdInfo.put("type", DataSourceType.SPD.name());
            spdInfo.put("available", true);
            availableDataSources.add(spdInfo);
        }

        if (Boolean.TRUE.equals(scmAvailable))
        {
            Map<String, Object> scmInfo = new HashMap<>();
            scmInfo.put("name", "SCM");
            scmInfo.put("type", DataSourceType.SCM.name());
            scmInfo.put("available", true);
            availableDataSources.add(scmInfo);
        }

        return availableDataSources;
    }

    /**
     * 获取异步结果，带超时处理
     * 
     * @param future 异步任务
     * @param dataSourceName 数据源名称（用于日志）
     * @return 检测结果，超时或异常返回false
     */
    private Boolean getResultWithTimeout(CompletableFuture<Boolean> future, String dataSourceName)
    {
        try
        {
            return future.get(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            log.debug("数据源 {} 检测超时（{}秒），视为不可用", dataSourceName, CHECK_TIMEOUT_SECONDS);
            future.cancel(true);
            return false;
        }
        catch (Exception e)
        {
            log.debug("数据源 {} 检测异常: {}", dataSourceName, e.getMessage());
            future.cancel(true);
            return false;
        }
    }

    /**
     * 检测指定数据源连接状态（带超时机制）
     * 
     * @param beanName 数据源Bean名称
     * @param dataSourceType 数据源类型
     * @return 是否可用
     */
    private boolean checkDataSource(String beanName, String dataSourceType)
    {
        try
        {
            if (!SpringUtils.containsBean(beanName))
            {
                log.debug("数据源 {} 不存在", dataSourceType);
                return false;
            }

            DataSource dataSource = SpringUtils.getBean(beanName);
            if (dataSource == null)
            {
                log.debug("数据源 {} 为null", dataSourceType);
                return false;
            }

            // 使用异步方式检测连接，设置超时时间，避免长时间等待
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try (Connection connection = dataSource.getConnection())
                {
                    if (connection != null && !connection.isClosed())
                    {
                        // 执行一个简单的查询来验证连接
                        connection.isValid(1);
                        log.debug("数据源 {} 连接正常", dataSourceType);
                        return true;
                    }
                }
                catch (Exception e)
                {
                    log.debug("数据源 {} 连接检测异常: {}", dataSourceType, e.getMessage());
                }
                return false;
            });

            // 等待结果，如果超时则返回false
            try
            {
                return future.get(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            catch (TimeoutException e)
            {
                log.warn("检测数据源 {} 连接超时（{}秒），视为不可用", dataSourceType, CHECK_TIMEOUT_SECONDS);
                future.cancel(true);
                return false;
            }
        }
        catch (Exception e)
        {
            log.warn("检测数据源 {} 连接状态失败: {}", dataSourceType, e.getMessage());
        }
        return false;
    }
}
