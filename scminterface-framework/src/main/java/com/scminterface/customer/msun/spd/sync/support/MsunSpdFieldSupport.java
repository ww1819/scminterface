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

    /** HIS invalidFlag：0/启用 → SPD del_flag=0；1/作废/停用 → 1（科室/供应商/厂商等主数据） */
    public static int toFdDelFlag(String invalidFlag)
    {
        return isHisEnabled(invalidFlag) ? 0 : 1;
    }

    /**
     * 众阳 2.5.44 产品档案 invalidFlag → {@code fd_material.is_use}（字典 is_use_status：1=启用，2=停用）。
     * <p>与 {@link #toFdDelFlag} 分离：产品档案启停用不写 del_flag，SPD 侧逻辑删除由人工维护且同步时不覆盖。
     */
    public static String toFdMaterialIsUse(String invalidFlag)
    {
        return isHisEnabled(invalidFlag) ? "1" : "2";
    }

    /**
     * 众阳 invalidFlag 常见取值：0/启用/否 为有效；1/作废/停用/是 为作废。
     * 2.5.44 字典回参可能直接返回中文「启用」。
     */
    public static boolean isHisEnabled(String invalidFlag)
    {
        if (StringUtils.isEmpty(invalidFlag))
        {
            return true;
        }
        String v = invalidFlag.trim();
        if ("0".equals(v) || "启用".equals(v) || "否".equals(v))
        {
            return true;
        }
        if ("1".equals(v) || "作废".equals(v) || "停用".equals(v) || "是".equals(v))
        {
            return false;
        }
        return !"invalid".equalsIgnoreCase(v);
    }

    /**
     * 2.5.44 回参行常不含 materialOrDrug，需从镜像 request_params_json 推断是否材料查询。
     */
    public static boolean inferMaterialFromDrugDictRequest(String requestParamsJson)
    {
        if (StringUtils.isEmpty(requestParamsJson))
        {
            return false;
        }
        try
        {
            com.alibaba.fastjson2.JSONObject req = com.alibaba.fastjson2.JSON.parseObject(requestParamsJson);
            Object mod = req.get("materialOrDrug");
            if (mod == null)
            {
                return false;
            }
            String text = String.valueOf(mod).trim();
            return "1".equals(text);
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    /** 众阳占位 ID（如 -1）不作为 SPD 外键主数据写入 */
    public static boolean isPlaceholderHisId(String hisId)
    {
        if (StringUtils.isEmpty(hisId))
        {
            return true;
        }
        String v = hisId.trim();
        return "-1".equals(v) || "0".equals(v);
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

    /**
     * 众阳最小包装单位 ID（min_packing_id）；缺失时用名称生成稳定键供 fd_unit.his_unit_id 使用。
     */
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
