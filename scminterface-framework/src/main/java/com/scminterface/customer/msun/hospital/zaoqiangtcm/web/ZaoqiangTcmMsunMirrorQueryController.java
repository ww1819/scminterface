package com.scminterface.customer.msun.hospital.zaoqiangtcm.web;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.MsunVendorConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.msun.mirror.service.MsunHisMirrorQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ZaoqiangTcmMsunProperties msunProperties;

    public ZaoqiangTcmMsunMirrorQueryController(
            MsunHisMirrorQueryService mirrorQueryService,
            ZaoqiangTcmMsunProperties msunProperties)
    {
        this.mirrorQueryService = mirrorQueryService;
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
}
