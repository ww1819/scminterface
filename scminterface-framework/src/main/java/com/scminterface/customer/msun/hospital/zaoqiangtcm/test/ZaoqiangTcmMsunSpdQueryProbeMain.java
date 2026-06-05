package com.scminterface.customer.msun.hospital.zaoqiangtcm.test;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunEnvProfile;
import com.scminterface.customer.msun.spd.MsunSpdApiPaths;
import com.scminterface.customer.msun.support.MsunOpenApiSupport;
import com.scminterface.customer.msun.support.MsunSignedHttpClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 众阳 HIS — 枣强县中医院 SPD 查询联调入口（接口文档2）。
 */
public class ZaoqiangTcmMsunSpdQueryProbeMain
{
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception
    {
        ZaoqiangTcmMsunEnvProfile env = resolveEnv(args);
        printEnvBanner(env);
        MsunSignedHttpClient client = ZaoqiangTcmMsunOpenApiRunner.createClient(env);

        Map<String, Object> dictParams = new HashMap<>(4);
        dictParams.put("limitCount", 5);
        dictParams.put("materialOrDrug", 0);
        dictParams.put("invalidFlag", "0");
        dictParams.put("hospitalId", Long.valueOf(env.getHospitalId()));
        dictParams.put("orgId", Long.valueOf(env.getOrgId()));
        String dictRaw = invokeGet(env, client, MsunSpdApiPaths.DRUG_DICT_INFOS, dictParams);
        ZaoqiangTcmMsunOpenApiRunner.printResponse("2.5.44 药品材料字典 /v1/drug-dict-infos", dictRaw);

        Map<String, Object> catParams = new HashMap<>(2);
        catParams.put("keyWord", "西药");
        catParams.put("limitCount", 10);
        String catRaw = invokeGet(env, client, MsunSpdApiPaths.DICT_CATEGORY, catParams);
        ZaoqiangTcmMsunOpenApiRunner.printResponse("2.5.58 分类字典 /v1/dict-category", catRaw);

        Map<String, Object> supParams = new HashMap<>(4);
        supParams.put("limitCount", 5);
        supParams.put("materialOrDrug", "0");
        supParams.put("hospitalId", Long.valueOf(env.getHospitalId()));
        supParams.put("orgId", Long.valueOf(env.getOrgId()));
        String supRaw = invokeGet(env, client, MsunSpdApiPaths.DRUG_SUPPLIERES, supParams);
        ZaoqiangTcmMsunOpenApiRunner.printResponse("2.5.62 供应商 /v1/drug-supplieres", supRaw);

        Map<String, Object> prodParams = new HashMap<>(4);
        prodParams.put("limitCount", 5);
        prodParams.put("materialOrDrug", "0");
        prodParams.put("hospitalId", Long.valueOf(env.getHospitalId()));
        prodParams.put("orgId", Long.valueOf(env.getOrgId()));
        String prodRaw = invokeGet(env, client, MsunSpdApiPaths.DRUG_PRODUCERES, prodParams);
        ZaoqiangTcmMsunOpenApiRunner.printResponse("2.5.63 生产厂商 /v1/drug-produceres", prodRaw);

        tryProbeBatchStock(env, client, dictRaw);

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(7);
        Map<String, Object> ykBody = new HashMap<>(2);
        ykBody.put("startTime", start.format(DT));
        ykBody.put("endTime", end.format(DT));
        String ykRaw = invokePost(env, client, MsunSpdApiPaths.QUERY_YK_INSTOCK, ykBody);
        ZaoqiangTcmMsunOpenApiRunner.printResponse("2.5.102 一级库入退库 /v1/query-yk-instock", ykRaw);

        System.out.println();
        System.out.println("SPD 查询联调完成。推送接口 2.5.41/2.5.42 本阶段未调用（避免写库）。");
    }

