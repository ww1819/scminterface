package com.scminterface.customer.msun.hospital.zaoqiangtcm.web;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.MsunVendorConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
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
@RequestMapping(ZaoqiangTcmHospitalConstants.SPD_QUERY_API_PREFIX)
public class ZaoqiangTcmMsunSpdQueryController
{
    private final MsunSpdQueryService spdQueryService;
    private final ZaoqiangTcmMsunProperties msunProperties;

    public ZaoqiangTcmMsunSpdQueryController(
            MsunSpdQueryService spdQueryService,
            ZaoqiangTcmMsunProperties msunProperties)
    {
        this.spdQueryService = spdQueryService;
        this.msunProperties = msunProperties;
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
        return invoke(() -> spdQueryService.queryDrugDictInfos(
                msunProperties, drugCode, drugId, drugName, startTime, endTime, limitCount, materialOrDrug,
                specialFlag, invalidFlag, hospitalId, orgId));
    }

    @ApiOperation("2.5.58 SPD 药品材料分类字典查询")
    @GetMapping("/dict-category")
    public AjaxResult dictCategory(
            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false) Integer limitCount)
    {
        return invoke(() -> spdQueryService.queryDictCategory(msunProperties, keyWord, limitCount));
    }

    @ApiOperation("2.5.62 SPD 供应商查询")
    @GetMapping("/drug-suppliers")
    public AjaxResult drugSuppliers(
            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false) Integer limitCount,
            @RequestParam(required = false) String materialOrDrug,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Long orgId)
    {
        return invoke(() -> spdQueryService.queryDrugSuppliers(
                msunProperties, keyWord, limitCount, materialOrDrug, hospitalId, orgId));
    }

    @ApiOperation("2.5.63 SPD 生产厂商查询")
    @GetMapping("/drug-producers")
    public AjaxResult drugProducers(
            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false) Integer limitCount,
            @RequestParam(required = false) String materialOrDrug,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Long orgId)
    {
        return invoke(() -> spdQueryService.queryDrugProducers(
                msunProperties, keyWord, limitCount, materialOrDrug, hospitalId, orgId));
    }

    @ApiOperation("2.5.43 药房批次库存查询")
    @GetMapping("/drug-batch-stocks")
    public AjaxResult drugBatchStocks(
            @ApiParam(value = "药房科室Id", required = true) @RequestParam Long deptId,
            @ApiParam(value = "药品/材料Id", required = true) @RequestParam Long drugId,
            @ApiParam(value = "规格包装Id", required = true) @RequestParam Long drugSpecPackingId)
    {
        return invoke(() -> spdQueryService.queryDrugBatchStocks(msunProperties, deptId, drugId, drugSpecPackingId));
    }

    @ApiOperation("2.5.102 一级库入库和退库记录查询")
    @PostMapping("/yk-instock")
    public AjaxResult ykInstock(@RequestBody Map<String, Object> body)
    {
        return invoke(() -> spdQueryService.queryYkInstock(
                msunProperties,
                parseLong(body.get("deptId")),
                stringVal(body.get("startTime")),
                stringVal(body.get("endTime")),
                stringVal(body.get("instockCode")),
                stringVal(body.get("type"))));
    }

    private AjaxResult invoke(SpdQueryCall call)
    {
        try
        {
            return enrichEnv(AjaxResult.success(call.run()));
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
        return result
                .put("vendorCode", MsunVendorConstants.VENDOR_CODE)
                .put("vendorName", MsunVendorConstants.VENDOR_NAME)
                .put("hospitalKey", msunProperties.getHospitalKey())
                .put("hospitalName", msunProperties.getHospitalName())
                .put("tenantId", ZaoqiangTcmHospitalConstants.TENANT_ID)
                .put("activeEnv", msunProperties.getActiveEnv())
                .put("msunBaseUrl", msunProperties.getBaseUrl());
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
        Object run() throws Exception;
    }
}
