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

    /** 日志/探针展示用：超长 JSON 截断，避免刷屏。 */
    public static String truncateForLog(String text, int maxLen)
    {
        if (text == null)
        {
            return null;
        }
        if (maxLen <= 0 || text.length() <= maxLen)
        {
            return text;
        }
        return text.substring(0, maxLen) + "...(truncated, total " + text.length() + " chars)";
    }

    public static String truncateForLog(Object body, int maxLen)
    {
        if (body == null)
        {
            return null;
        }
        return truncateForLog(toRequestJson(body), maxLen);
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
