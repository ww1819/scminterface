package com.scminterface.customer.hengsuiThird.his;

/**
 * 与 SPD {@code com.spd.his.constants.HisBillingTenantConstants} 保持一致。
 */
public final class HisBillingTenantConstants
{
    private HisBillingTenantConstants()
    {
    }

    public static final String TENANT_HENGSHUI_THIRD = "hengsui-third-001";

    public static final String SETTING_LV_AUTO_CONSUME_ENABLED = "dept.billing.lv.auto_consume_enabled";

    public static final String SETTING_BILLING_AUTO_REFUND_ENABLED = "dept.billing.auto_refund_enabled";

    /** 耗材 sys_config：SPD 内部接口基址 */
    public static final String CONFIG_SPD_INTERNAL_BASE_URL = "spd.internal.base_url";

    /** 耗材 sys_config：内部接口密钥 */
    public static final String CONFIG_INTERNAL_API_KEY = "his.internal.api_key";
}
