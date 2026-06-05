package com.scminterface.customer.msun.spd.sync.service;

import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.spd.sync.config.MsunSpdMasterSyncProperties;
import com.scminterface.framework.datasource.DataSourceAvailability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 众阳镜像落库后，将本批次数据 upsert 至 SPD 主数据表。
 */
@Service
public class MsunSpdMasterSyncService
{
    private static final Logger log = LoggerFactory.getLogger(MsunSpdMasterSyncService.class);

    private final MsunSpdMasterSyncProperties properties;
    private final DataSourceAvailability dataSourceAvailability;
    private final MsunSpdMasterSyncExecutor syncExecutor;

    public MsunSpdMasterSyncService(
            MsunSpdMasterSyncProperties properties,
            DataSourceAvailability dataSourceAvailability,
            MsunSpdMasterSyncExecutor syncExecutor)
    {
        this.properties = properties;
        this.dataSourceAvailability = dataSourceAvailability;
        this.syncExecutor = syncExecutor;
    }

    /**
     * 镜像批次写入成功后调用；失败静默，不影响接口返回。
     */
    public void syncAfterMirror(MsunHospitalRuntime runtime, String apiCode, String batchNo, int mirrorRows)
    {
        if (!properties.isEnabled() || mirrorRows <= 0)
        {
            return;
        }
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            log.debug("SPD 数据源未启用，跳过主数据同步 api={}", apiCode);
            return;
        }
        if (runtime == null || StringUtils.isEmpty(apiCode) || StringUtils.isEmpty(batchNo))
        {
            return;
        }
        try
        {
            int rows = syncExecutor.execute(runtime, apiCode, batchNo);
            if (rows > 0)
            {
                log.info("众阳HIS SPD主数据同步完成 hospital={} api={} batch={} rows={}",
                        runtime.getHospitalKey(), apiCode, batchNo, rows);
            }
        }
        catch (Exception ex)
        {
            log.warn("众阳HIS SPD主数据同步失败 hospital={} api={} batch={} err={}",
                    runtime.getHospitalKey(), apiCode, batchNo, ex.getMessage(), ex);
        }
    }
}
