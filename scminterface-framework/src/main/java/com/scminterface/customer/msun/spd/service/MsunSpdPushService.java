package com.scminterface.customer.msun.spd.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.service.MsunHisPushLogService;
import com.scminterface.customer.msun.spd.MsunSpdApiPaths;
import com.scminterface.customer.msun.support.MsunOpenApiSupport;
import com.scminterface.customer.msun.support.MsunSignedHttpResult;
import com.scminterface.common.utils.StringUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 众阳 SPD 写库推送（2.5.41 入库 / 2.5.42 退库）。
 */
@Service
public class MsunSpdPushService
{
    private static final Logger log = LoggerFactory.getLogger(MsunSpdPushService.class);

    private final MsunHisPushLogService pushLogService;

    public MsunSpdPushService(MsunHisPushLogService pushLogService)
    {
        this.pushLogService = pushLogService;
    }

    public MsunSpdPushInvokeResult pushDrugStocksNew(
            MsunHospitalRuntime runtime, Map<String, Object> body, Map<String, Object> logMeta) throws Exception
    {
        return invokePost(runtime, "2.5.41", MsunSpdApiPaths.DRUG_STOCKS_NEW, body, logMeta);
    }

    public MsunSpdPushInvokeResult pushDrugStocksReturn(
            MsunHospitalRuntime runtime, Map<String, Object> body, Map<String, Object> logMeta) throws Exception
    {
        return invokePost(runtime, "2.5.42", MsunSpdApiPaths.DRUG_STOCKS_NEW_RETURN, body, logMeta);
    }

    private MsunSpdPushInvokeResult invokePost(
            MsunHospitalRuntime runtime,
            String apiNo,
            String path,
            Map<String, Object> body,
            Map<String, Object> logMeta) throws Exception
    {
        String url = MsunOpenApiSupport.buildUrl(runtime, path);
        MsunSignedHttpResult http = MsunOpenApiSupport.createClient(runtime).postWithDebug(url, body);
        log.info("众阳HIS 医院 {} SPD推送 {} [{}] env={} url={} status={}",
                runtime.getHospitalKey(), apiNo, runtime.getActiveEnvLabel(), runtime.getActiveEnv(),
                url, http.getHttpStatus());
        JSONObject wrapped = MsunOpenApiSupport.wrapRawResponse(http.getResponseBody(), body);
        pushLogService.savePushLog(runtime, apiNo, body, wrapped, logMeta);
        Map<String, Object> debug = buildDebug(runtime, apiNo, path, http, wrapped);
        return new MsunSpdPushInvokeResult(wrapped, debug);
    }

    private static Map<String, Object> buildDebug(
            MsunHospitalRuntime runtime,
            String apiNo,
            String path,
            MsunSignedHttpResult http,
            JSONObject wrapped)
    {
        Map<String, Object> debug = new LinkedHashMap<>(16);
        debug.put("apiCode", apiNo);
        debug.put("apiPath", path);
        debug.put("method", http.getMethod());
        debug.put("url", http.getUrl());
        debug.put("httpStatus", http.getHttpStatus());
        debug.put("activeEnv", runtime.getActiveEnv());
        debug.put("baseUrl", runtime.getBaseUrl());
        debug.put("hospitalId", runtime.getHospitalId());
        debug.put("orgId", runtime.getOrgId());
        debug.put("appId", runtime.getAppId());
        debug.put("requestHeaders", http.getRequestHeaders());
        debug.put("requestBody", parseRequestBodyForDebug(http.getRequestBody()));
        debug.put("responseRaw", parseJsonOrRaw(http.getResponseBody()));
        if (wrapped != null)
        {
            debug.put("hisBody", wrapped.get("hisBody"));
        }
        return debug;
    }

    private static Object parseRequestBodyForDebug(String json)
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

    private static Object parseJsonOrRaw(String raw)
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
