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
        TABLE_PRIMARY_KEY.put("m_sync_batch", "batch_id");
        TABLE_PRIMARY_KEY.put("m_dept_category_rel", "rel_id");
        TABLE_PRIMARY_KEY.put("m_user_identity_account", "rel_id");
        TABLE_PRIMARY_KEY.put("m_yk_instock_detail", "detail_id");
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
        row.put("mirror_time", new Date());
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
        row.put("mirror_time", new Date());
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
}
