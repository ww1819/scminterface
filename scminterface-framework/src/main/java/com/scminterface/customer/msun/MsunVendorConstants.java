package com.scminterface.customer.msun;

/**
 * 众阳 HIS 厂家级常量。
 */
public final class MsunVendorConstants
{
    public static final String VENDOR_CODE = "msun";

    public static final String VENDOR_NAME = "众阳健康";

    /** 厂家 API 根路径 */
    public static final String API_PREFIX = "/api/vendor/msun";

    /** 医院客户列表 */
    public static final String HOSPITALS_API = API_PREFIX + "/hospitals";

    private MsunVendorConstants()
    {
    }

    public static String hospitalApiPrefix(String hospitalKey)
    {
        return API_PREFIX + "/hospitals/" + hospitalKey;
    }

    public static String spdQueryApiPrefix(String hospitalKey)
    {
        return hospitalApiPrefix(hospitalKey) + "/spd/query";
    }
}