    private static void tryProbeBatchStock(
            ZaoqiangTcmMsunEnvProfile env,
            MsunSignedHttpClient client,
            String dictRaw) throws Exception
    {
        Long drugId = null;
        Long drugSpecPackingId = null;
        JSONObject dictBody = JSONParseHelper.parseBody(dictRaw);
        if (dictBody != null)
        {
            JSONArray data = dictBody.getJSONArray("data");
            if (data != null && !data.isEmpty())
            {
                JSONObject first = data.getJSONObject(0);
                drugId = first.getLong("drugId");
                if (first.containsKey("drugSpecPackingId"))
                {
                    drugSpecPackingId = first.getLong("drugSpecPackingId");
                }
                else if (first.containsKey("specPackingList"))
                {
                    JSONArray packs = first.getJSONArray("specPackingList");
                    if (packs != null && !packs.isEmpty())
                    {
                        drugSpecPackingId = packs.getJSONObject(0).getLong("drugSpecPackingId");
                    }
                }
            }
        }

        Long deptId = null;
        try
        {
            String deptsRaw = ZaoqiangTcmMsunOpenApiRunner.fetchDepts(env, -1);
            deptId = ZaoqiangTcmMsunOpenApiRunner.extractFirstDeptId(deptsRaw);
        }
        catch (Exception ex)
        {
            System.out.println();
            System.out.println("2.5.43 跳过：无法从科室接口获取 deptId - " + ex.getMessage());
        }

        if (deptId == null || drugId == null || drugSpecPackingId == null)
        {
            System.out.println();
            System.out.println("2.5.43 跳过：缺少 deptId/drugId/drugSpecPackingId，"
                    + "请用 2.5.44 与 2.1.9 回参手工调用 /drug-batch-stocks");
            return;
        }

        Map<String, Object> stockParams = new HashMap<>(3);
        stockParams.put("deptId", deptId);
        stockParams.put("drugId", drugId);
        stockParams.put("drugSpecPackingId", drugSpecPackingId);
        String stockRaw = invokeGet(env, client, MsunSpdApiPaths.DRUG_BATCH_STOCKS, stockParams);
        ZaoqiangTcmMsunOpenApiRunner.printResponse("2.5.43 药房批次库存 /v1/drug-batch-stocks", stockRaw);
    }

    private static String invokeGet(
            ZaoqiangTcmMsunEnvProfile env,
            MsunSignedHttpClient client,
            String path,
            Map<String, Object> params) throws Exception
    {
        String url = MsunOpenApiSupport.buildUrl(env.getBaseUrl(), path);
        System.out.println(">>> GET " + url + " params=" + params);
        return client.get(url, params);
    }

    private static String invokePost(
            ZaoqiangTcmMsunEnvProfile env,
            MsunSignedHttpClient client,
            String path,
            Map<String, Object> body) throws Exception
    {
        String url = MsunOpenApiSupport.buildUrl(env.getBaseUrl(), path);
        System.out.println(">>> POST " + url + " body=" + body);
        return client.post(url, body);
    }

    private static ZaoqiangTcmMsunEnvProfile resolveEnv(String[] args)
    {
        if (args != null && args.length > 0 && ZaoqiangTcmMsunEnvProfile.PROD.getCode().equalsIgnoreCase(args[0].trim()))
        {
            return ZaoqiangTcmMsunEnvProfile.PROD;
        }
        return ZaoqiangTcmMsunEnvProfile.TEST;
    }

    private static void printEnvBanner(ZaoqiangTcmMsunEnvProfile env)
    {
        System.out.println("众阳HIS / 医院: " + ZaoqiangTcmHospitalConstants.HOSPITAL_NAME
                + " | SPD查询联调 | 环境: " + env.getCode() + " (" + env.getLabel() + ")");
        System.out.println("baseUrl: " + env.getBaseUrl());
        System.out.println();
    }

    private static final class JSONParseHelper
    {
        private static JSONObject parseBody(String raw)
        {
            try
            {
                JSONObject root = com.alibaba.fastjson2.JSON.parseObject(raw);
                if (root == null || !Boolean.TRUE.equals(root.getBoolean("success")))
                {
                    return null;
                }
                return root;
            }
            catch (Exception ex)
            {
                return null;
            }
        }
    }
}
