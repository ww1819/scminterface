package com.scminterface.customer.msun.hospital.zaoqiangtcm.web;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.MsunVendorConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.msun.mirror.service.MsunHisMirrorSyncService;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorSyncOutcome;
import com.scminterface.customer.msun.service.MsunProbeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 众阳 HIS — 枣强县中医院字典探针入口（独立 URL 前缀，共用厂家 Service）。
 */
@Api(tags = "众阳HIS-枣强县中医院-字典探针")
@RestController
@ConditionalOnProperty(prefix = ZaoqiangTcmHospitalConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@RequestMapping(ZaoqiangTcmHospitalConstants.API_PREFIX)
public class ZaoqiangTcmMsunProbeController
{
    private final MsunProbeService probeService;
    private final ZaoqiangTcmMsunProperties msunProperties;
    private final MsunHisMirrorSyncService mirrorSyncService;

    public ZaoqiangTcmMsunProbeController(
            MsunProbeService probeService,
            ZaoqiangTcmMsunProperties msunProperties,
            MsunHisMirrorSyncService mirrorSyncService)
    {
        this.probeService = probeService;
        this.msunProperties = msunProperties;
        this.mirrorSyncService = mirrorSyncService;
    }

    @ApiOperation("当前众阳连接环境（不含密钥）")
    @GetMapping("/env")
    public AjaxResult currentEnv()
    {
        Map<String, Object> info = new LinkedHashMap<>(12);
        info.put("vendorCode", MsunVendorConstants.VENDOR_CODE);
        info.put("vendorName", MsunVendorConstants.VENDOR_NAME);
        info.put("hospitalKey", msunProperties.getHospitalKey());
        info.put("hospitalName", msunProperties.getHospitalName());
        info.put("tenantId", ZaoqiangTcmHospitalConstants.TENANT_ID);
        info.put("activeEnv", msunProperties.getActiveEnv());
        info.put("label", msunProperties.getActiveEnvLabel());
        info.put("baseUrl", msunProperties.getBaseUrl());
        info.put("appId", msunProperties.getAppId());
        info.put("hospitalId", msunProperties.getHospitalId());
        info.put("orgId", msunProperties.getOrgId());
        info.put("signType", msunProperties.getSignType());
        info.put("documentUrl", msunProperties.getDocumentUrl());
        info.put("deptsUrl", msunProperties.deptsUrl());
        info.put("identitiesUrl", msunProperties.identitiesUrl());
        info.put("identitiesAllPath", ZaoqiangTcmHospitalConstants.API_PREFIX + "/identities/all");
        return AjaxResult.success(info);
    }

    @ApiOperation("2.1.9 科室基本信息（原样返回 HIS JSON）")
    @GetMapping("/depts")
    public AjaxResult fetchDepts(
            @ApiParam("院区ID，可选") @RequestParam(required = false) Long hospitalAreaId,
            @ApiParam("0启用 1作废 -1全量，默认 -1") @RequestParam(required = false, defaultValue = "-1") Integer invalidFlag,
            @ApiParam("科室ID，可选") @RequestParam(required = false) Long deptId,
            @ApiParam("科室名称，可选") @RequestParam(required = false) String deptName)
    {
        try
        {
            JSONObject data = probeService.fetchDepts(msunProperties, hospitalAreaId, invalidFlag, deptId, deptName);
            MsunHisMirrorSyncOutcome syncOutcome = mirrorSyncService.syncQueryResult(msunProperties, "2.1.9", data);
            return enrichEnv(AjaxResult.success(data), syncOutcome);
        }
        catch (Exception ex)
        {
            return AjaxResult.error("科室接口调用失败: " + ex.getMessage());
        }
    }

    @ApiOperation("2.1.12 用户身份信息（原样返回 HIS JSON）")
    @GetMapping("/identities")
    public AjaxResult fetchIdentities(
            @ApiParam("角色类型 0-8，与 deptId/identityId/userId 至少传一") @RequestParam(required = false) String roleType,
            @ApiParam("科室ID") @RequestParam(required = false) Long deptId,
            @ApiParam("身份ID") @RequestParam(required = false) Long identityId,
            @ApiParam("用户ID") @RequestParam(required = false) Long userId)
    {
        try
        {
            JSONObject data = probeService.fetchIdentities(msunProperties, roleType, deptId, identityId, userId);
            MsunHisMirrorSyncOutcome syncOutcome = mirrorSyncService.syncQueryResult(msunProperties, "2.1.12", data);
            return enrichEnv(AjaxResult.success(data), syncOutcome);
        }
        catch (IllegalArgumentException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("人员身份接口调用失败: " + ex.getMessage());
        }
    }

    @ApiOperation("2.1.12 获取全部用户：按 roleType 0-8 遍历合并（供下载/镜像落库）")
    @GetMapping("/identities/all")
    public AjaxResult fetchIdentitiesAll()
    {
        try
        {
            JSONObject data = probeService.fetchIdentitiesAllRoleTypes(msunProperties);
            MsunHisMirrorSyncOutcome syncOutcome = mirrorSyncService.syncQueryResult(msunProperties, "2.1.12", data);
            return enrichEnv(AjaxResult.success(data), syncOutcome);
        }
        catch (Exception ex)
        {
            return AjaxResult.error("获取全部用户失败: " + ex.getMessage());
        }
    }

    @ApiOperation("2.1.12 快速探针：取首个科室 ID 再查人员身份")
    @GetMapping("/identities/sample")
    public AjaxResult fetchIdentitiesSample(
            @ApiParam("角色类型，默认 0 管理员") @RequestParam(required = false, defaultValue = "0") String roleType)
    {
        try
        {
            JSONObject data = probeService.fetchIdentitiesByFirstDept(msunProperties, roleType);
            MsunHisMirrorSyncOutcome syncOutcome = mirrorSyncService.syncQueryResult(msunProperties, "2.1.12", data);
            return enrichEnv(AjaxResult.success(data), syncOutcome);
        }
        catch (Exception ex)
        {
            return AjaxResult.error("人员身份探针失败: " + ex.getMessage());
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
}
