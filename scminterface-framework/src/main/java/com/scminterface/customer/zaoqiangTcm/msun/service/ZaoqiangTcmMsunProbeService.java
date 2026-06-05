package com.scminterface.customer.zaoqiangTcm.msun.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.zaoqiangTcm.ZaoqiangTcmTenantConstants;
import com.scminterface.customer.zaoqiangTcm.msun.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.zaoqiangTcm.msun.support.MsunSignedHttpClient;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 调用众阳科室/人员身份接口，原样返回 HIS 响应（不落库）。
 */
@Service
@ConditionalOnProperty(prefix = "scminterface.customer.zaoqiang-tcm-001.msun", name = "enabled", havingValue = "true")
public class ZaoqiangTcmMsunProbeService
{
    private static final Logger log = LoggerFactory.getLogger(ZaoqiangTcmMsunProbeService.class);

    private final ZaoqiangTcmMsunProperties properties;

    public ZaoqiangTcmMsunProbeService(ZaoqiangTcmMsunProperties properties)
    {
        this.properties = properties;
    }

    public JSONObject fetchDepts(Long hospitalAreaId, Integer invalidFlag, Long deptId, String deptName) throws Exception
    {
        Map<String, Object> params = new HashMap<>(4);
        putIfPresent(params, "hospitalAreaId", hospitalAreaId);
        putIfPresent(params, "invalidFlag", invalidFlag);
        putIfPresent(params, "deptId", deptId);
        putIfPresent(params, "deptName", deptName);

        String raw = createClient().get(properties.deptsUrl(), params);
        log.info("租户 {} 科室探针 [{}] env={} url={}",
                ZaoqiangTcmTenantConstants.TENANT_ID,
                properties.activeProfile().getLabel(),
                properties.getActiveEnv(),
                properties.deptsUrl());
        return wrapRawResponse(raw, params);
    }

    public JSONObject fetchIdentities(String roleType, Long deptId, Long identityId, Long userId) throws Exception
    {
        assertIdentityQuery(roleType, deptId, identityId, userId);

        Map<String, Object> params = new HashMap<>(4);
        putIfPresent(params, "roleType", roleType);
        putIfPresent(params, "deptId", deptId);
        putIfPresent(params, "identityId", identityId);
        putIfPresent(params, "userId", userId);

        String raw = createClient().get(properties.identitiesUrl(), params);
        log.info("租户 {} 人员身份探针 [{}] env={} url={}",
                ZaoqiangTcmTenantConstants.TENANT_ID,
                properties.activeProfile().getLabel(),
                properties.getActiveEnv(),
                properties.identitiesUrl());
        return wrapRawResponse(raw, params);
    }

    /**
     * 先拉科室，再按首个科室 ID 拉人员身份（便于无参快速查看回参结构）。
     */
    public JSONObject fetchIdentitiesByFirstDept(String roleType) throws Exception
    {
        JSONObject deptResult = fetchDepts(null, -1, null, null);
        Object data = deptResult.get("hisBody");
        if (!(data instanceof JSONObject))
        {
            throw new IllegalStateException("科室接口未返回可解析的 JSON 对象");
        }
        JSONObject body = (JSONObject) data;
        if (!Boolean.TRUE.equals(body.getBoolean("success")))
        {
            throw new IllegalStateException("科室接口调用失败: " + body.getString("message"));
        }
        com.alibaba.fastjson2.JSONArray list = body.getJSONArray("data");
        if (list == null || list.isEmpty())
        {
            throw new IllegalStateException("科室列表为空，无法自动探测人员身份");
        }
        Long firstDeptId = list.getJSONObject(0).getLong("deptId");
        return fetchIdentities(roleType, firstDeptId, null, null);
    }

    private MsunSignedHttpClient createClient()
    {
        return new MsunSignedHttpClient(
                properties.getAppId(),
                properties.getAppSecret(),
                properties.getHospitalId(),
                properties.getOrgId(),
                properties.getLoginUser());
    }

    private static JSONObject wrapRawResponse(String raw, Map<String, Object> requestParams)
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

    private static void putIfPresent(Map<String, Object> params, String key, Object value)
    {
        if (value != null && (!(value instanceof String) || StringUtils.isNotEmpty((String) value)))
        {
            params.put(key, value);
        }
    }

    private static void assertIdentityQuery(String roleType, Long deptId, Long identityId, Long userId)
    {
        boolean hasRole = StringUtils.isNotEmpty(roleType);
        boolean hasDept = deptId != null;
        boolean hasIdentity = identityId != null;
        boolean hasUser = userId != null;
        if (!hasRole && !hasDept && !hasIdentity && !hasUser)
        {
            throw new IllegalArgumentException(
                    "人员身份接口要求 roleType、deptId、identityId、userId 至少传一个；"
                            + "可调用 /identities/sample 自动取首个科室探测");
        }
    }
}
