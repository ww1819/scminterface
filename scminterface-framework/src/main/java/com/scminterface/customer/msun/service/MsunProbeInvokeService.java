package com.scminterface.customer.msun.service;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.msun.mirror.service.MsunHisMirrorSyncService;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorSyncOutcome;
import com.scminterface.customer.msun.spd.service.MsunSpdStockCascadeService;
import com.scminterface.customer.msun.support.MsunProbeParamSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 联调探针统一入口：前端只传字符串参数，服务端组包并调用众阳 HIS。
 */
@Service
public class MsunProbeInvokeService
{
    private final MsunProbeService probeService;
    private final MsunSpdQueryService spdQueryService;
    private final MsunSpdStockCascadeService stockCascadeService;
    private final MsunHisMirrorSyncService mirrorSyncService;

    public MsunProbeInvokeService(
            MsunProbeService probeService,
            MsunSpdQueryService spdQueryService,
            MsunSpdStockCascadeService stockCascadeService,
            MsunHisMirrorSyncService mirrorSyncService)
    {
        this.probeService = probeService;
        this.spdQueryService = spdQueryService;
        this.stockCascadeService = stockCascadeService;
        this.mirrorSyncService = mirrorSyncService;
    }

    public Map<String, Object> invoke(
            ZaoqiangTcmMsunProperties properties,
            String apiKey,
            Map<String, String> formParams,
            String paramsJsonOverride) throws Exception
    {
        if (StringUtils.isEmpty(apiKey))
        {
            throw new IllegalArgumentException("apiKey 不能为空");
        }
        Map<String, String> params = MsunProbeParamSupport.mergeParams(formParams, paramsJsonOverride);
        MsunHospitalRuntime runtime = properties;
        JSONObject data;
        String apiCode;
        switch (apiKey)
        {
            case "depts":
                apiCode = "2.1.9";
                data = probeService.fetchDepts(
                        runtime,
                        MsunProbeParamSupport.parseLongParam(params, "hospitalAreaId"),
                        MsunProbeParamSupport.parseIntegerParam(params, "invalidFlag"),
                        MsunProbeParamSupport.parseLongParam(params, "deptId"),
                        MsunProbeParamSupport.stringParam(params, "deptName"));
                break;
            case "identities":
                apiCode = "2.1.12";
                data = probeService.fetchIdentities(
                        runtime,
                        MsunProbeParamSupport.stringParam(params, "roleType"),
                        MsunProbeParamSupport.parseLongParam(params, "deptId"),
                        MsunProbeParamSupport.parseLongParam(params, "identityId"),
                        MsunProbeParamSupport.parseLongParam(params, "userId"));
                break;
            case "drugDict":
                apiCode = "2.5.44";
                data = spdQueryService.queryDrugDictInfos(
                        runtime,
                        MsunProbeParamSupport.stringParam(params, "drugCode"),
                        MsunProbeParamSupport.parseLongParam(params, "drugId"),
                        MsunProbeParamSupport.stringParam(params, "drugName"),
                        MsunProbeParamSupport.stringParam(params, "startTime"),
                        MsunProbeParamSupport.stringParam(params, "endTime"),
                        MsunProbeParamSupport.parseIntegerParam(params, "limitCount"),
                        MsunProbeParamSupport.parseIntegerParam(params, "materialOrDrug"),
                        MsunProbeParamSupport.stringParam(params, "specialFlag"),
                        MsunProbeParamSupport.stringParam(params, "invalidFlag"),
                        MsunProbeParamSupport.parseLongParam(params, "hospitalId"),
                        MsunProbeParamSupport.parseLongParam(params, "orgId"));
                break;
            case "dictCategory":
                apiCode = "2.5.58";
                data = spdQueryService.queryDictCategory(
                        runtime,
                        MsunProbeParamSupport.stringParam(params, "keyWord"),
                        MsunProbeParamSupport.parseIntegerParam(params, "limitCount"),
                        MsunProbeParamSupport.parseLongParam(params, "hisDictId"));
                break;
            case "suppliers":
                apiCode = "2.5.62";
                data = spdQueryService.queryDrugSuppliers(
                        runtime,
                        MsunProbeParamSupport.stringParam(params, "keyWord"),
                        MsunProbeParamSupport.parseIntegerParam(params, "limitCount"),
                        MsunProbeParamSupport.stringParam(params, "materialOrDrug"),
                        MsunProbeParamSupport.parseLongParam(params, "hospitalId"),
                        MsunProbeParamSupport.parseLongParam(params, "orgId"),
                        MsunProbeParamSupport.parseLongParam(params, "supplierId"));
                break;
            case "producers":
                apiCode = "2.5.63";
                data = spdQueryService.queryDrugProducers(
                        runtime,
                        MsunProbeParamSupport.stringParam(params, "keyWord"),
                        MsunProbeParamSupport.parseIntegerParam(params, "limitCount"),
                        MsunProbeParamSupport.stringParam(params, "materialOrDrug"),
                        MsunProbeParamSupport.parseLongParam(params, "hospitalId"),
                        MsunProbeParamSupport.parseLongParam(params, "orgId"),
                        MsunProbeParamSupport.parseLongParam(params, "producerId"));
                break;
            case "mergeStocks":
                apiCode = "2.5.82";
                data = spdQueryService.queryMergeStockInfos(
                        runtime,
                        MsunProbeParamSupport.parseLongParam(params, "deptId"),
                        MsunProbeParamSupport.stringParam(params, "categoryIdList"),
                        MsunProbeParamSupport.stringParam(params, "drugCode"),
                        MsunProbeParamSupport.parseLongParam(params, "drugId"),
                        MsunProbeParamSupport.stringParam(params, "drugName"),
                        MsunProbeParamSupport.parseLongParam(params, "drugSpecPackingId"),
                        MsunProbeParamSupport.stringParam(params, "zeroFlag"),
                        MsunProbeParamSupport.parseLongParam(params, "maxId"));
                if (MsunProbeParamSupport.parseBooleanParam(params, "cascadeBatch", true))
                {
                    int cascadeMax = MsunProbeParamSupport.parseIntegerParam(params, "cascadeMax") != null
                            ? MsunProbeParamSupport.parseIntegerParam(params, "cascadeMax") : 500;
                    data.put("cascadeBatch", stockCascadeService.cascadeBatchStocks(runtime, data, cascadeMax));
                }
                break;
            case "batchStocks":
                apiCode = "2.5.43";
                data = spdQueryService.queryDrugBatchStocks(
                        runtime,
                        MsunProbeParamSupport.parseLongParam(params, "deptId"),
                        MsunProbeParamSupport.parseLongParam(params, "drugId"),
                        MsunProbeParamSupport.parseLongParam(params, "drugSpecPackingId"));
                break;
            case "ykInstock":
                apiCode = "2.5.102";
                data = spdQueryService.queryYkInstock(
                        runtime,
                        MsunProbeParamSupport.parseLongParam(params, "deptId"),
                        MsunProbeParamSupport.stringParam(params, "startTime"),
                        MsunProbeParamSupport.stringParam(params, "endTime"),
                        MsunProbeParamSupport.stringParam(params, "instockCode"),
                        MsunProbeParamSupport.stringParam(params, "type"));
                break;
            default:
                throw new IllegalArgumentException("不支持的探针 apiKey: " + apiKey);
        }
        MsunHisMirrorSyncOutcome syncOutcome = mirrorSyncService.syncQueryResult(properties, apiCode, data);
        Map<String, Object> payload = new LinkedHashMap<>(8);
        payload.put("data", data);
        payload.put("syncOutcome", syncOutcome);
        Map<String, Object> probeMeta = new LinkedHashMap<>(6);
        probeMeta.put("apiKey", apiKey);
        probeMeta.put("apiCode", apiCode);
        probeMeta.put("resolvedParams", params);
        probeMeta.put("note", "参数由服务端组包调用众阳 HIS，雪花 ID 以字符串从前端传入");
        payload.put("probeInvoke", probeMeta);
        return payload;
    }
}
