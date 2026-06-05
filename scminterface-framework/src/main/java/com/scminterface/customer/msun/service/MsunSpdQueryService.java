package com.scminterface.customer.msun.service;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.spd.MsunSpdApiPaths;
import com.scminterface.customer.msun.support.MsunOpenApiSupport;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 众阳 SPD 查询类接口探针（各医院客户共用逻辑，连接信息由 {@link MsunHospitalRuntime} 注入）。
 */
@Service
public class MsunSpdQueryService
{
    private static final Logger log = LoggerFactory.getLogger(MsunSpdQueryService.class);

    public JSONObject queryDrugDictInfos(
            MsunHospitalRuntime runtime,
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
        putIfPresent(params, "hospitalId", hospitalId != null ? hospitalId : parseLong(runtime.getHospitalId()));
        putIfPresent(params, "orgId", orgId != null ? orgId : parseLong(runtime.getOrgId()));
        return invokeGet(runtime, "2.5.44", MsunSpdApiPaths.DRUG_DICT_INFOS, params);
    }

    public JSONObject queryDictCategory(
            MsunHospitalRuntime runtime,
            String keyWord,
            Integer limitCount,
            Long hisDictId) throws Exception
    {
        Map<String, Object> params = new HashMap<>(3);
        putIfPresent(params, "keyWord", keyWord);
        putIfPresent(params, "limitCount", limitCount);
        putIfPresent(params, "hisDictId", hisDictId);
        return invokeGet(runtime, "2.5.58", MsunSpdApiPaths.DICT_CATEGORY, params);
    }

    public JSONObject queryDrugSuppliers(
            MsunHospitalRuntime runtime,
            String keyWord,
            Integer limitCount,
            String materialOrDrug,
            Long hospitalId,
            Long orgId,
            Long supplierId) throws Exception
    {
        Map<String, Object> params = new HashMap<>(6);
        putIfPresent(params, "keyWord", keyWord);
        putIfPresent(params, "limitCount", limitCount);
        putIfPresent(params, "materialOrDrug", materialOrDrug);
        putIfPresent(params, "hospitalId", hospitalId != null ? hospitalId : parseLong(runtime.getHospitalId()));
        putIfPresent(params, "orgId", orgId != null ? orgId : parseLong(runtime.getOrgId()));
        putIfPresent(params, "supplierId", supplierId);
        return invokeGet(runtime, "2.5.62", MsunSpdApiPaths.DRUG_SUPPLIERES, params);
    }

    public JSONObject queryDrugProducers(
            MsunHospitalRuntime runtime,
            String keyWord,
            Integer limitCount,
            String materialOrDrug,
            Long hospitalId,
            Long orgId,
            Long producerId) throws Exception
    {
        Map<String, Object> params = new HashMap<>(6);
        putIfPresent(params, "keyWord", keyWord);
        putIfPresent(params, "limitCount", limitCount);
        putIfPresent(params, "materialOrDrug", materialOrDrug);
        putIfPresent(params, "hospitalId", hospitalId != null ? hospitalId : parseLong(runtime.getHospitalId()));
        putIfPresent(params, "orgId", orgId != null ? orgId : parseLong(runtime.getOrgId()));
        putIfPresent(params, "producerId", producerId);
        return invokeGet(runtime, "2.5.63", MsunSpdApiPaths.DRUG_PRODUCERES, params);
    }

    public JSONObject queryMergeStockInfos(
            MsunHospitalRuntime runtime,
            Long deptId,
            String categoryIdList,
            String drugCode,
            Long drugId,
            String drugName,
            Long drugSpecPackingId,
            String zeroFlag,
            Long maxId) throws Exception
    {
        assertRequired("deptId", deptId);
        Map<String, Object> params = new HashMap<>(8);
        params.put("deptId", deptId);
        putIfPresent(params, "drugCode", drugCode);
        putIfPresent(params, "drugId", drugId);
        putIfPresent(params, "drugName", drugName);
        putIfPresent(params, "drugSpecPackingId", drugSpecPackingId);
        putIfPresent(params, "zeroFlag", zeroFlag);
        putIfPresent(params, "maxId", maxId);
        putCategoryIdList(params, categoryIdList);
        return invokeGet(runtime, "2.5.82", MsunSpdApiPaths.MERGE_STOCK_INFOS, params);
    }

    public JSONObject queryDrugBatchStocks(
            MsunHospitalRuntime runtime,
            Long deptId,
            Long drugId,
            Long drugSpecPackingId) throws Exception
    {
        assertRequired("deptId", deptId);
        assertRequired("drugId", drugId);
        assertRequired("drugSpecPackingId", drugSpecPackingId);
        Map<String, Object> params = new HashMap<>(3);
        params.put("deptId", deptId);
        params.put("drugId", drugId);
        params.put("drugSpecPackingId", drugSpecPackingId);
        return invokeGet(runtime, "2.5.43", MsunSpdApiPaths.DRUG_BATCH_STOCKS, params);
    }

    public JSONObject queryYkInstock(
            MsunHospitalRuntime runtime,
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
        return invokePost(runtime, "2.5.102", MsunSpdApiPaths.QUERY_YK_INSTOCK, body);
    }

    private JSONObject invokeGet(
            MsunHospitalRuntime runtime,
            String apiNo,
            String path,
            Map<String, Object> params) throws Exception
    {
        String url = MsunOpenApiSupport.buildUrl(runtime, path);
        String raw = MsunOpenApiSupport.createClient(runtime).get(url, params);
        log.info("众阳HIS 医院 {} SPD查询 {} [{}] env={} url={}",
                runtime.getHospitalKey(), apiNo,
                runtime.getActiveEnvLabel(), runtime.getActiveEnv(), url);
        return MsunOpenApiSupport.wrapRawResponse(raw, params);
    }

    private JSONObject invokePost(
            MsunHospitalRuntime runtime,
            String apiNo,
            String path,
            Map<String, Object> body) throws Exception
    {
        String url = MsunOpenApiSupport.buildUrl(runtime, path);
        String raw = MsunOpenApiSupport.createClient(runtime).post(url, body);
        log.info("众阳HIS 医院 {} SPD查询 {} [{}] env={} url={}",
                runtime.getHospitalKey(), apiNo,
                runtime.getActiveEnvLabel(), runtime.getActiveEnv(), url);
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

    private static void putCategoryIdList(Map<String, Object> params, String categoryIdList)
    {
        if (StringUtils.isEmpty(categoryIdList))
        {
            return;
        }
        String[] parts = categoryIdList.split(",");
        StringBuilder sb = new StringBuilder();
        for (String part : parts)
        {
            String trimmed = part.trim();
            if (!trimmed.isEmpty())
            {
                if (sb.length() > 0)
                {
                    sb.append(',');
                }
                sb.append(trimmed);
            }
        }
        if (sb.length() > 0)
        {
            params.put("categoryIdList", sb.toString());
        }
    }

    private static void assertRequired(String name, Object value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException(name + " 必填");
        }
    }
}
