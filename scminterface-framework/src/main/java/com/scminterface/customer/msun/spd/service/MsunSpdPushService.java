package com.scminterface.customer.msun.spd.service;

import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.service.MsunHisPushLogService;
import com.scminterface.customer.msun.spd.MsunSpdApiPaths;
import com.scminterface.customer.msun.support.MsunHisInvokeDebugSupport;
import com.scminterface.customer.msun.support.MsunOpenApiSupport;
import com.scminterface.customer.msun.support.MsunSignedHttpResult;
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
        Map<String, Object> debug = MsunHisInvokeDebugSupport.buildDebug(runtime, apiNo, path, http, wrapped);
        return new MsunSpdPushInvokeResult(wrapped, debug);
    }
}
