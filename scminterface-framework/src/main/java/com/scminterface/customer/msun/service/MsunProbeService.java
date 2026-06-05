package com.scminterface.customer.msun.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.support.MsunOpenApiSupport;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    /** 2.1.12 人员身份：按 roleType 全量拉取时的默认角色类型（0 管理员 … 8 医技） */
    private static final String[] DEFAULT_IDENTITY_ROLE_TYPES = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8"
    };

    private static final long IDENTITY_ROLE_SWEEP_DELAY_MS = 300L;

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

    /**
     * 2.1.12 获取全部用户：按 roleType 0~8 依次查询 HIS 并合并 data 数组。
     */
    public JSONObject fetchIdentitiesAllRoleTypes(MsunHospitalRuntime runtime) throws Exception
    {
        JSONArray allItems = new JSONArray();
        JSONArray roleStats = new JSONArray();
        int failCount = 0;

        for (int i = 0; i < DEFAULT_IDENTITY_ROLE_TYPES.length; i++)
        {
            String roleType = DEFAULT_IDENTITY_ROLE_TYPES[i];
            try
            {
                JSONObject page = fetchIdentities(runtime, roleType, null, null, null);
                JSONObject hisBody = page.getJSONObject("hisBody");
                if (hisBody == null)
                {
                    failCount++;
                    roleStats.add(buildIdentityRoleStat(roleType, false, 0, "无 hisBody"));
                }
                else if (!Boolean.TRUE.equals(hisBody.getBoolean("success")))
                {
                    failCount++;
                    roleStats.add(buildIdentityRoleStat(roleType, false, 0, hisBody.getString("message")));
                }
                else
                {
                    JSONArray items = hisBody.getJSONArray("data");
                    int rows = items == null ? 0 : items.size();
                    if (items != null)
                    {
                        allItems.addAll(items);
                    }
                    roleStats.add(buildIdentityRoleStat(roleType, true, rows, hisBody.getString("message")));
                }
            }
            catch (Exception ex)
            {
                failCount++;
                roleStats.add(buildIdentityRoleStat(roleType, false, 0, ex.getMessage()));
                log.warn("众阳HIS 医院 {} 人员身份全量 roleType={} 失败: {}",
                        runtime.getHospitalKey(), roleType, ex.getMessage());
            }

            if (i < DEFAULT_IDENTITY_ROLE_TYPES.length - 1)
            {
                Thread.sleep(IDENTITY_ROLE_SWEEP_DELAY_MS);
            }
        }

        JSONObject mergedBody = new JSONObject();
        mergedBody.put("success", failCount < DEFAULT_IDENTITY_ROLE_TYPES.length);
        mergedBody.put("code", failCount > 0 ? "PARTIAL" : "0000");
        mergedBody.put("message", failCount > 0
                ? "部分 roleType 失败 " + failCount + " 次，已合并成功部分"
                : "成功");
        mergedBody.put("data", allItems);

        JSONObject probeMerged = new JSONObject();
        probeMerged.put("mode", "allRoleTypes");
        probeMerged.put("roleTypes", DEFAULT_IDENTITY_ROLE_TYPES.length);
        probeMerged.put("totalRows", allItems.size());
        probeMerged.put("roleStats", roleStats);
        probeMerged.put("failCount", failCount);
        mergedBody.put("_probeMerged", probeMerged);

        Map<String, Object> requestParams = new LinkedHashMap<>(4);
        requestParams.put("mode", "allRoleTypes");
        requestParams.put("roleTypes", DEFAULT_IDENTITY_ROLE_TYPES);

        JSONObject result = new JSONObject();
        result.put("hisBody", mergedBody);
        result.put("requestParams", requestParams);

        log.info("众阳HIS 医院 {} 人员身份全量 roleType 遍历完成 env={} totalRows={} failCount={}",
                runtime.getHospitalKey(),
                runtime.getActiveEnv(),
                allItems.size(),
                failCount);
        return result;
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

    private static JSONObject buildIdentityRoleStat(String roleType, boolean ok, int rows, String message)
    {
        JSONObject stat = new JSONObject();
        stat.put("roleType", roleType);
        stat.put("ok", ok);
        stat.put("rows", rows);
        stat.put("message", message);
        return stat;
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
