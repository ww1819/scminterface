package com.scminterface.customer.msun.mirror.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.mapper.MsunHisMirrorMapper;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorRowSupport;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 镜像库写入执行器（独立 Bean 以保证 {@link DataSource} 切面生效）。
 */
@Service
public class MsunHisMirrorSyncExecutor
{
    private static final String MIRROR_SOURCE_API = "api";

    private final MsunHisMirrorMapper mirrorMapper;

    public MsunHisMirrorSyncExecutor(MsunHisMirrorMapper mirrorMapper)
    {
        this.mirrorMapper = mirrorMapper;
    }

    @DataSource(DataSourceType.SPD)
    public int execute(MsunHospitalRuntime runtime, String apiCode, String batchNo, JSONObject wrappedResponse)
    {
        Object hisBodyObj = wrappedResponse.get("hisBody");
        if (!(hisBodyObj instanceof JSONObject))
        {
            return 0;
        }
        JSONObject hisBody = (JSONObject) hisBodyObj;
        if (!Boolean.TRUE.equals(hisBody.getBoolean("success")))
        {
            return 0;
        }
        JSONArray data = hisBody.getJSONArray("data");
        if (data == null || data.isEmpty())
        {
            return 0;
        }

        String traceId = hisBody.getString("traceId");
        Object requestParams = wrappedResponse.get("requestParams");
        String requestJson = requestParams == null ? null : JSON.toJSONString(requestParams);

        int rows;
        switch (apiCode)
        {
            case "2.1.9":
                rows = syncDepts(runtime, apiCode, batchNo, traceId, requestJson, data);
                break;
            case "2.1.12":
                rows = syncIdentities(runtime, apiCode, batchNo, traceId, requestJson, data);
                break;
            case "2.5.44":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, "m_drug_dict");
                break;
            case "2.5.58":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, "m_dict_category");
                break;
            case "2.5.62":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, "m_supplier");
                break;
            case "2.5.63":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, "m_producer");
                break;
            case "2.5.82":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, "m_merge_stock");
                break;
            case "2.5.43":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, "m_drug_batch_stock");
                break;
            case "2.5.102":
                rows = syncYkInstock(runtime, apiCode, batchNo, traceId, requestJson, data);
                break;
            default:
                return 0;
        }

        upsertSyncBatch(runtime, apiCode, batchNo, rows);
        return rows;
    }

    private int syncDepts(
            MsunHospitalRuntime runtime,
            String apiCode,
            String batchNo,
            String traceId,
            String requestJson,
            JSONArray data)
    {
        int count = 0;
        for (int i = 0; i < data.size(); i++)
        {
            JSONObject item = data.getJSONObject(i);
            Map<String, Object> row = MsunHisMirrorRowSupport.buildMirrorRow(
                    runtime, apiCode, batchNo, traceId, requestJson, item, MIRROR_SOURCE_API);
            upsertRow("m_dept", row);
            count++;

            JSONArray cats = item.getJSONArray("categoryIdList");
            if (cats != null)
            {
                String deptId = item.getString("deptId");
                for (int j = 0; j < cats.size(); j++)
                {
                    Map<String, Object> relFields = new HashMap<>(2);
                    relFields.put("dept_id", deptId);
                    relFields.put("category_id", String.valueOf(cats.get(j)));
                    Map<String, Object> rel = MsunHisMirrorRowSupport.buildChildRelRow(runtime, batchNo, relFields);
                    upsertRow("m_dept_category_rel", rel);
                }
            }
        }
        return count;
    }

    private int syncIdentities(
            MsunHospitalRuntime runtime,
            String apiCode,
            String batchNo,
            String traceId,
            String requestJson,
            JSONArray data)
    {
        int count = 0;
        for (int i = 0; i < data.size(); i++)
        {
            JSONObject item = data.getJSONObject(i);
            Map<String, Object> row = MsunHisMirrorRowSupport.buildMirrorRow(
                    runtime, apiCode, batchNo, traceId, requestJson, item, MIRROR_SOURCE_API);
            upsertRow("m_user_identity", row);
            count++;

            JSONArray accounts = item.getJSONArray("accountList");
            if (accounts != null)
            {
                String identityId = item.getString("identityId");
                for (int j = 0; j < accounts.size(); j++)
                {
                    Map<String, Object> relFields = new HashMap<>(2);
                    relFields.put("identity_id", identityId);
                    relFields.put("account_no", String.valueOf(accounts.get(j)));
                    Map<String, Object> rel = MsunHisMirrorRowSupport.buildChildRelRow(runtime, batchNo, relFields);
                    upsertRow("m_user_identity_account", rel);
                }
            }
        }
        return count;
    }

    private int syncFlatRows(
            MsunHospitalRuntime runtime,
            String apiCode,
            String batchNo,
            String traceId,
            String requestJson,
            JSONArray data,
            String table)
    {
        int count = 0;
        for (int i = 0; i < data.size(); i++)
        {
            JSONObject item = data.getJSONObject(i);
            Map<String, Object> row = MsunHisMirrorRowSupport.buildMirrorRow(
                    runtime, apiCode, batchNo, traceId, requestJson, item, MIRROR_SOURCE_API);
            upsertRow(table, row);
            count++;
        }
        return count;
    }

    private int syncYkInstock(
            MsunHospitalRuntime runtime,
            String apiCode,
            String batchNo,
            String traceId,
            String requestJson,
            JSONArray data)
    {
        int count = 0;
        for (int i = 0; i < data.size(); i++)
        {
            JSONObject item = data.getJSONObject(i);
            Map<String, Object> header = MsunHisMirrorRowSupport.buildMirrorRow(
                    runtime, apiCode, batchNo, traceId, requestJson, item, MIRROR_SOURCE_API);
            upsertRow("m_yk_instock", header);
            count++;

            String storageInstockId = item.getString("storageInstockId");
            String instockCode = item.getString("instockCode");
            if (StringUtils.isNotEmpty(storageInstockId))
            {
                mirrorMapper.deleteYkInstockDetails(
                        runtime.getHospitalKey(), runtime.getActiveEnv(), storageInstockId);
            }

            JSONArray details = item.getJSONArray("stockDetailList");
            if (details != null)
            {
                for (int j = 0; j < details.size(); j++)
                {
                    JSONObject detail = details.getJSONObject(j);
                    Map<String, Object> detailRow = MsunHisMirrorRowSupport.buildMirrorRow(
                            runtime, apiCode, batchNo, traceId, requestJson, detail, MIRROR_SOURCE_API);
                    detailRow.put("storage_instock_id", storageInstockId);
                    detailRow.put("instock_code", instockCode);
                    detailRow.remove("api_code");
                    detailRow.remove("his_trace_id");
                    detailRow.remove("request_params_json");
                    detailRow.remove("raw_item_json");
                    detailRow.remove("mirror_source");
                    upsertRow("m_yk_instock_detail", detailRow);
                }
            }
        }
        return count;
    }

    private void upsertSyncBatch(MsunHospitalRuntime runtime, String apiCode, String batchNo, int recordCount)
    {
        Map<String, Object> batch = new HashMap<>(8);
        batch.put("sync_batch_no", batchNo);
        batch.put("hospital_key", runtime.getHospitalKey());
        batch.put("tenant_id", runtime.getTenantId());
        batch.put("active_env", runtime.getActiveEnv());
        batch.put("api_code", apiCode);
        batch.put("mirror_source", MIRROR_SOURCE_API);
        batch.put("record_count", recordCount);
        batch.put("remark", "API查询自动落库");
        upsertRow("m_sync_batch", batch);
    }

    private void upsertRow(String table, Map<String, Object> row)
    {
        MsunHisMirrorRowSupport.ensurePrimaryKey(table, row);
        MsunHisMirrorRowSupport.stampTimestamps(row);
        mirrorMapper.upsertMirrorRow(table, row);
    }
}
