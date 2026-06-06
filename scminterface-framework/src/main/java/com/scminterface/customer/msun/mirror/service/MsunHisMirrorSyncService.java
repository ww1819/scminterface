package com.scminterface.customer.msun.mirror.service;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.config.MsunHisMirrorProperties;
import com.scminterface.customer.msun.spd.sync.service.MsunSpdMasterSyncService;
import com.scminterface.framework.datasource.DataSourceAvailability;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 众阳 HIS 查询接口回参落镜像库（探针页与正式 API 调用共用）。
 */
@Service
public class MsunHisMirrorSyncService
{
    private static final Logger log = LoggerFactory.getLogger(MsunHisMirrorSyncService.class);
    private static final DateTimeFormatter BATCH_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MsunHisMirrorProperties mirrorProperties;
    private final DataSourceAvailability dataSourceAvailability;
    private final MsunHisMirrorSchemaService schemaService;
    private final MsunHisMirrorSyncExecutor syncExecutor;
    private final MsunSpdMasterSyncService spdMasterSyncService;

    public MsunHisMirrorSyncService(
            MsunHisMirrorProperties mirrorProperties,
            DataSourceAvailability dataSourceAvailability,
            MsunHisMirrorSchemaService schemaService,
            MsunHisMirrorSyncExecutor syncExecutor,
            MsunSpdMasterSyncService spdMasterSyncService)
    {
        this.mirrorProperties = mirrorProperties;
        this.dataSourceAvailability = dataSourceAvailability;
        this.schemaService = schemaService;
        this.syncExecutor = syncExecutor;
        this.spdMasterSyncService = spdMasterSyncService;
    }

    /**
     * HIS 查询成功后写入镜像表；失败或未启用数据源时静默跳过，不影响接口返回。
     */
    public void syncQueryResult(MsunHospitalRuntime runtime, String apiCode, JSONObject wrappedResponse)
    {
        if (!mirrorProperties.isEnabled())
        {
            return;
        }
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            log.debug("SPD 数据源未启用，跳过众阳镜像表落库 api={}", apiCode);
            return;
        }
        if (runtime == null || wrappedResponse == null || StringUtils.isEmpty(apiCode))
        {
            return;
        }
        try
        {
            schemaService.ensureTablesForApi(apiCode);
            String batchNo = buildBatchNo();
            int rows = syncExecutor.execute(runtime, apiCode, batchNo, wrappedResponse);
            if (rows > 0)
            {
                log.info("众阳HIS镜像落库完成 hospital={} api={} batch={} rows={}",
                        runtime.getHospitalKey(), apiCode, batchNo, rows);
                spdMasterSyncService.syncAfterMirror(runtime, apiCode, batchNo, rows);
            }
        }
        catch (Exception ex)
        {
            log.warn("众阳HIS镜像落库失败 hospital={} api={} err={}",
                    runtime.getHospitalKey(), apiCode, ex.getMessage(), ex);
        }
    }

    private static String buildBatchNo()
    {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "API-" + LocalDateTime.now().format(BATCH_FMT) + "-" + suffix;
    }
}
