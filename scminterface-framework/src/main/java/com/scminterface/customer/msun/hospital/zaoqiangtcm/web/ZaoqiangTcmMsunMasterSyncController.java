package com.scminterface.customer.msun.hospital.zaoqiangtcm.web;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.msun.spd.sync.service.MsunSpdMasterPullService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 众阳 HIS — 枣强县中医院：SPD 一键同步主数据（系统间调用，JWT 白名单）。
 */
@Api(tags = "SPD-众阳HIS-枣强-主数据同步")
@RestController
@ConditionalOnProperty(prefix = ZaoqiangTcmHospitalConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@RequestMapping(ZaoqiangTcmHospitalConstants.SPD_API_PREFIX + "/sync")
public class ZaoqiangTcmMsunMasterSyncController
{
    private final MsunSpdMasterPullService pullService;
    private final ZaoqiangTcmMsunProperties msunProperties;

    public ZaoqiangTcmMsunMasterSyncController(
            MsunSpdMasterPullService pullService,
            ZaoqiangTcmMsunProperties msunProperties)
    {
        this.pullService = pullService;
        this.msunProperties = msunProperties;
    }

    @ApiOperation("单条耗材档案同步（2.5.44，按 drugId / drugSpecPackingId）")
    @PostMapping("/materials/single")
    public AjaxResult syncMaterialSingle(@RequestBody Map<String, Object> body)
    {
        try
        {
            Long drugId = parseLong(body != null ? body.get("drugId") : null);
            String drugSpecPackingId = parseString(body != null ? body.get("drugSpecPackingId") : null);
            JSONObject result = pullService.pullMaterialSingle(msunProperties, drugId, drugSpecPackingId);
            return AjaxResult.success(
                    "单条同步完成: " + result.getString("label") + "，共 " + result.getInteger("rows") + " 条",
                    result);
        }
        catch (IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("众阳HIS单条同步失败: " + ex.getMessage());
        }
    }

    @ApiOperation("一键同步：depts|identities|suppliers|producers|categories|materials")
    @PostMapping("/{syncType}")
    public AjaxResult sync(@PathVariable String syncType)
    {
        try
        {
            JSONObject result = pullService.pullByType(msunProperties, syncType);
            return AjaxResult.success("同步完成: " + result.getString("label") + "，共 " + result.getInteger("rows") + " 条", result);
        }
        catch (IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("众阳HIS同步失败: " + ex.getMessage());
        }
    }

    private static Long parseLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty())
        {
            return null;
        }
        return Long.valueOf(text);
    }

    private static String parseString(Object value)
    {
        if (value == null)
        {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
