package com.scminterface.customer.msun.hospital.zaoqiangtcm;

/**
 * 枣强县中医院在众阳 HIS 下的客户标识与 API 前缀。
 */
public final class ZaoqiangTcmHospitalConstants
{
    public static final String HOSPITAL_KEY = "zaoqiang-tcm-001";

    public static final String HOSPITAL_NAME = "枣强县中医院";

    /** SPD 租户 ID，与 SPD 侧 zaoqiang-tcm-001 一致 */
    public static final String TENANT_ID = HOSPITAL_KEY;

    public static final String CONFIG_PREFIX = "scminterface.vendor.msun.hospitals." + HOSPITAL_KEY;

    public static final String API_PREFIX = "/api/vendor/msun/hospitals/" + HOSPITAL_KEY;

    public static final String SPD_QUERY_API_PREFIX = API_PREFIX + "/spd/query";

    /** SPD 调用前置机：推送/实时查询/主数据同步（按 hospitalKey 隔离） */
    public static final String SPD_API_PREFIX = "/api/spd/msun/hospitals/" + HOSPITAL_KEY;

    private ZaoqiangTcmHospitalConstants()
    {
    }
}
