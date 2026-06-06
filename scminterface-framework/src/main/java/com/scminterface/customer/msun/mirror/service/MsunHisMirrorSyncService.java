package com.scminterface.customer.msun.mirror.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.config.MsunHisMirrorProperties;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorProbeRegistry;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorSyncOutcome;
import com.scminterface.customer.msun.spd.sync.config.MsunSpdMasterSyncProperties;
import com.scminterface.customer.msun.spd.sync.service.MsunSpdMasterSyncService;
import com.scminterface.customer.msun.spd.sync.support.MsunSpdMasterSyncSupport;
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
    private final MsunSpdMasterSyncProperties spdMasterSyncProperties;
    private final DataSourceAvailability dataSourceAvailability;
    private final MsunHisMirrorSchemaService schemaService;
    private final MsunHisMirrorSyncExecutor syncExecutor;
    private final MsunSpdMasterSyncService spdMasterSyncService;

    public MsunHisMirrorSyncService(
            MsunHisMirrorProperties mirrorProperties,
            MsunSpdMasterSyncProperties spdMasterSyncProperties,
            DataSourceAvailability dataSourceAvailability,
            MsunHisMirrorSchemaService schemaService,
            MsunHisMirrorSyncExecutor syncExecutor,
            MsunSpdMasterSyncService spdMasterSyncService)
    {
        this.mirrorProperties = mirrorProperties;
        this.spdMasterSyncProperties = spdMasterSyncProperties;
        this.dataSourceAvailability = dataSourceAvailability;
        this.schemaService = schemaService;
        this.syncExecutor = syncExecutor;
        this.spdMasterSyncService = spdMasterSyncService;
    }

    /**
     * HIS 查询成功后写入镜像表并尝试同步 SPD 主数据；失败时 outcome 携带原因，不影响接口 data 返回。
     */
    public MsunHisMirrorSyncOutcome syncQueryResult(
            MsunHospitalRuntime runtime,
            String apiCode,
            JSONObject wrappedResponse)
    {
        MsunHisMirrorSyncOutcome outcome = createBaseOutcome(apiCode);
        if (!mirrorProperties.isEnabled())
        {
            outcome.setMirrorSkippedReason("mirror.enabled=false，未落镜像库");
            spdMasterSyncService.fillSpdSyncOutcome(outcome, runtime, apiCode, null, 0);
            return outcome;
        }
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            outcome.setMirrorSkippedReason("spring.datasource.druid.spd.enabled=false，SPD 数据源未启用");
            spdMasterSyncService.fillSpdSyncOutcome(outcome, runtime, apiCode, null, 0);
            return outcome;
        }
        if (runtime == null || wrappedResponse == null || StringUtils.isEmpty(apiCode))
        {
            outcome.setMirrorSkippedReason("runtime 或响应为空");
            return outcome;
        }
        try
        {
            schemaService.ensureTablesForApi(apiCode);
            String batchNo = buildBatchNo();
            int rows = syncExecutor.execute(runtime, apiCode, batchNo, wrappedResponse);
            outcome.setMirrorRows(rows);
            outcome.setSyncBatchNo(batchNo);
            if (rows > 0)
            {
                log.info("众阳HIS镜像落库完成 hospital={} api={} batch={} rows={}",
                        runtime.getHospitalKey(), apiCode, batchNo, rows);
                spdMasterSyncService.fillSpdSyncOutcome(outcome, runtime, apiCode, batchNo, rows);
            }
            else
            {
                outcome.setMirrorError(diagnoseZeroMirrorRows(wrappedResponse));
                spdMasterSyncService.fillSpdSyncOutcome(outcome, runtime, apiCode, batchNo, 0);
            }
        }
        catch (Exception ex)
        {
            log.warn("众阳HIS镜像落库失败 hospital={} api={} err={}",
                    runtime.getHospitalKey(), apiCode, ex.getMessage(), ex);
            outcome.setMirrorError(ex.getMessage());
        }
        return outcome;
    }

    /**
     * 探针页手动：将指定批次（或最新批次）镜像 upsert 至 SPD 主数据表。
     */
    public MsunHisMirrorSyncOutcome syncSpdFromProbe(
            MsunHospitalRuntime runtime,
            String probeKey,
            String batchNo)
    {
        MsunHisMirrorProbeRegistry.ProbeMirrorSpec spec = MsunHisMirrorProbeRegistry.specOf(probeKey);
        if (spec == null)
        {
            throw new IllegalArgumentException("未知探针接口键: " + probeKey);
        }
        if (!MsunHisMirrorProbeRegistry.supportsSpdMasterSync(probeKey))
        {
            throw new IllegalArgumentException("探针 " + probeKey + " 不支持 SPD 主数据同步");
        }
        String apiCode = spec.getApiCode();
        MsunHisMirrorSyncOutcome outcome = createBaseOutcome(apiCode);
        outcome.setMirrorEnabled(mirrorProperties.isEnabled());
        if (!mirrorProperties.isEnabled())
        {
            outcome.setSpdSyncError("mirror.enabled=false，请先启用镜像落库");
            return outcome;
        }
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            outcome.setSpdSyncError("spring.datasource.druid.spd.enabled=false，SPD 数据源未启用");
            return outcome;
        }
        if (runtime == null)
        {
            outcome.setSpdSyncError("医院运行时配置为空");
            return outcome;
        }
        try
        {
            schemaService.ensureTablesForProbe(probeKey);
            String resolvedBatch = StringUtils.isNotEmpty(batchNo)
                    ? batchNo
                    : syncExecutor.resolveLatestBatchNo(runtime, apiCode);
            if (StringUtils.isEmpty(resolvedBatch))
            {
                outcome.setSpdSyncError("镜像库无该接口批次，请先「调用」或「获取全部分页」落库");
                return outcome;
            }
            outcome.setSyncBatchNo(resolvedBatch);
            String primaryTable = MsunHisMirrorProbeRegistry.primaryTableForProbe(probeKey);
            long mirrorRows = syncExecutor.countBatchMirrorRows(runtime, primaryTable, apiCode, resolvedBatch);
            outcome.setMirrorRows((int) Math.min(mirrorRows, Integer.MAX_VALUE));
            if (mirrorRows <= 0)
            {
                outcome.setSpdSyncError("批次 " + resolvedBatch + " 无镜像行");
                return outcome;
            }
            spdMasterSyncService.fillSpdSyncOutcome(outcome, runtime, apiCode, resolvedBatch, outcome.getMirrorRows());
            if (outcome.getSpdRows() > 0 && MsunSpdMasterSyncSupport.spdTableHint(apiCode).length() > 0)
            {
                outcome.setSpdNote("已 upsert 至 " + MsunSpdMasterSyncSupport.spdTableHint(apiCode));
            }
        }
        catch (Exception ex)
        {
            log.warn("众阳HIS探针SPD主数据同步失败 hospital={} probe={} err={}",
                    runtime.getHospitalKey(), probeKey, ex.getMessage(), ex);
            outcome.setSpdSyncError(ex.getMessage());
        }
        return outcome;
    }

    private MsunHisMirrorSyncOutcome createBaseOutcome(String apiCode)
    {
        MsunHisMirrorSyncOutcome outcome = new MsunHisMirrorSyncOutcome();
        outcome.setApiCode(apiCode);
        outcome.setMirrorEnabled(mirrorProperties.isEnabled());
        outcome.setSpdSyncEnabled(spdMasterSyncProperties.isEnabled());
        outcome.setSpdDataSourceAvailable(dataSourceAvailability.isAvailable(DataSourceType.SPD));
        return outcome;
    }

    private static String diagnoseZeroMirrorRows(JSONObject wrappedResponse)
    {
        Object hisBodyObj = wrappedResponse.get("hisBody");
        if (!(hisBodyObj instanceof JSONObject))
        {
            return "响应无 hisBody，未落镜像";
        }
        JSONObject hisBody = (JSONObject) hisBodyObj;
        if (!Boolean.TRUE.equals(hisBody.getBoolean("success")))
        {
            return "HIS success!=true，未落镜像";
        }
        JSONArray data = hisBody.getJSONArray("data");
        if (data == null || data.isEmpty())
        {
            return "HIS data 为空，未落镜像";
        }
        return "落库执行返回 0 行（请查服务端日志）";
    }

    private static String buildBatchNo()
    {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "API-" + LocalDateTime.now().format(BATCH_FMT) + "-" + suffix;
    }
}
