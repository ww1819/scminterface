package com.scminterface.customer.msun.service;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.support.MsunOpenApiSupport;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 众阳科室/人员身份探针（各医院客户共用逻辑，连接信息由 {@link MsunHospitalRuntime} 注入）。
 */
@Service
public class MsunProbeService
{
    private static final Logger log = LoggerFactory.getLogger(MsunProbeService.class);

    public JSONObject fetchDepts(
            MsunHospitalRuntime runtime,
            Long hospitalAreaId,
            Integer invalidFlag,
            Long deptId,
            String deptName) throws Exception
    {
        Map<String, Object> params = new HashMap<>(4);
        putIfPresent(params, "hospitalAreaId", hospitalAreaId);
        putIfPresent(params, "invalidFlag", invalidFlag);
        putIfPresent(params, "deptId", deptId);
        putIfPresent(params, "deptName", deptName);

        String raw = MsunOpenApiSupport.createClient(runtime).get(runtime.deptsUrl(), params);
        log.info("众阳HIS 医院 {} 科室探针 [{}] env={} url={}",
                runtime.getHospitalKey(),
                runtime.getActiveEnvLabel(),
                runtime.getActiveEnv(),
                runtime.deptsUrl());
        return MsunOpenApiSupport.wrapRawResponse(raw, params);
    }

    public JSONObject fetchIdentities(
            MsunHospitalRuntime runtime,
            String roleType,
            Long deptId,
            Long identityId,
            Long userId) throws Exception
    {
        assertIdentityQuery(roleType, deptId, identityId, userId);

        Map<String, Object> params = new HashMap<>(4);
        putIfPresent(params, "roleType", roleType);
        putIfPresent(params, "deptId", deptId);
        putIfPresent(params, "identityId", identityId);
        putIfPresent(params, "userId", userId);

        String raw = MsunOpenApiSupport.createClient(runtime).get(runtime.identitiesUrl(), params);
        log.info("众阳HIS 医院 {} 人员身份探针 [{}] env={} url={}",
                runtime.getHospitalKey(),
                runtime.getActiveEnvLabel(),
                runtime.getActiveEnv(),
                runtime.identitiesUrl());
        return MsunOpenApiSupport.wrapRawResponse(raw, params);
    }

    public JSONObject fetchIdentitiesByFirstDept(MsunHospitalRuntime runtime, String roleType) throws Exception
    {
        JSONObject deptResult = fetchDepts(runtime, null, -1, null, null);
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
        return fetchIdentities(runtime, roleType, firstDeptId, null, null);
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
