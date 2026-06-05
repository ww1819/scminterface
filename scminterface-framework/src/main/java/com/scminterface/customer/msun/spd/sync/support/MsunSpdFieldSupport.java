package com.scminterface.customer.msun.spd.sync.support;

import com.scminterface.common.utils.StringUtils;

/**
 * 众阳镜像行 → SPD 主数据字段转换。
 */
public final class MsunSpdFieldSupport
{
    private static final String MSUN_SYNC_BY = "msun-mirror-sync";

    private MsunSpdFieldSupport()
    {
    }

    public static String syncBy()
    {
        return MSUN_SYNC_BY;
    }

    /** HIS invalidFlag：0 启用 → SPD del_flag=0；其它视为作废/删除 */
    public static int toFdDelFlag(String invalidFlag)
    {
        return "0".equals(StringUtils.trim(invalidFlag)) ? 0 : 1;
    }

    /** sys_user.status：0 正常 1 停用 */
    public static String toUserStatus(String invalidFlag)
    {
        return "0".equals(StringUtils.trim(invalidFlag)) ? "0" : "1";
    }

    /** sys_user.del_flag：0 存在 2 删除 */
    public static String toUserDelFlag(String invalidFlag)
    {
        return "0".equals(StringUtils.trim(invalidFlag)) ? "0" : "2";
    }

    public static String firstNonBlank(String... values)
    {
        if (values == null)
        {
            return null;
        }
        for (String v : values)
        {
            if (StringUtils.isNotEmpty(v))
            {
                return v.trim();
            }
        }
        return null;
    }

    /** HIS 单位 ID 缺失时用名称生成稳定键 */
    public static String resolveHisUnitId(String hisUnitId, String unitName)
    {
        if (StringUtils.isNotEmpty(hisUnitId))
        {
            return hisUnitId.trim();
        }
        if (StringUtils.isNotEmpty(unitName))
        {
            return "N:" + unitName.trim();
        }
        return null;
    }

    public static String truncate(String value, int maxLen)
    {
        if (value == null || maxLen <= 0)
        {
            return value;
        }
        String t = value.trim();
        return t.length() <= maxLen ? t : t.substring(0, maxLen);
    }
}
