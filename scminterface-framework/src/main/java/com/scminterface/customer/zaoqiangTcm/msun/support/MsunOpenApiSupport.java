package com.scminterface.customer.zaoqiangTcm.msun.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.zaoqiangTcm.msun.config.ZaoqiangTcmMsunEnvProfile;
import com.scminterface.customer.zaoqiangTcm.msun.config.ZaoqiangTcmMsunProperties;
import java.util.Map;

/**
 * 众阳 OpenAPI 公共辅助（URL 拼接、回参包装、客户端创建）。
 */
public final class MsunOpenApiSupport
{
    private MsunOpenApiSupport()
    {
    }

    public static MsunSignedHttpClient createClient(ZaoqiangTcmMsunProperties properties)
    {
        return new MsunSignedHttpClient(
                properties.getAppId(),
                properties.getAppSecret(),
                properties.getHospitalId(),
                properties.getOrgId(),
                properties.getLoginUser());
    }

    public static MsunSignedHttpClient createClient(ZaoqiangTcmMsunEnvProfile env)
    {
        return new MsunSignedHttpClient(
                env.getAppId(),
                env.getAppSecret(),
                env.getHospitalId(),
                env.getOrgId(),
                env.getLoginUser());
    }

    public static String buildUrl(String baseUrl, String path)
    {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return base + normalizedPath;
    }

    public static String buildUrl(ZaoqiangTcmMsunProperties properties, String path)
    {
        return buildUrl(properties.getBaseUrl(), path);
    }

    public static JSONObject wrapRawResponse(String raw, Object requestParams)
    {
        JSONObject result = new JSONObject();
        result.put("requestParams", requestParams);
        try
        {
            result.put("hisBody", JSON.parse(raw));
        }
        catch (Exception ex)
        {
            result.put("hisBody", raw);
            result.put("parseError", ex.getMessage());
        }
        return result;
    }

    public static JSONObject wrapRawResponse(String raw, Map<String, Object> requestParams)
    {
        return wrapRawResponse(raw, (Object) requestParams);
    }
}
