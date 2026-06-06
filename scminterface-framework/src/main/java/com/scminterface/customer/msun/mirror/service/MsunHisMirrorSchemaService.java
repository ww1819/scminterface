package com.scminterface.customer.msun.mirror.service;

import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.msun.mirror.config.MsunHisMirrorProperties;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorSchemaTables;
import com.scminterface.framework.datasource.DataSourceAvailability;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 众阳 HIS 镜像表按需建表入口：调用/探针/查询前检测，缺失则自动 CREATE + 增量补列。
 */
@Service
public class MsunHisMirrorSchemaService
{
    private static final Logger log = LoggerFactory.getLogger(MsunHisMirrorSchemaService.class);

    private final MsunHisMirrorProperties mirrorProperties;
    private final DataSourceAvailability dataSourceAvailability;
    private final MsunHisMirrorSchemaExecutor schemaExecutor;

    private final Map<String, Object> tableLocks = new ConcurrentHashMap<>();
    private final Map<String, Boolean> ensuredTables = new ConcurrentHashMap<>();

    public MsunHisMirrorSchemaService(
            MsunHisMirrorProperties mirrorProperties,
            DataSourceAvailability dataSourceAvailability,
            MsunHisMirrorSchemaExecutor schemaExecutor)
    {
        this.mirrorProperties = mirrorProperties;
        this.dataSourceAvailability = dataSourceAvailability;
        this.schemaExecutor = schemaExecutor;
    }

    public void ensureTablesForApi(String apiCode)
    {
        ensureTables(MsunHisMirrorSchemaTables.tablesForApi(apiCode));
    }

    public void ensureTablesForProbe(String probeKey)
    {
        ensureTables(MsunHisMirrorSchemaTables.tablesForProbe(probeKey));
    }

    public void ensureTable(String tableName)
    {
        ensureTables(MsunHisMirrorSchemaTables.tablesForTable(tableName));
    }

    private void ensureTables(List<String> tables)
    {
        if (!shouldBootstrap() || tables == null || tables.isEmpty())
        {
            return;
        }
        for (String table : tables)
        {
            ensureTableInternal(table);
        }
    }

    private void ensureTableInternal(String tableName)
    {
        if (tableName == null || tableName.isEmpty())
        {
            return;
        }
        if (Boolean.TRUE.equals(ensuredTables.get(tableName)))
        {
            return;
        }
        Object lock = tableLocks.computeIfAbsent(tableName, k -> new Object());
        synchronized (lock)
        {
            if (Boolean.TRUE.equals(ensuredTables.get(tableName)))
            {
                return;
            }
            try
            {
                schemaExecutor.ensureTable(tableName);
                ensuredTables.put(tableName, Boolean.TRUE);
            }
            catch (Exception ex)
            {
                if (mirrorProperties.isSchemaFailOnError())
                {
                    throw ex;
                }
                log.warn("众阳HIS镜像表自动建表/补列失败 table={} err={}", tableName, ex.getMessage());
            }
        }
    }

    private boolean shouldBootstrap()
    {
        return mirrorProperties.isEnabled()
                && mirrorProperties.isAutoSchema()
                && dataSourceAvailability.isAvailable(DataSourceType.SPD);
    }
}
