package com.scminterface.customer.zaoqiangTcm.msun.web;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.zaoqiangTcm.ZaoqiangTcmTenantConstants;
import com.scminterface.customer.zaoqiangTcm.msun.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.zaoqiangTcm.msun.service.ZaoqiangTcmMsunProbeService;
import java.util.LinkedHashMap;
import java.util.Map;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 枣强中医院众阳字典探针：仅查看 HIS 回参，不做落库。
 * <p>路径含租户 ID，且需系统登录 token（未加入 JWT 白名单）。</p>
 */
@Api(tags = "枣强中医院-众阳字典探针(zaoqiang-tcm-001)")
@RestController
@ConditionalOnProperty(prefix = "scminterface.customer.zaoqiang-tcm-001.msun", name = "enabled", havingValue = "true")
@RequestMapping(ZaoqiangTcmTenantConstants.MSUN_PROBE_API_PREFIX)
public class ZaoqiangTcmMsunProbeController
{
    private final ZaoqiangTcmMsunProbeService probeService;
    private final ZaoqiangTcmMsunProperties msunProperties;

    public ZaoqiangTcmMsunProbeController(
            ZaoqiangTcmMsunProbeService probeService,
            ZaoqiangTcmMsunProperties msunProperties)
    {
        this.probeService = probeService;
        this.msunProperties = msunProperties;
    }

    @ApiOperation("当前众阳连接环境（不含密钥）")
    @GetMapping("/env")
    public AjaxResult currentEnv()
    {
        Map<String, Object> info = new LinkedHashMap<>(8);
        info.put("tenantId", ZaoqiangTcmTenantConstants.TENANT_ID);
        info.put("activeEnv", msunProperties.getActiveEnv());
        info.put("label", msunProperties.activeProfile().getLabel());
        info.put("baseUrl", msunProperties.getBaseUrl());
        info.put("appId", msunProperties.getAppId());
        info.put("hospitalId", msunProperties.getHospitalId());
        info.put("orgId", msunProperties.getOrgId());
        info.put("signType", msunProperties.getSignType());
        info.put("documentUrl", msunProperties.getDocumentUrl());
        info.put("deptsUrl", msunProperties.deptsUrl());
        info.put("identitiesUrl", msunProperties.identitiesUrl());
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
            return enrichEnv(AjaxResult.success(probeService.fetchDepts(hospitalAreaId, invalidFlag, deptId, deptName)));
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
            return enrichEnv(AjaxResult.success(probeService.fetchIdentities(roleType, deptId, identityId, userId)));
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

    @ApiOperation("2.1.12 快速探针：取首个科室 ID 再查人员身份")
    @GetMapping("/identities/sample")
    public AjaxResult fetchIdentitiesSample(
            @ApiParam("角色类型，默认 0 管理员") @RequestParam(required = false, defaultValue = "0") String roleType)
    {
        try
        {
            return enrichEnv(AjaxResult.success(probeService.fetchIdentitiesByFirstDept(roleType)));
        }
        catch (Exception ex)
        {
            return AjaxResult.error("人员身份探针失败: " + ex.getMessage());
        }
    }

    private AjaxResult enrichEnv(AjaxResult result)
    {
        return result
                .put("tenantId", ZaoqiangTcmTenantConstants.TENANT_ID)
                .put("activeEnv", msunProperties.getActiveEnv())
                .put("msunBaseUrl", msunProperties.getBaseUrl());
    }
}
