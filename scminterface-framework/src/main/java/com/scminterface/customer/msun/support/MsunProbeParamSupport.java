package com.scminterface.customer.msun.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探针入参：前端仅传字符串，服务端合并/解析，避免 JS 大整数精度丢失。
 */
public final class MsunProbeParamSupport
{
    private MsunProbeParamSupport()
    {
    }

    public static Map<String, String> mergeParams(Map<String, String> formParams, String paramsJsonOverride)
    {
        Map<String, String> merged = new LinkedHashMap<>();
        if (formParams != null)
        {
            formParams.forEach((key, value) -> {
                if (StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value))
                {
                    merged.put(key, value.trim());
                }
            });
        }
        if (StringUtils.isEmpty(paramsJsonOverride))
        {
            return merged;
        }
        JSONObject override = JSON.parseObject(paramsJsonOverride.trim());
        if (override == null || override.isEmpty())
        {
            return merged;
        }
        for (String key : override.keySet())
        {
            String normalized = stringifyJsonValue(override.get(key));
            if (StringUtils.isNotEmpty(normalized))
            {
                merged.put(key, normalized);
            }
        }
        return merged;
    }

    public static Long parseLongParam(Map<String, String> params, String key)
    {
        if (params == null || StringUtils.isEmpty(key))
        {
            return null;
        }
        String text = params.get(key);
        if (StringUtils.isEmpty(text))
        {
            return null;
        }
        try
        {
            return Long.valueOf(text.trim());
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException(key + " 格式非法: " + text);
        }
    }

    public static Integer parseIntegerParam(Map<String, String> params, String key)
    {
        Long val = parseLongParam(params, key);
        return val == null ? null : val.intValue();
    }

    public static boolean parseBooleanParam(Map<String, String> params, String key, boolean defaultValue)
    {
        if (params == null || StringUtils.isEmpty(key))
        {
            return defaultValue;
        }
        String text = params.get(key);
        if (StringUtils.isEmpty(text))
        {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text.trim()) || "1".equals(text.trim());
    }

    public static String stringParam(Map<String, String> params, String key)
    {
        if (params == null || StringUtils.isEmpty(key))
        {
            return null;
        }
        String text = params.get(key);
        return StringUtils.isEmpty(text) ? null : text.trim();
    }

    private static String stringifyJsonValue(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof String)
        {
            String text = ((String) value).trim();
            return text.isEmpty() ? null : text;
        }
        if (value instanceof Number)
        {
            return numberToPlainString((Number) value);
        }
        if (value instanceof Boolean)
        {
            return value.toString();
        }
        return String.valueOf(value);
    }

    private static String numberToPlainString(Number number)
    {
        if (number instanceof java.math.BigInteger)
        {
            return number.toString();
        }
        if (number instanceof java.math.BigDecimal)
        {
            return ((java.math.BigDecimal) number).toPlainString();
        }
        if (number instanceof Double || number instanceof Float)
        {
            double d = number.doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 9.0E15D)
            {
                return String.valueOf((long) d);
            }
            return number.toString();
        }
        return String.valueOf(number.longValue());
    }
}
