package com.scminterface.customer.msun.mirror.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.framework.util.ZsUuid7;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 众阳 HIS 回参行 → 镜像表列（camelCase → snake_case）。
 */
public final class MsunHisMirrorRowSupport
{
    private static final Pattern COLUMN_NAME = Pattern.compile("^[a-z][a-z0-9_]*$");

    private static final Map<String, String> TABLE_PRIMARY_KEY = new HashMap<>(16);

    static
    {
        TABLE_PRIMARY_KEY.put(MsunHisMirrorTableNames.SYNC_BATCH, "batch_id");
        TABLE_PRIMARY_KEY.put(MsunHisMirrorTableNames.DEPT_CATEGORY_REL, "rel_id");
        TABLE_PRIMARY_KEY.put(MsunHisMirrorTableNames.USER_IDENTITY_ACCOUNT, "rel_id");
        TABLE_PRIMARY_KEY.put(MsunHisMirrorTableNames.YK_INSTOCK_DETAIL, "detail_id");
    }

    private MsunHisMirrorRowSupport()
    {
    }

    /**
     * 镜像表主键均为 UUID7（varchar 36）；冲突走业务唯一键 upsert 时不更新主键列。
     */
    public static void ensurePrimaryKey(String table, Map<String, Object> row)
    {
        if (row == null || table == null)
        {
            return;
        }
        String pkCol = TABLE_PRIMARY_KEY.getOrDefault(table, "mirror_id");
        if (!row.containsKey(pkCol))
        {
            row.put(pkCol, ZsUuid7.newString());
        }
    }

    /** 新行写入插入/更新时间；upsert 冲突时 SQL 保留 insert_time、刷新 update_time。 */
    public static void stampTimestamps(Map<String, Object> row)
    {
        if (row == null)
        {
            return;
        }
        Date now = new Date();
        row.put("update_time", now);
        if (!row.containsKey("insert_time"))
        {
            row.put("insert_time", now);
        }
    }

    public static Map<String, Object> buildMirrorRow(
            MsunHospitalRuntime runtime,
            String apiCode,
            String syncBatchNo,
            String hisTraceId,
            String requestParamsJson,
            JSONObject item,
            String mirrorSource)
    {
        Map<String, Object> row = new LinkedHashMap<>(item.size() + 13);
        row.put("hospital_key", runtime.getHospitalKey());
        row.put("tenant_id", runtime.getTenantId());
        row.put("active_env", runtime.getActiveEnv());
        row.put("api_code", apiCode);
        row.put("sync_batch_no", syncBatchNo);
        row.put("his_trace_id", hisTraceId);
        row.put("request_params_json", requestParamsJson);
        row.put("raw_item_json", item == null ? null : item.toJSONString());
        row.put("mirror_source", mirrorSource);
        if (item != null)
        {
            for (String key : item.keySet())
            {
                if ("categoryIdList".equals(key) || "accountList".equals(key) || "stockDetailList".equals(key))
                {
                    continue;
                }
                String col = camelToSnake(key);
                if (isValidColumn(col))
                {
                    row.put(col, normalizeValue(item.get(key)));
                }
            }
        }
        return row;
    }

    /**
     * 2.5.44 字典行：HIS data 项常不带 materialOrDrug，从请求参数补全至 material_or_drug 列。
     */
    /**
     * 2.5.43 批次库存：API 字段名与历史镜像列不一致时，补全唯一键与查询用列。
     */
    public static void enrichDrugBatchStockRow(Map<String, Object> row)
    {
        if (row == null)
        {
            return;
        }
        copyIfEmpty(row, "dept_id", row.get("yc_dept_id"));
        copyIfEmpty(row, "batch_number", row.get("yc_batch_no"));
        copyIfEmpty(row, "quantity", row.get("stock_amount"));
        copyIfEmpty(row, "effective", row.get("effective_date"));
        copyIfEmpty(row, "producer_name", row.get("producer_cnname"));
    }

    private static void copyIfEmpty(Map<String, Object> row, String targetKey, Object sourceVal)
    {
        if (row == null || sourceVal == null)
        {
            return;
        }
        Object existing = row.get(targetKey);
        if (existing == null || String.valueOf(existing).trim().isEmpty())
        {
            row.put(targetKey, normalizeValue(sourceVal));
        }
    }

    public static void enrichDrugDictRow(Map<String, Object> row, String requestParamsJson)
    {
        if (row == null || row.get("material_or_drug") != null)
        {
            return;
        }
        if (requestParamsJson == null || requestParamsJson.isEmpty())
        {
            return;
        }
        try
        {
            JSONObject req = JSON.parseObject(requestParamsJson);
            Object mod = req.get("materialOrDrug");
            if (mod != null)
            {
                row.put("material_or_drug", normalizeValue(mod));
            }
        }
        catch (Exception ignored)
        {
            // 请求 JSON 解析失败时保持为空，由 SPD 同步侧再推断
        }
    }

    public static Map<String, Object> buildChildRelRow(
            MsunHospitalRuntime runtime,
            String syncBatchNo,
            Map<String, Object> fields)
    {
        Map<String, Object> row = new LinkedHashMap<>(fields.size() + 5);
        row.put("hospital_key", runtime.getHospitalKey());
        row.put("tenant_id", runtime.getTenantId());
        row.put("active_env", runtime.getActiveEnv());
        row.put("sync_batch_no", syncBatchNo);
        row.putAll(fields);
        return row;
    }

    public static String camelToSnake(String name)
    {
        if (name == null || name.isEmpty())
        {
            return name;
        }
        StringBuilder sb = new StringBuilder(name.length() + 8);
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (Character.isUpperCase(c))
            {
                if (i > 0)
                {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static Object normalizeValue(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof JSONArray || value instanceof JSONObject)
        {
            return JSON.toJSONString(value);
        }
        if (value instanceof BigDecimal)
        {
            return value;
        }
        if (value instanceof Number)
        {
            return value;
        }
        if (value instanceof Boolean)
        {
            return value;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    public static boolean isValidColumn(String col)
    {
        return col != null && COLUMN_NAME.matcher(col).matches();
    }

    /** 镜像表 VARCHAR 列写入前截断，避免 Data truncation。 */
    public static String clampVarchar(String value, int maxLen)
    {
        if (value == null || maxLen <= 0)
        {
            return value;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
