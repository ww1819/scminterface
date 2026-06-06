package com.scminterface.customer.msun.hospital.zaoqiangtcm.web;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.MsunVendorConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.msun.mirror.service.MsunHisMirrorQueryService;
import com.scminterface.customer.msun.mirror.service.MsunHisMirrorSyncService;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorSyncOutcome;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 众阳 HIS — 枣强县中医院镜像表探针查询。
 */
@Api(tags = "众阳HIS-枣强县中医院-镜像查询")
@RestController
@ConditionalOnProperty(prefix = ZaoqiangTcmHospitalConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@RequestMapping(ZaoqiangTcmHospitalConstants.API_PREFIX + "/mirror")
public class ZaoqiangTcmMsunMirrorQueryController
{
    private final MsunHisMirrorQueryService mirrorQueryService;
    private final MsunHisMirrorSyncService mirrorSyncService;
    private final ZaoqiangTcmMsunProperties msunProperties;

    public ZaoqiangTcmMsunMirrorQueryController(
            MsunHisMirrorQueryService mirrorQueryService,
            MsunHisMirrorSyncService mirrorSyncService,
            ZaoqiangTcmMsunProperties msunProperties)
    {
        this.mirrorQueryService = mirrorQueryService;
        this.mirrorSyncService = mirrorSyncService;
        this.msunProperties = msunProperties;
    }

    @ApiOperation("按探针接口键查询 SPD 库镜像表数据")
    @GetMapping("/data/{probeKey}")
    public AjaxResult queryMirrorData(
            @ApiParam(value = "探针 apiKey：depts/identities/drugDict/...", required = true)
            @PathVariable String probeKey,
            @ApiParam("每表返回条数，最大200") @RequestParam(defaultValue = "50") int limit,
            @ApiParam("偏移量") @RequestParam(defaultValue = "0") int offset)
    {
        try
        {
            Map<String, Object> data = mirrorQueryService.queryProbeMirror(msunProperties, probeKey, limit, offset);
            return enrichEnv(AjaxResult.success(data));
        }
        catch (IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (IllegalStateException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("镜像数据查询失败: " + ex.getMessage());
        }
    }

    @ApiOperation("将镜像库批次 upsert 至 SPD 主数据表（科室/人员/分类/厂商/供应商/产品档案）")
    @PostMapping("/spd-sync/{probeKey}")
    public AjaxResult syncSpdMasterData(
            @ApiParam(value = "探针 apiKey：depts/identities/drugDict/dictCategory/suppliers/producers", required = true)
            @PathVariable String probeKey,
            @ApiParam("批次号，留空则取该接口最新批次") @RequestParam(required = false) String batchNo)
    {
        try
        {
            MsunHisMirrorSyncOutcome outcome = mirrorSyncService.syncSpdFromProbe(msunProperties, probeKey, batchNo);
            AjaxResult result = enrichEnv(AjaxResult.success(outcome.toMap()));
            result.put("mirrorSync", outcome.toMap());
            return result;
        }
        catch (IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (IllegalStateException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("SPD 主数据同步失败: " + ex.getMessage());
        }
    }

    @ApiOperation("按 SPD 单号查询 HIS 推送日志（评估文档 §10.4 bill-his）")
    @GetMapping("/bill-his")
    public AjaxResult queryBillHis(
            @RequestParam String billId,
            @RequestParam(required = false) String billType)
    {
        try
        {
            Map<String, Object> data = mirrorQueryService.queryBillHis(msunProperties, billId, billType);
            return enrichEnv(AjaxResult.success(data));
        }
        catch (IllegalStateException | IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("HIS单据推送日志查询失败: " + ex.getMessage());
        }
    }

    @ApiOperation("按明细键查询 HIS 批次库存镜像（评估文档 §10.4 entry-his）")
    @GetMapping("/entry-his")
    public AjaxResult queryEntryHis(
            @RequestParam(required = false) String pharmacyStockId,
            @RequestParam(required = false) String deptId,
            @RequestParam(required = false) String drugId,
            @RequestParam(required = false) String drugSpecPackingId,
            @RequestParam(required = false) String batchNumber)
    {
        try
        {
            Map<String, Object> data = mirrorQueryService.queryEntryHis(
                    msunProperties, pharmacyStockId, deptId, drugId, drugSpecPackingId, batchNumber);
            return enrichEnv(AjaxResult.success(data));
        }
        catch (IllegalStateException | IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("HIS明细镜像查询失败: " + ex.getMessage());
        }
    }

    private AjaxResult enrichEnv(AjaxResult result)
    {
        return result
                .put("vendorCode", MsunVendorConstants.VENDOR_CODE)
                .put("vendorName", MsunVendorConstants.VENDOR_NAME)
                .put("hospitalKey", msunProperties.getHospitalKey())
                .put("hospitalName", msunProperties.getHospitalName())
                .put("tenantId", msunProperties.getTenantId())
                .put("activeEnv", msunProperties.getActiveEnv())
                .put("msunBaseUrl", msunProperties.getBaseUrl());
    }
}
