package com.scminterface.customer.msun.hospital.zaoqiangtcm.web;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.msun.service.MsunSpdQueryService;
import com.scminterface.customer.msun.spd.service.MsunSpdPushInvokeResult;
import com.scminterface.customer.msun.spd.service.MsunSpdPushService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 众阳 HIS — 枣强县中医院：SPD 单据推送与审核用实时查询（2.5.41/42/43）。
 */
@Api(tags = "SPD-众阳HIS-枣强-单据推送")
@RestController
@ConditionalOnProperty(prefix = ZaoqiangTcmHospitalConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@RequestMapping(ZaoqiangTcmHospitalConstants.SPD_API_PREFIX)
public class ZaoqiangTcmMsunSpdPushController
{
    private final MsunSpdPushService pushService;
    private final MsunSpdQueryService queryService;
    private final ZaoqiangTcmMsunProperties msunProperties;

    public ZaoqiangTcmMsunSpdPushController(
            MsunSpdPushService pushService,
            MsunSpdQueryService queryService,
            ZaoqiangTcmMsunProperties msunProperties)
    {
        this.pushService = pushService;
        this.queryService = queryService;
        this.msunProperties = msunProperties;
    }

    @ApiOperation("2.5.41 药品材料入库（药库→药房）")
    @PostMapping("/push/drug-stocks-new")
    public AjaxResult pushDrugStocksNew(@RequestBody Map<String, Object> body)
    {
        return invokePush(() -> pushService.pushDrugStocksNew(msunProperties, body, extractLogMeta(body)));
    }

    @ApiOperation("2.5.42 药品材料退库")
    @PostMapping("/push/drug-stocks-return")
    public AjaxResult pushDrugStocksReturn(@RequestBody Map<String, Object> body)
    {
        return invokePush(() -> pushService.pushDrugStocksReturn(msunProperties, body, extractLogMeta(body)));
    }

    @ApiOperation("2.5.43 药房批次库存（退库审核实时校验）")
    @GetMapping("/query/drug-batch-stocks")
    public AjaxResult queryDrugBatchStocks(
            @RequestParam Long deptId,
            @RequestParam Long drugId,
            @RequestParam Long drugSpecPackingId)
    {
        try
        {
            JSONObject data = queryService.queryDrugBatchStocks(msunProperties, deptId, drugId, drugSpecPackingId);
            return AjaxResult.success(data);
        }
        catch (IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("2.5.43 查询失败: " + ex.getMessage());
        }
    }

    private AjaxResult invokePush(PushCallable callable)
    {
        try
        {
            MsunSpdPushInvokeResult invoke = callable.call();
            JSONObject data = invoke.getWrappedResponse();
            Map<String, Object> payload = new HashMap<>(4);
            payload.put("data", data);
            payload.put("hisInvoke", invoke.getDebug());
            Object hisBody = data.get("hisBody");
            if (hisBody instanceof JSONObject)
            {
                JSONObject hb = (JSONObject) hisBody;
                if (!Boolean.TRUE.equals(hb.getBoolean("success")))
                {
                    return AjaxResult.error(hb.getString("message") != null ? hb.getString("message") : "HIS推送失败", payload);
                }
            }
            return AjaxResult.success("推送成功", payload);
        }
        catch (IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("HIS推送失败: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractLogMeta(Map<String, Object> body)
    {
        Map<String, Object> meta = new HashMap<>(8);
        if (body == null)
        {
            return meta;
        }
        Object m = body.get("_spdLogMeta");
        if (m instanceof Map)
        {
            meta.putAll((Map<String, Object>) m);
        }
        return meta;
    }

    @FunctionalInterface
    private interface PushCallable
    {
        MsunSpdPushInvokeResult call() throws Exception;
    }
}
