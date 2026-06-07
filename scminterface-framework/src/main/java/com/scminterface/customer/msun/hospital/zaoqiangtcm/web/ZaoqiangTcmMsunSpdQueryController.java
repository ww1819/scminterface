package com.scminterface.customer.msun.hospital.zaoqiangtcm.web;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.mirror.service.MsunHisMirrorSyncService;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorSyncOutcome;
import com.scminterface.customer.msun.MsunVendorConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.msun.spd.service.MsunSpdStockCascadeService;
import com.scminterface.customer.msun.service.MsunSpdQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 众阳 HIS — 枣强县中医院 SPD 查询探针入口（独立 URL 前缀，共用厂家 Service）。
 */
@Api(tags = "众阳HIS-枣强县中医院-SPD查询探针")
@RestController
@ConditionalOnProperty(prefix = ZaoqiangTcmHospitalConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@RequestMapping({
        ZaoqiangTcmHospitalConstants.SPD_QUERY_API_PREFIX,
        ZaoqiangTcmHospitalConstants.SPD_API_PREFIX + "/query"
})
public class ZaoqiangTcmMsunSpdQueryController
{
    private final MsunSpdQueryService spdQueryService;
    private final ZaoqiangTcmMsunProperties msunProperties;
    private final MsunHisMirrorSyncService mirrorSyncService;
    private final MsunSpdStockCascadeService stockCascadeService;

    public ZaoqiangTcmMsunSpdQueryController(
            MsunSpdQueryService spdQueryService,
            ZaoqiangTcmMsunProperties msunProperties,
            MsunHisMirrorSyncService mirrorSyncService,
            MsunSpdStockCascadeService stockCascadeService)
    {
        this.spdQueryService = spdQueryService;
        this.msunProperties = msunProperties;
        this.mirrorSyncService = mirrorSyncService;
        this.stockCascadeService = stockCascadeService;
    }

    @ApiOperation("2.5.44 药品、材料字典查询")
    @GetMapping("/drug-dict-infos")
    public AjaxResult drugDictInfos(
            @RequestParam(required = false) String drugCode,
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) String drugName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer limitCount,
            @RequestParam(required = false) Integer materialOrDrug,
            @RequestParam(required = false) String specialFlag,
            @RequestParam(required = false) String invalidFlag,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Long orgId)
    {
        return invoke("2.5.44", () -> spdQueryService.queryDrugDictInfos(
                msunProperties, drugCode, drugId, drugName, startTime, endTime, limitCount, materialOrDrug,
                specialFlag, invalidFlag, hospitalId, orgId));
    }

    @ApiOperation("2.5.58 SPD 药品材料分类字典查询")
    @GetMapping("/dict-category")
    public AjaxResult dictCategory(
            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false) Integer limitCount,
            @ApiParam("翻页游标：本页最大 hisDictId") @RequestParam(required = false) Long hisDictId)
    {
        return invoke("2.5.58", () -> spdQueryService.queryDictCategory(msunProperties, keyWord, limitCount, hisDictId));
    }

    @ApiOperation("2.5.62 SPD 供应商查询")
    @GetMapping("/drug-suppliers")
    public AjaxResult drugSuppliers(
            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false) Integer limitCount,
            @RequestParam(required = false) String materialOrDrug,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Long orgId,
            @ApiParam("翻页游标：本页最大 supplierId") @RequestParam(required = false) Long supplierId)
    {
        return invoke("2.5.62", () -> spdQueryService.queryDrugSuppliers(
                msunProperties, keyWord, limitCount, materialOrDrug, hospitalId, orgId, supplierId));
    }

    @ApiOperation("2.5.63 SPD 生产厂商查询")
    @GetMapping("/drug-producers")
    public AjaxResult drugProducers(
            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false) Integer limitCount,
            @RequestParam(required = false) String materialOrDrug,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Long orgId,
            @ApiParam("翻页游标：本页最大 producerId") @RequestParam(required = false) Long producerId)
    {
        return invoke("2.5.63", () -> spdQueryService.queryDrugProducers(
                msunProperties, keyWord, limitCount, materialOrDrug, hospitalId, orgId, producerId));
    }

    @ApiOperation("2.5.82 SPD 合并库存查询（落库后可链式调用 2.5.43）")
    @GetMapping("/merge-stock-infos")
    public AjaxResult mergeStockInfos(
            @ApiParam(value = "库存科室Id", required = true) @RequestParam Long deptId,
            @ApiParam("药材分类Id，逗号分隔") @RequestParam(required = false) String categoryIdList,
            @RequestParam(required = false) String drugCode,
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) String drugName,
            @RequestParam(required = false) Long drugSpecPackingId,
            @ApiParam("0否1是2只查零库存") @RequestParam(required = false) String zeroFlag,
            @ApiParam("翻页游标：本页最大 ycStockQueryId") @RequestParam(required = false) Long maxId,
            @ApiParam("落库后是否自动链式查询批次库存") @RequestParam(defaultValue = "true") boolean cascadeBatch,
            @ApiParam("链式批次查询最大条数（去重后）") @RequestParam(defaultValue = "500") int cascadeMax)
    {
        try
        {
            JSONObject data = spdQueryService.queryMergeStockInfos(
                    msunProperties, deptId, categoryIdList, drugCode, drugId, drugName, drugSpecPackingId, zeroFlag, maxId);
            MsunHisMirrorSyncOutcome syncOutcome = mirrorSyncService.syncQueryResult(msunProperties, "2.5.82", data);
            if (cascadeBatch)
            {
                data.put("cascadeBatch", stockCascadeService.cascadeBatchStocks(msunProperties, data, cascadeMax));
            }
            return enrichEnv(AjaxResult.success(data), syncOutcome);
        }
        catch (IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("SPD 查询接口调用失败: " + ex.getMessage());
        }
    }

    @ApiOperation("2.5.43 药房批次库存查询")
    @GetMapping("/drug-batch-stocks")
    public AjaxResult drugBatchStocks(
            @ApiParam(value = "药房科室Id", required = true) @RequestParam Long deptId,
            @ApiParam(value = "药品/材料Id", required = true) @RequestParam Long drugId,
            @ApiParam(value = "规格包装Id", required = true) @RequestParam Long drugSpecPackingId)
    {
        return invoke("2.5.43", () -> spdQueryService.queryDrugBatchStocks(msunProperties, deptId, drugId, drugSpecPackingId));
    }

    @ApiOperation("2.5.102 一级库入库和退库记录查询")
    @PostMapping("/yk-instock")
    public AjaxResult ykInstock(@RequestBody Map<String, Object> body)
    {
        return invoke("2.5.102", () -> spdQueryService.queryYkInstock(
                msunProperties,
                parseLong(body.get("deptId")),
                stringVal(body.get("startTime")),
                stringVal(body.get("endTime")),
                stringVal(body.get("instockCode")),
                stringVal(body.get("type"))));
    }

    private AjaxResult invoke(String apiCode, SpdQueryCall call)
    {
        try
        {
            JSONObject data = call.run();
            MsunHisMirrorSyncOutcome syncOutcome = mirrorSyncService.syncQueryResult(msunProperties, apiCode, data);
            return enrichEnv(AjaxResult.success(data), syncOutcome);
        }
        catch (IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("SPD 查询接口调用失败: " + ex.getMessage());
        }
    }

    private AjaxResult enrichEnv(AjaxResult result)
    {
        return enrichEnv(result, null);
    }

    private AjaxResult enrichEnv(AjaxResult result, MsunHisMirrorSyncOutcome syncOutcome)
    {
        AjaxResult enriched = result
                .put("vendorCode", MsunVendorConstants.VENDOR_CODE)
                .put("vendorName", MsunVendorConstants.VENDOR_NAME)
                .put("hospitalKey", msunProperties.getHospitalKey())
                .put("hospitalName", msunProperties.getHospitalName())
                .put("tenantId", ZaoqiangTcmHospitalConstants.TENANT_ID)
                .put("activeEnv", msunProperties.getActiveEnv())
                .put("msunBaseUrl", msunProperties.getBaseUrl());
        if (syncOutcome != null)
        {
            enriched.put("mirrorSync", syncOutcome.toMap());
        }
        return enriched;
    }

    private static Long parseLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : Long.valueOf(text);
    }

    private static String stringVal(Object value)
    {
        return value == null ? null : value.toString();
    }

    @FunctionalInterface
    private interface SpdQueryCall
    {
        JSONObject run() throws Exception;
    }
}
