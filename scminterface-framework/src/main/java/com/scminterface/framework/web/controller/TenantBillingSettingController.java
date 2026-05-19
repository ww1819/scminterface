package com.scminterface.framework.web.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.hengsuiThird.his.HisBillingTenantConstants;
import com.scminterface.customer.hengsuiThird.his.service.TenantBillingSettingService;

/**
 * 衡水三院：HIS 计费自动处理开关（与 SPD sb_tenant_setting 同源）。
 */
@RestController
@RequestMapping("/api/hengsui/billingSetting")
public class TenantBillingSettingController
{
    @Autowired
    private TenantBillingSettingService tenantBillingSettingService;

    @GetMapping
    public AjaxResult get(@RequestParam(value = "tenantId", required = false) String tenantId)
    {
        return AjaxResult.success(tenantBillingSettingService.getBillingSettings(tenantId));
    }

    @PutMapping
    public AjaxResult save(@RequestBody Map<String, String> body,
        @RequestParam(value = "tenantId", required = false) String tenantId)
    {
        if (body == null)
        {
            return AjaxResult.error("请求体不能为空");
        }
        try
        {
            tenantBillingSettingService.saveBillingSettings(
                tenantId,
                body.getOrDefault("lvAutoConsumeEnabled", "0"),
                body.getOrDefault("billingAutoRefundEnabled", "0"));
            return AjaxResult.success();
        }
        catch (IllegalArgumentException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/defaultTenant")
    public AjaxResult defaultTenant()
    {
        return AjaxResult.success(HisBillingTenantConstants.TENANT_HENGSHUI_THIRD);
    }
}
