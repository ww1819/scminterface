package com.scminterface.customer.msun.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.scminterface.common.utils.StringUtils;

/**
 * 众阳 HIS 请求 JSON：雪花 ID 超过 JS 安全整数范围时，必须以字符串写入 JSON。
 */
public final class MsunHisJsonSupport
{
    private MsunHisJsonSupport()
    {
    }

    public static String toRequestJson(Object body)
    {
        if (body == null)
        {
            return "";
        }
        return JSON.toJSONString(body, JSONWriter.Feature.WriteLongAsString);
    }

    /** 校验整型 ID 后以字符串放入请求体，避免 JSON 数字精度丢失。 */
    public static String requireSnowflakeId(String val, String label)
    {
        if (StringUtils.isEmpty(val))
        {
            throw new IllegalArgumentException(label + " 不能为空");
        }
        String trimmed = val.trim();
        try
        {
            Long.parseLong(trimmed);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(label + " 格式非法: " + val);
        }
        return trimmed;
    }
}
