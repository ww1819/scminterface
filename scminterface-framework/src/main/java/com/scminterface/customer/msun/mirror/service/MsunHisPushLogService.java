package com.scminterface.customer.msun.mirror.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.support.MsunHisJsonSupport;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorPushLogRowSupport;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorRowSupport;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorTableNames;
import com.scminterface.framework.util.ZsUuid7;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * HIS 推送日志落库 {@link com.scminterface.customer.msun.mirror.support.MsunHisMirrorTableNames#PUSH_LOG}。
 */
@Service
public class MsunHisPushLogService
{
    private static final Logger log = LoggerFactory.getLogger(MsunHisPushLogService.class);

    private static final int PUSH_MSG_MAX_LEN = 480;
    private static final int HIS_TRACE_ID_MAX_LEN = 64;
    private static final int PUSH_STATUS_MAX_LEN = 16;

    private final MsunHisMirrorSchemaService schemaService;
    private final MsunHisPushLogExecutor pushLogExecutor;

    public MsunHisPushLogService(MsunHisMirrorSchemaService schemaService, MsunHisPushLogExecutor pushLogExecutor)
    {
        this.schemaService = schemaService;
        this.pushLogExecutor = pushLogExecutor;
    }

    public void savePushLog(
            MsunHospitalRuntime runtime,
            String apiCode,
            Map<String, Object> requestBody,
            JSONObject wrappedResponse,
            Map<String, Object> meta)
    {
        Map<String, Object> row = new LinkedHashMap<>(20);
        row.put("log_id", ZsUuid7.newString());
        row.put("hospital_key", runtime.getHospitalKey());
        row.put("tenant_id", runtime.getTenantId());
        row.put("active_env", runtime.getActiveEnv());
        row.put("api_code", apiCode);
        row.put("request_json", requestBody == null ? null : MsunHisJsonSupport.toRequestJson(requestBody));
        row.put("response_json", wrappedResponse == null ? null : wrappedResponse.toJSONString());
        row.put("insert_time", new Date());

        if (meta != null)
        {
            putIfPresent(row, "spd_bill_id", meta.get("spdBillId"));
            putIfPresent(row, "spd_entry_id", meta.get("spdEntryId"));
            putIfPresent(row, "bill_no", meta.get("billNo"));
            putIfPresent(row, "bill_type", meta.get("billType"));
        }

        Object hisBodyObj = wrappedResponse != null ? wrappedResponse.get("hisBody") : null;
        if (hisBodyObj instanceof JSONObject)
        {
            JSONObject hisBody = (JSONObject) hisBodyObj;
            row.put("his_trace_id", MsunHisMirrorRowSupport.clampVarchar(hisBody.getString("traceId"), HIS_TRACE_ID_MAX_LEN));
            boolean ok = Boolean.TRUE.equals(hisBody.getBoolean("success"));
            row.put("push_status", MsunHisMirrorRowSupport.clampVarchar(ok ? "成功" : "失败", PUSH_STATUS_MAX_LEN));
            if (!ok)
            {
                String msg = MsunHisMirrorPushLogRowSupport.extractHisMessage(hisBody.get("message"));
                if (msg == null)
                {
                    msg = MsunHisMirrorPushLogRowSupport.extractHisMessage(hisBody.get("msg"));
                }
                row.put("push_msg", MsunHisMirrorRowSupport.clampVarchar(msg, PUSH_MSG_MAX_LEN));
            }
        }
        else
        {
            row.put("push_status", "失败");
            row.put("push_msg", "HIS响应无法解析");
        }

        schemaService.ensureTable(MsunHisMirrorTableNames.PUSH_LOG);
        try
        {
            pushLogExecutor.insert(row);
        }
        catch (Exception ex)
        {
            // 推送日志落库失败不应阻断 HIS 推送主流程；完整报文见 request_json/response_json
            log.warn("HIS推送日志落库失败 billNo={} api={} err={}",
                    row.get("bill_no"), apiCode, ex.getMessage());
        }
    }

    private static void putIfPresent(Map<String, Object> row, String col, Object val)
    {
        if (val != null && MsunHisMirrorRowSupport.isValidColumn(col))
        {
            row.put(col, String.valueOf(val));
        }
    }
}
