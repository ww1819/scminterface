package com.scminterface.customer.zaoqiangTcm.msun.service;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.zaoqiangTcm.ZaoqiangTcmTenantConstants;
import com.scminterface.customer.zaoqiangTcm.msun.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.zaoqiangTcm.msun.spd.ZaoqiangTcmMsunSpdApiPaths;
import com.scminterface.customer.zaoqiangTcm.msun.support.MsunOpenApiSupport;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 枣强中医院 SPD 查询类众阳接口探针（原样回参，不落库）。
 */
@Service
@ConditionalOnProperty(prefix = "scminterface.customer.zaoqiang-tcm-001.msun", name = "enabled", havingValue = "true")
public class ZaoqiangTcmMsunSpdQueryService
{
    private static final Logger log = LoggerFactory.getLogger(ZaoqiangTcmMsunSpdQueryService.class);

    private final ZaoqiangTcmMsunProperties properties;

    public ZaoqiangTcmMsunSpdQueryService(ZaoqiangTcmMsunProperties properties)
    {
        this.properties = properties;
    }

    /** 2.5.44 药品、材料字典查询 */
    public JSONObject queryDrugDictInfos(
            String drugCode,
            Long drugId,
            String drugName,
            String startTime,
            String endTime,
            Integer limitCount,
            Integer materialOrDrug,
            String specialFlag,
            String invalidFlag,
            Long hospitalId,
            Long orgId) throws Exception
    {
        Map<String, Object> params = new HashMap<>(12);
        putIfPresent(params, "drugCode", drugCode);
        putIfPresent(params, "drugId", drugId);
        putIfPresent(params, "drugName", drugName);
        putIfPresent(params, "startTime", startTime);
        putIfPresent(params, "endTime", endTime);
        putIfPresent(params, "limitCount", limitCount);
        putIfPresent(params, "materialOrDrug", materialOrDrug);
        putIfPresent(params, "specialFlag", specialFlag);
        putIfPresent(params, "invalidFlag", invalidFlag);
        putIfPresent(params, "hospitalId", hospitalId != null ? hospitalId : parseLong(properties.getHospitalId()));
        putIfPresent(params, "orgId", orgId != null ? orgId : parseLong(properties.getOrgId()));
        return invokeGet("2.5.44", ZaoqiangTcmMsunSpdApiPaths.DRUG_DICT_INFOS, params);
    }

    /** 2.5.58 SPD 药品材料分类字典 */
    public JSONObject queryDictCategory(String keyWord, Integer limitCount) throws Exception
    {
        Map<String, Object> params = new HashMap<>(2);
        putIfPresent(params, "keyWord", keyWord);
        putIfPresent(params, "limitCount", limitCount);
        return invokeGet("2.5.58", ZaoqiangTcmMsunSpdApiPaths.DICT_CATEGORY, params);
    }

    /** 2.5.62 SPD 供应商查询 */
    public JSONObject queryDrugSuppliers(
            String keyWord,
            Integer limitCount,
            String materialOrDrug,
            Long hospitalId,
            Long orgId) throws Exception
    {
        Map<String, Object> params = new HashMap<>(5);
        putIfPresent(params, "keyWord", keyWord);
        putIfPresent(params, "limitCount", limitCount);
        putIfPresent(params, "materialOrDrug", materialOrDrug);
        putIfPresent(params, "hospitalId", hospitalId != null ? hospitalId : parseLong(properties.getHospitalId()));
        putIfPresent(params, "orgId", orgId != null ? orgId : parseLong(properties.getOrgId()));
        return invokeGet("2.5.62", ZaoqiangTcmMsunSpdApiPaths.DRUG_SUPPLIERES, params);
    }

    /** 2.5.63 SPD 生产厂商查询 */
    public JSONObject queryDrugProducers(
            String keyWord,
            Integer limitCount,
            String materialOrDrug,
            Long hospitalId,
            Long orgId) throws Exception
    {
        Map<String, Object> params = new HashMap<>(5);
        putIfPresent(params, "keyWord", keyWord);
        putIfPresent(params, "limitCount", limitCount);
        putIfPresent(params, "materialOrDrug", materialOrDrug);
        putIfPresent(params, "hospitalId", hospitalId != null ? hospitalId : parseLong(properties.getHospitalId()));
        putIfPresent(params, "orgId", orgId != null ? orgId : parseLong(properties.getOrgId()));
        return invokeGet("2.5.63", ZaoqiangTcmMsunSpdApiPaths.DRUG_PRODUCERES, params);
    }

    /** 2.5.43 药房批次库存（deptId/drugId/drugSpecPackingId 必填） */
    public JSONObject queryDrugBatchStocks(Long deptId, Long drugId, Long drugSpecPackingId) throws Exception
    {
        assertRequired("deptId", deptId);
        assertRequired("drugId", drugId);
        assertRequired("drugSpecPackingId", drugSpecPackingId);
        Map<String, Object> params = new HashMap<>(3);
        params.put("deptId", deptId);
        params.put("drugId", drugId);
        params.put("drugSpecPackingId", drugSpecPackingId);
        return invokeGet("2.5.43", ZaoqiangTcmMsunSpdApiPaths.DRUG_BATCH_STOCKS, params);
    }

    /** 2.5.102 一级库入库/退库记录（startTime/endTime 必填） */
    public JSONObject queryYkInstock(
            Long deptId,
            String startTime,
            String endTime,
            String instockCode,
            String type) throws Exception
    {
        if (StringUtils.isEmpty(startTime) || StringUtils.isEmpty(endTime))
        {
            throw new IllegalArgumentException("2.5.102 要求 startTime、endTime 必填，格式 yyyy-MM-dd HH:mm:ss");
        }
        Map<String, Object> body = new HashMap<>(5);
        putIfPresent(body, "deptId", deptId);
        body.put("startTime", startTime);
        body.put("endTime", endTime);
        putIfPresent(body, "instockCode", instockCode);
        putIfPresent(body, "type", type);
        return invokePost("2.5.102", ZaoqiangTcmMsunSpdApiPaths.QUERY_YK_INSTOCK, body);
    }

    private JSONObject invokeGet(String apiNo, String path, Map<String, Object> params) throws Exception
    {
        String url = MsunOpenApiSupport.buildUrl(properties, path);
        String raw = MsunOpenApiSupport.createClient(properties).get(url, params);
        log.info("租户 {} SPD查询 {} [{}] env={} url={}",
                ZaoqiangTcmTenantConstants.TENANT_ID, apiNo,
                properties.activeProfile().getLabel(), properties.getActiveEnv(), url);
        return MsunOpenApiSupport.wrapRawResponse(raw, params);
    }

    private JSONObject invokePost(String apiNo, String path, Map<String, Object> body) throws Exception
    {
        String url = MsunOpenApiSupport.buildUrl(properties, path);
        String raw = MsunOpenApiSupport.createClient(properties).post(url, body);
        log.info("租户 {} SPD查询 {} [{}] env={} url={}",
                ZaoqiangTcmTenantConstants.TENANT_ID, apiNo,
                properties.activeProfile().getLabel(), properties.getActiveEnv(), url);
        return MsunOpenApiSupport.wrapRawResponse(raw, body);
    }

    private static void putIfPresent(Map<String, Object> params, String key, Object value)
    {
        if (value != null && (!(value instanceof String) || StringUtils.isNotEmpty((String) value)))
        {
            params.put(key, value);
        }
    }

    private static Long parseLong(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }
        return Long.valueOf(value);
    }

    private static void assertRequired(String name, Object value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("2.5.43 要求 " + name + " 必填");
        }
    }
}
