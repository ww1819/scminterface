package com.scminterface.customer.msun.mirror.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorRowSupport;
import com.scminterface.framework.util.ZsUuid7;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * HIS 推送日志落库 m_his_push_log。
 */
@Service
public class MsunHisPushLogService
{
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
        row.put("request_json", requestBody == null ? null : JSON.toJSONString(requestBody));
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
            row.put("his_trace_id", hisBody.getString("traceId"));
            boolean ok = Boolean.TRUE.equals(hisBody.getBoolean("success"));
            row.put("push_status", ok ? "成功" : "失败");
            if (!ok)
            {
                row.put("push_msg", hisBody.getString("message"));
            }
        }
        else
        {
            row.put("push_status", "失败");
            row.put("push_msg", "HIS响应无法解析");
        }

        schemaService.ensureTable("m_his_push_log");
        pushLogExecutor.insert(row);
    }

    private static void putIfPresent(Map<String, Object> row, String col, Object val)
    {
        if (val != null && MsunHisMirrorRowSupport.isValidColumn(col))
        {
            row.put(col, String.valueOf(val));
        }
    }
}
