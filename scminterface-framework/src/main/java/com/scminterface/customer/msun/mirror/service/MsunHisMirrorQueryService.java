package com.scminterface.customer.msun.mirror.service;

import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.config.MsunHisMirrorProperties;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorProbeRegistry;
import com.scminterface.framework.datasource.DataSourceAvailability;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 众阳 HIS 镜像表探针查询入口。
 */
@Service
public class MsunHisMirrorQueryService
{
    private static final Logger log = LoggerFactory.getLogger(MsunHisMirrorQueryService.class);

    private final MsunHisMirrorProperties mirrorProperties;
    private final DataSourceAvailability dataSourceAvailability;
    private final MsunHisMirrorSchemaService schemaService;
    private final MsunHisMirrorQueryExecutor queryExecutor;

    public MsunHisMirrorQueryService(
            MsunHisMirrorProperties mirrorProperties,
            DataSourceAvailability dataSourceAvailability,
            MsunHisMirrorSchemaService schemaService,
            MsunHisMirrorQueryExecutor queryExecutor)
    {
        this.mirrorProperties = mirrorProperties;
        this.dataSourceAvailability = dataSourceAvailability;
        this.schemaService = schemaService;
        this.queryExecutor = queryExecutor;
    }

    public Map<String, Object> queryProbeMirror(
            MsunHospitalRuntime runtime,
            String probeKey,
            int limit,
            int offset)
    {
        if (!mirrorProperties.isEnabled())
        {
            throw new IllegalStateException("镜像落库未启用（scminterface.vendor.msun.mirror.enabled=false）");
        }
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            throw new IllegalStateException("SPD 数据源未启用，无法查询镜像表");
        }
        if (!MsunHisMirrorProbeRegistry.isQueryable(probeKey))
        {
            throw new IllegalArgumentException("未知探针接口键: " + probeKey);
        }
        try
        {
            schemaService.ensureTablesForProbe(probeKey);
            return queryExecutor.queryByProbeKey(runtime, probeKey, limit, offset);
        }
        catch (Exception ex)
        {
            log.warn("众阳HIS镜像查询失败 hospital={} probeKey={} err={}",
                    runtime.getHospitalKey(), probeKey, ex.getMessage());
            throw ex;
        }
    }

    public Map<String, Object> queryEntryHis(
            MsunHospitalRuntime runtime,
            String pharmacyStockId,
            String deptId,
            String drugId,
            String drugSpecPackingId,
            String batchNumber)
    {
        if (!mirrorProperties.isEnabled())
        {
            throw new IllegalStateException("镜像落库未启用");
        }
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            throw new IllegalStateException("SPD 数据源未启用");
        }
        schemaService.ensureTable("m_drug_batch_stock");
        return queryExecutor.queryEntryHis(runtime, pharmacyStockId, deptId, drugId, drugSpecPackingId, batchNumber);
    }

    public Map<String, Object> queryBillHis(MsunHospitalRuntime runtime, String billId, String billType)
    {
        if (!mirrorProperties.isEnabled())
        {
            throw new IllegalStateException("镜像落库未启用");
        }
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            throw new IllegalStateException("SPD 数据源未启用");
        }
        schemaService.ensureTable("m_his_push_log");
        return queryExecutor.queryBillHis(runtime, billId, billType);
    }
}
