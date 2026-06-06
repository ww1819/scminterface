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
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorTableNames;
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
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, MsunHisMirrorTableNames.DRUG_DICT);
                break;
            case "2.5.58":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, MsunHisMirrorTableNames.DICT_CATEGORY);
                break;
            case "2.5.62":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, MsunHisMirrorTableNames.SUPPLIER);
                break;
            case "2.5.63":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, MsunHisMirrorTableNames.PRODUCER);
                break;
            case "2.5.82":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, MsunHisMirrorTableNames.MERGE_STOCK);
                break;
            case "2.5.43":
                rows = syncFlatRows(runtime, apiCode, batchNo, traceId, requestJson, data, MsunHisMirrorTableNames.DRUG_BATCH_STOCK);
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
            upsertRow(MsunHisMirrorTableNames.DEPT, row);
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
                    upsertRow(MsunHisMirrorTableNames.DEPT_CATEGORY_REL, rel);
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
            upsertRow(MsunHisMirrorTableNames.USER_IDENTITY, row);
            count++;

            JSONArray accounts = item.getJSONArray("accountList");
            if (accounts != null)
            {
                String identityId = item.getString("identityId");
                for (int j = 0; j < accounts.size(); j++)
                {
                    String accountNo = resolveAccountNo(accounts.get(j));
                    if (StringUtils.isEmpty(accountNo))
                    {
                        continue;
                    }
                    Map<String, Object> relFields = new HashMap<>(2);
                    relFields.put("identity_id", identityId);
                    relFields.put("account_no", accountNo);
                    Map<String, Object> rel = MsunHisMirrorRowSupport.buildChildRelRow(runtime, batchNo, relFields);
                    upsertRow(MsunHisMirrorTableNames.USER_IDENTITY_ACCOUNT, rel);
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
            if (MsunHisMirrorTableNames.DRUG_DICT.equals(table))
            {
                MsunHisMirrorRowSupport.enrichDrugDictRow(row, requestJson);
            }
            else if (MsunHisMirrorTableNames.DRUG_BATCH_STOCK.equals(table))
            {
                MsunHisMirrorRowSupport.enrichDrugBatchStockRow(row);
            }
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
            upsertRow(MsunHisMirrorTableNames.YK_INSTOCK, header);
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
                    upsertRow(MsunHisMirrorTableNames.YK_INSTOCK_DETAIL, detailRow);
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
        upsertRow(MsunHisMirrorTableNames.SYNC_BATCH, batch);
    }

    @DataSource(DataSourceType.SPD)
    public String resolveLatestBatchNo(MsunHospitalRuntime runtime, String apiCode)
    {
        if (runtime == null || StringUtils.isEmpty(apiCode))
        {
            return null;
        }
        Map<String, Object> params = new HashMap<>(4);
        params.put("hospitalKey", runtime.getHospitalKey());
        params.put("tenantId", runtime.getTenantId());
        params.put("activeEnv", runtime.getActiveEnv());
        params.put("apiCode", apiCode);
        return mirrorMapper.selectLatestSyncBatchNo(params);
    }

    @DataSource(DataSourceType.SPD)
    public long countBatchMirrorRows(
            MsunHospitalRuntime runtime,
            String table,
            String apiCode,
            String batchNo)
    {
        if (runtime == null || StringUtils.isEmpty(table) || StringUtils.isEmpty(batchNo))
        {
            return 0L;
        }
        Map<String, Object> params = new HashMap<>(8);
        params.put("table", table);
        params.put("hospitalKey", runtime.getHospitalKey());
        params.put("tenantId", runtime.getTenantId());
        params.put("activeEnv", runtime.getActiveEnv());
        params.put("apiCode", apiCode);
        params.put("syncBatchNo", batchNo);
        return mirrorMapper.countMirrorRows(params);
    }

    private void upsertRow(String table, Map<String, Object> row)
    {
        MsunHisMirrorRowSupport.ensurePrimaryKey(table, row);
        MsunHisMirrorRowSupport.stampTimestamps(row);
        mirrorMapper.upsertMirrorRow(table, row);
    }

    private static String resolveAccountNo(Object account)
    {
        if (account == null)
        {
            return null;
        }
        if (account instanceof JSONObject)
        {
            JSONObject obj = (JSONObject) account;
            String no = obj.getString("accountNo");
            if (StringUtils.isEmpty(no))
            {
                no = obj.getString("account_no");
            }
            return no;
        }
        String text = String.valueOf(account).trim();
        return text.isEmpty() ? null : text;
    }
}
