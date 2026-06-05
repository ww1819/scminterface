package com.scminterface.customer.zaoqiangTcm;

/**
 * 枣强县中医院租户常量。
 */
public final class ZaoqiangTcmTenantConstants
{
    public static final String TENANT_ID = "zaoqiang-tcm-001";

    /** 仅供本租户探针使用的 API 前缀（需登录 token，未加入全局白名单） */
    public static final String MSUN_PROBE_API_PREFIX = "/api/customer/zaoqiang-tcm-001/msun";

    /** SPD 查询类接口探针前缀（接口文档2：2.5.44/58/62/63/43/102） */
    public static final String MSUN_SPD_QUERY_API_PREFIX = MSUN_PROBE_API_PREFIX + "/spd/query";

    private ZaoqiangTcmTenantConstants()
    {
    }
}
