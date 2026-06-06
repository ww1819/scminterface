package com.scminterface.customer.msun.hospital.zaoqiangtcm.web;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.MsunVendorConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.msun.spd.billpush.service.MsunSpdBillPushService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 众阳 HIS — 枣强：SPD 出库/退库单据查询与 HIS 推送（回写 SPD 主从表 his_push_status）。
 */
@Api(tags = "众阳HIS-枣强-单据推送")
@RestController
@ConditionalOnProperty(prefix = ZaoqiangTcmHospitalConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@RequestMapping(ZaoqiangTcmHospitalConstants.API_PREFIX + "/spd/bill-push")
public class ZaoqiangTcmMsunSpdBillPushController
{
    private final MsunSpdBillPushService billPushService;
    private final ZaoqiangTcmMsunProperties msunProperties;

    public ZaoqiangTcmMsunSpdBillPushController(
            MsunSpdBillPushService billPushService,
            ZaoqiangTcmMsunProperties msunProperties)
    {
        this.billPushService = billPushService;
        this.msunProperties = msunProperties;
    }

    @ApiOperation("查询 SPD 出库/退库单据明细（支持单号、产品、规格、科室、仓库模糊）")
    @GetMapping("/entries")
    public AjaxResult queryEntries(
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String materialSpeci,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) Integer billType,
            @RequestParam(required = false) Integer billStatus,
            @RequestParam(required = false) String hisPushStatus,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize)
    {
        try
        {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("billNo", billNo);
            params.put("materialName", materialName);
            params.put("materialSpeci", materialSpeci);
            params.put("departmentName", departmentName);
            params.put("warehouseName", warehouseName);
            params.put("billType", billType);
            params.put("billStatus", billStatus);
            params.put("hisPushStatus", hisPushStatus);
            params.put("pageNum", pageNum);
            params.put("pageSize", pageSize);
            Map<String, Object> data = billPushService.queryBillEntries(msunProperties, params);
            return enrichEnv(AjaxResult.success(data));
        }
        catch (IllegalArgumentException | IllegalStateException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("单据明细查询失败: " + ex.getMessage());
        }
    }

    @ApiOperation("批量推送 HIS（仅已审核单；回写 SPD 主表/明细 his_push_status）")
    @PostMapping("/push")
    public AjaxResult pushBills(@RequestBody Map<String, Object> body)
    {
        try
        {
            List<Long> billIds = parseBillIds(body);
            Map<String, Object> data = billPushService.pushBills(msunProperties, billIds);
            return enrichEnv(AjaxResult.success(resolvePushMessage(data), data));
        }
        catch (IllegalArgumentException | IllegalStateException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("单据推送失败: " + ex.getMessage());
        }
    }

    @ApiOperation("单条推送 HIS")
    @PostMapping("/push/{billId}")
    public AjaxResult pushOne(
            @ApiParam(value = "SPD 主单 id", required = true) @PathVariable Long billId)
    {
        try
        {
            List<Long> ids = new ArrayList<>(1);
            ids.add(billId);
            Map<String, Object> data = billPushService.pushBills(msunProperties, ids);
            return enrichEnv(AjaxResult.success(resolvePushMessage(data), data));
        }
        catch (IllegalArgumentException | IllegalStateException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("单据推送失败: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Long> parseBillIds(Map<String, Object> body)
    {
        if (body == null)
        {
            throw new IllegalArgumentException("请指定 billIds");
        }
        Object raw = body.get("billIds");
        if (!(raw instanceof List))
        {
            throw new IllegalArgumentException("billIds 须为数组");
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : (List<?>) raw)
        {
            if (item == null)
            {
                continue;
            }
            if (item instanceof Number)
            {
                ids.add(((Number) item).longValue());
            }
            else
            {
                ids.add(Long.parseLong(String.valueOf(item).trim()));
            }
        }
        return ids;
    }

    private static String resolvePushMessage(Map<String, Object> data)
    {
        if (data == null)
        {
            return "推送完成";
        }
        Object msg = data.get("message");
        return msg != null && String.valueOf(msg).trim().length() > 0
                ? String.valueOf(msg)
                : "推送完成";
    }

    private AjaxResult enrichEnv(AjaxResult result)
    {
        return result
                .put("vendorCode", MsunVendorConstants.VENDOR_CODE)
                .put("vendorName", MsunVendorConstants.VENDOR_NAME)
                .put("hospitalKey", msunProperties.getHospitalKey())
                .put("hospitalName", msunProperties.getHospitalName())
                .put("tenantId", msunProperties.getTenantId())
                .put("activeEnv", msunProperties.getActiveEnv());
    }
}
