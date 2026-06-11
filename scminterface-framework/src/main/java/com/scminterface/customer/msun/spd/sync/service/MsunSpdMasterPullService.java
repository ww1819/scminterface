package com.scminterface.customer.msun.spd.sync.service;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.service.MsunHisMirrorSyncService;
import com.scminterface.customer.msun.service.MsunProbeService;
import com.scminterface.customer.msun.service.MsunSpdQueryService;
import com.scminterface.customer.msun.spd.sync.support.MsunHisPaginationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 众阳 HIS 主数据一键拉取（仅材料 materialOrDrug=1，落镜像并 upsert SPD 主数据）。
 */
@Service
public class MsunSpdMasterPullService
{
    private static final Logger log = LoggerFactory.getLogger(MsunSpdMasterPullService.class);

    /** 众阳接口：1=材料（不同步药品 0） */
    private static final String MATERIAL_ONLY = "1";
    private static final Integer MATERIAL_ONLY_INT = 1;

    private final MsunProbeService probeService;
    private final MsunSpdQueryService spdQueryService;
    private final MsunHisMirrorSyncService mirrorSyncService;

    public MsunSpdMasterPullService(
            MsunProbeService probeService,
            MsunSpdQueryService spdQueryService,
            MsunHisMirrorSyncService mirrorSyncService)
    {
        this.probeService = probeService;
        this.spdQueryService = spdQueryService;
        this.mirrorSyncService = mirrorSyncService;
    }

    public JSONObject pullDepts(MsunHospitalRuntime runtime) throws Exception
    {
        JSONObject data = probeService.fetchDepts(runtime, null, -1, null, null);
        return finish(runtime, "2.1.9", "科室", data);
    }

    public JSONObject pullIdentities(MsunHospitalRuntime runtime) throws Exception
    {
        JSONObject data = probeService.fetchIdentitiesAllRoleTypes(runtime);
        return finish(runtime, "2.1.12", "人员", data);
    }

    public JSONObject pullSuppliers(MsunHospitalRuntime runtime) throws Exception
    {
        JSONObject data = MsunHisPaginationSupport.pullAllPages(
                cursor -> spdQueryService.queryDrugSuppliers(
                        runtime, null, 100, MATERIAL_ONLY, null, null, cursor),
                "supplierId");
        return finish(runtime, "2.5.62", "供应商(材料)", data);
    }

    public JSONObject pullProducers(MsunHospitalRuntime runtime) throws Exception
    {
        JSONObject data = MsunHisPaginationSupport.pullAllPages(
                cursor -> spdQueryService.queryDrugProducers(
                        runtime, null, 100, MATERIAL_ONLY, null, null, cursor),
                "producerId");
        return finish(runtime, "2.5.63", "生产厂家(材料)", data);
    }

    public JSONObject pullCategories(MsunHospitalRuntime runtime) throws Exception
    {
        JSONObject data = MsunHisPaginationSupport.pullAllPages(
                cursor -> spdQueryService.queryDictCategory(runtime, null, 100, cursor),
                "hisDictId");
        return finish(runtime, "2.5.58", "库房分类", data);
    }

    public JSONObject pullMaterials(MsunHospitalRuntime runtime) throws Exception
    {
        JSONObject data = MsunHisPaginationSupport.pullAllPages(
                cursor -> spdQueryService.queryDrugDictInfos(
                        runtime, null, cursor, null, null, null, 100,
                        MATERIAL_ONLY_INT, null, null, null, null),
                "drugId");
        return finish(runtime, "2.5.44", "耗材档案(材料)", data);
    }

    public JSONObject pullByType(MsunHospitalRuntime runtime, String syncType) throws Exception
    {
        switch (syncType)
        {
            case "depts":
                return pullDepts(runtime);
            case "identities":
                return pullIdentities(runtime);
            case "suppliers":
                return pullSuppliers(runtime);
            case "producers":
                return pullProducers(runtime);
            case "categories":
                return pullCategories(runtime);
            case "materials":
                return pullMaterials(runtime);
            default:
                throw new IllegalArgumentException("不支持的同步类型: " + syncType);
        }
    }

    private JSONObject finish(MsunHospitalRuntime runtime, String apiCode, String label, JSONObject data)
            throws Exception
    {
        if (!MsunHisPaginationSupport.isHisOk(data))
        {
            throw new IllegalStateException(label + " HIS 调用失败: " + MsunHisPaginationSupport.hisMessage(data));
        }
        mirrorSyncService.syncQueryResult(runtime, apiCode, data);
        int rows = MsunHisPaginationSupport.extractData(data) != null
                ? MsunHisPaginationSupport.extractData(data).size() : 0;
        log.info("众阳主数据拉取完成 hospital={} type={} rows={}", runtime.getHospitalKey(), label, rows);
        JSONObject result = new JSONObject();
        result.put("label", label);
        result.put("apiCode", apiCode);
        result.put("rows", rows);
        result.put("hisBody", data.getJSONObject("hisBody"));
        return result;
    }
}
