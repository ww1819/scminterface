package com.scminterface.customer.msun.spd.sync.service;

import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorSyncOutcome;
import com.scminterface.customer.msun.spd.sync.config.MsunSpdMasterSyncProperties;
import com.scminterface.customer.msun.spd.sync.support.MsunSpdMasterSyncSupport;
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
     * 镜像批次写入成功后调用，结果写入 outcome（失败不影响接口返回）。
     */
    public void fillSpdSyncOutcome(
            MsunHisMirrorSyncOutcome outcome,
            MsunHospitalRuntime runtime,
            String apiCode,
            String batchNo,
            int mirrorRows)
    {
        if (outcome == null)
        {
            return;
        }
        outcome.setSpdSyncEnabled(properties.isEnabled());
        outcome.setSpdDataSourceAvailable(dataSourceAvailability.isAvailable(DataSourceType.SPD));
        if (!properties.isEnabled())
        {
            outcome.setSpdNote("spd-master-sync.enabled=false，未写入 SPD 主数据");
            return;
        }
        if (mirrorRows <= 0)
        {
            return;
        }
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            outcome.setSpdSyncError("spring.datasource.druid.spd.enabled=false，SPD 数据源未启用");
            return;
        }
        if (runtime == null || StringUtils.isEmpty(apiCode) || StringUtils.isEmpty(batchNo))
        {
            return;
        }
        if (!MsunSpdMasterSyncSupport.isMasterDataApi(apiCode))
        {
            return;
        }
        try
        {
            int rows = syncExecutor.execute(runtime, apiCode, batchNo);
            outcome.setSpdRows(rows);
            if (rows > 0)
            {
                log.info("众阳HIS SPD主数据同步完成 hospital={} api={} batch={} rows={}",
                        runtime.getHospitalKey(), apiCode, batchNo, rows);
            }
            else
            {
                String note = MsunSpdMasterSyncSupport.zeroSpdRowsNote(apiCode, mirrorRows);
                if (note != null)
                {
                    outcome.setSpdNote(note);
                }
            }
        }
        catch (Exception ex)
        {
            log.warn("众阳HIS SPD主数据同步失败 hospital={} api={} batch={} err={}",
                    runtime.getHospitalKey(), apiCode, batchNo, ex.getMessage(), ex);
            outcome.setSpdSyncError(ex.getMessage());
        }
    }

    /**
     * @deprecated 请使用 {@link #fillSpdSyncOutcome}，保留供未改造调用方兼容。
     */
    @Deprecated
    public void syncAfterMirror(MsunHospitalRuntime runtime, String apiCode, String batchNo, int mirrorRows)
    {
        MsunHisMirrorSyncOutcome outcome = new MsunHisMirrorSyncOutcome();
        outcome.setApiCode(apiCode);
        outcome.setSyncBatchNo(batchNo);
        outcome.setMirrorRows(mirrorRows);
        fillSpdSyncOutcome(outcome, runtime, apiCode, batchNo, mirrorRows);
    }
}
