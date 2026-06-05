package com.scminterface.customer.zaoqiangTcm.msun.test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.zaoqiangTcm.msun.config.ZaoqiangTcmMsunEnvProfile;
import com.scminterface.customer.zaoqiangTcm.msun.support.MsunSignedHttpClient;
import java.util.HashMap;
import java.util.Map;

/**
 * 众阳 OpenAPI 直连调用（不启动 Spring），供 main / 手工联调使用。
 */
public final class ZaoqiangTcmMsunOpenApiRunner
{
    public static final String DEPTS_PATH = "/msun-middle-base-common/v1/depts";
    public static final String IDENTITIES_PATH = "/msun-middle-base-common/v1/identities";

    private ZaoqiangTcmMsunOpenApiRunner()
    {
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

    public static String buildUrl(ZaoqiangTcmMsunEnvProfile env, String path)
    {
        String base = env.getBaseUrl();
        if (base.endsWith("/"))
        {
            base = base.substring(0, base.length() - 1);
        }
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    /**
     * 2.1.9 科室基本信息。
     */
    public static String fetchDepts(ZaoqiangTcmMsunEnvProfile env, Integer invalidFlag) throws Exception
    {
        Map<String, Object> params = new HashMap<>(2);
        if (invalidFlag != null)
        {
            params.put("invalidFlag", invalidFlag);
        }
        String url = buildUrl(env, DEPTS_PATH);
        System.out.println(">>> GET " + url + " params=" + params);
        return createClient(env).get(url, params);
    }

    /**
     * 2.1.12 用户身份信息。
     */
    public static String fetchIdentities(
            ZaoqiangTcmMsunEnvProfile env,
            String roleType,
            Long deptId,
            Long identityId,
            Long userId) throws Exception
    {
        Map<String, Object> params = new HashMap<>(4);
        if (roleType != null && !roleType.isEmpty())
        {
            params.put("roleType", roleType);
        }
        if (deptId != null)
        {
            params.put("deptId", deptId);
        }
        if (identityId != null)
        {
            params.put("identityId", identityId);
        }
        if (userId != null)
        {
            params.put("userId", userId);
        }
        String url = buildUrl(env, IDENTITIES_PATH);
        System.out.println(">>> GET " + url + " params=" + params);
        return createClient(env).get(url, params);
    }

    public static void printResponse(String title, String raw)
    {
        System.out.println();
        System.out.println("========== " + title + " ==========");
        try
        {
            Object parsed = JSON.parse(raw);
            System.out.println(JSON.toJSONString(parsed, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat));
            if (parsed instanceof JSONObject)
            {
                JSONObject body = (JSONObject) parsed;
                System.out.println("success=" + body.getBoolean("success")
                        + ", code=" + body.getString("code")
                        + ", message=" + body.getString("message"));
                JSONArray data = body.getJSONArray("data");
                if (data != null)
                {
                    System.out.println("data.size=" + data.size());
                    if (!data.isEmpty())
                    {
                        System.out.println("data[0]=" + data.getJSONObject(0).toJSONString());
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("(非 JSON，原样输出)");
            System.out.println(raw);
        }
        System.out.println("========================================");
    }

    public static Long extractFirstDeptId(String deptsRaw)
    {
        JSONObject body = JSON.parseObject(deptsRaw);
        if (!Boolean.TRUE.equals(body.getBoolean("success")))
        {
            throw new IllegalStateException("科室接口失败: " + body.getString("message"));
        }
        JSONArray data = body.getJSONArray("data");
        if (data == null || data.isEmpty())
        {
            throw new IllegalStateException("科室 data 为空");
        }
        return data.getJSONObject(0).getLong("deptId");
    }
}
