package com.scminterface.customer.hengsuiThird.his.service;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.hengsuiThird.his.HisBillingTenantConstants;
import com.scminterface.framework.web.mapper.SbTenantSettingMapper;

@Service
public class TenantBillingSettingService
{
    @Autowired
    private SbTenantSettingMapper sbTenantSettingMapper;

    @DataSource(DataSourceType.SPD)
    public Map<String, String> getBillingSettings(String tenantId)
    {
        String tid = resolveTenantId(tenantId);
        Map<String, String> m = new HashMap<>();
        m.put("lvAutoConsumeEnabled", nz(getValue(tid, HisBillingTenantConstants.SETTING_LV_AUTO_CONSUME_ENABLED)));
        m.put("billingAutoRefundEnabled", nz(getValue(tid, HisBillingTenantConstants.SETTING_BILLING_AUTO_REFUND_ENABLED)));
        return m;
    }

    @DataSource(DataSourceType.SPD)
    public void saveBillingSettings(String tenantId, String lvAutoConsumeEnabled, String billingAutoRefundEnabled)
    {
        String tid = resolveTenantId(tenantId);
        validate01(lvAutoConsumeEnabled, "lvAutoConsumeEnabled");
        validate01(billingAutoRefundEnabled, "billingAutoRefundEnabled");
        saveOne(tid, HisBillingTenantConstants.SETTING_LV_AUTO_CONSUME_ENABLED, lvAutoConsumeEnabled,
            "低值计费抓取后自动生成消耗");
        saveOne(tid, HisBillingTenantConstants.SETTING_BILLING_AUTO_REFUND_ENABLED, billingAutoRefundEnabled,
            "计费退费镜像抓取后自动返还库存");
    }

    @DataSource(DataSourceType.SPD)
    public boolean isAnyAutoProcessEnabled(String tenantId)
    {
        if (!HisBillingTenantConstants.TENANT_HENGSHUI_THIRD.equals(resolveTenantId(tenantId)))
        {
            return false;
        }
        return "1".equals(nz(getValue(resolveTenantId(tenantId), HisBillingTenantConstants.SETTING_LV_AUTO_CONSUME_ENABLED)))
            || "1".equals(nz(getValue(resolveTenantId(tenantId), HisBillingTenantConstants.SETTING_BILLING_AUTO_REFUND_ENABLED)));
    }

    private String getValue(String tenantId, String key)
    {
        try
        {
            return sbTenantSettingMapper.selectValueByTenantAndKey(tenantId, key);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void saveOne(String tenantId, String key, String value, String remark)
    {
        String existing = getValue(tenantId, key);
        if (existing == null)
        {
            sbTenantSettingMapper.insertSetting(tenantId, key, value, remark);
        }
        else
        {
            sbTenantSettingMapper.updateSettingValue(tenantId, key, value, remark);
        }
    }

    private static String resolveTenantId(String tenantId)
    {
        return StringUtils.isNotBlank(tenantId) ? tenantId.trim() : HisBillingTenantConstants.TENANT_HENGSHUI_THIRD;
    }

    private static String nz(String v)
    {
        return "1".equals(StringUtils.trimToEmpty(v)) ? "1" : "0";
    }

    private static void validate01(String v, String name)
    {
        String t = StringUtils.trimToEmpty(v);
        if (!"0".equals(t) && !"1".equals(t))
        {
            throw new IllegalArgumentException(name + " 仅支持 0 或 1");
        }
    }
}
