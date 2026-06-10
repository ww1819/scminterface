package com.scminterface.customer.msun.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 众阳 HIS 调用排错信息（Headers / 入参 / 回参），供联调页与单据推送页展示。
 */
public final class MsunHisInvokeDebugSupport
{
    private MsunHisInvokeDebugSupport()
    {
    }

    public static Map<String, Object> buildDebug(
            MsunHospitalRuntime runtime,
            String apiCode,
            String apiPath,
            MsunSignedHttpResult http,
            JSONObject wrapped)
    {
        Map<String, Object> debug = new LinkedHashMap<>(16);
        if (StringUtils.isNotEmpty(apiCode))
        {
            debug.put("apiCode", apiCode);
        }
        if (StringUtils.isNotEmpty(apiPath))
        {
            debug.put("apiPath", apiPath);
        }
        if (http != null)
        {
            debug.put("method", http.getMethod());
            debug.put("url", http.getUrl());
            debug.put("httpStatus", http.getHttpStatus());
            debug.put("requestHeaders", maskSensitiveHeaders(http.getRequestHeaders()));
            String requestBodyRaw = http.getRequestBody();
            if (StringUtils.isNotEmpty(requestBodyRaw))
            {
                debug.put("requestBodyRaw", requestBodyRaw);
            }
            Object reqBody = parseRequestBodyForDebug(requestBodyRaw);
            if (reqBody != null)
            {
                debug.put("requestBody", reqBody);
            }
            debug.put("responseRaw", parseJsonOrRaw(http.getResponseBody()));
        }
        if (runtime != null)
        {
            debug.put("activeEnv", runtime.getActiveEnv());
            debug.put("baseUrl", runtime.getBaseUrl());
            debug.put("hospitalId", runtime.getHospitalId());
            debug.put("orgId", runtime.getOrgId());
            debug.put("appId", runtime.getAppId());
        }
        if (wrapped != null)
        {
            Object params = wrapped.get("requestParams");
            if (params != null && !debug.containsKey("requestBody"))
            {
                debug.put("requestBody", params);
            }
            debug.put("hisBody", wrapped.get("hisBody"));
        }
        return debug;
    }

    public static void attachInvokeDebug(JSONObject wrapped, Map<String, Object> debug)
    {
        if (wrapped == null || debug == null || debug.isEmpty())
        {
            return;
        }
        wrapped.put("hisInvoke", debug);
    }

    public static Map<String, String> maskSensitiveHeaders(Map<String, String> headers)
    {
        if (headers == null || headers.isEmpty())
        {
            return headers;
        }
        Map<String, String> masked = new LinkedHashMap<>(headers.size());
        headers.forEach((key, value) -> {
            if (value == null)
            {
                masked.put(key, null);
                return;
            }
            String lower = key == null ? "" : key.toLowerCase();
            if ("license".equals(lower))
            {
                masked.put(key, maskMiddle(value, 8, 6));
            }
            else if ("sign".equals(lower))
            {
                masked.put(key, maskMiddle(value, 10, 6));
            }
            else if ("appid".equals(lower) && value.length() > 12)
            {
                masked.put(key, value.substring(0, 8) + "****" + value.substring(value.length() - 4));
            }
            else
            {
                masked.put(key, value);
            }
        });
        return masked;
    }

    private static String maskMiddle(String value, int head, int tail)
    {
        if (value.length() <= head + tail + 3)
        {
            return "****";
        }
        return value.substring(0, head) + "****...****" + value.substring(value.length() - tail);
    }

    public static Object parseRequestBodyForDebug(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return null;
        }
        try
        {
            JSONObject obj = JSON.parseObject(json);
            if (obj != null)
            {
                obj.remove("_spdLogMeta");
            }
            return obj;
        }
        catch (Exception ex)
        {
            return json;
        }
    }

    public static Object parseJsonOrRaw(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return null;
        }
        try
        {
            return JSON.parse(raw);
        }
        catch (Exception ex)
        {
            return raw;
        }
    }
}
