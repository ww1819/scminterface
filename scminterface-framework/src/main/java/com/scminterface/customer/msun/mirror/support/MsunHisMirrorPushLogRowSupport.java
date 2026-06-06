package com.scminterface.customer.msun.mirror.support;

import com.alibaba.fastjson2.JSON;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link MsunHisMirrorTableNames#PUSH_LOG} 行写入前字段规整，避免 VARCHAR 截断导致推送失败。
 */
public final class MsunHisMirrorPushLogRowSupport
{
    private static final int PUSH_MSG_MAX_LEN = 480;
    private static final int HIS_TRACE_ID_MAX_LEN = 64;
    private static final int PUSH_STATUS_MAX_LEN = 16;
    private static final int BILL_NO_MAX_LEN = 64;
    private static final int API_CODE_MAX_LEN = 16;

    private MsunHisMirrorPushLogRowSupport()
    {
    }

    public static Map<String, Object> sanitize(Map<String, Object> row)
    {
        if (row == null || row.isEmpty())
        {
            return row;
        }
        Map<String, Object> copy = new LinkedHashMap<>(row);
        clamp(copy, "hospital_key", 64);
        clamp(copy, "tenant_id", 64);
        clamp(copy, "active_env", 16);
        clamp(copy, "spd_bill_id", 64);
        clamp(copy, "spd_entry_id", 64);
        clamp(copy, "bill_no", BILL_NO_MAX_LEN);
        clamp(copy, "bill_type", 16);
        clamp(copy, "api_code", API_CODE_MAX_LEN);
        clamp(copy, "his_trace_id", HIS_TRACE_ID_MAX_LEN);
        clamp(copy, "push_status", PUSH_STATUS_MAX_LEN);
        clamp(copy, "push_msg", PUSH_MSG_MAX_LEN);
        return copy;
    }

    public static String extractHisMessage(Object messageObj)
    {
        if (messageObj == null)
        {
            return null;
        }
        if (messageObj instanceof String)
        {
            return (String) messageObj;
        }
        return JSON.toJSONString(messageObj);
    }

    private static void clamp(Map<String, Object> row, String col, int maxLen)
    {
        if (!row.containsKey(col))
        {
            return;
        }
        Object val = row.get(col);
        if (val == null)
        {
            return;
        }
        row.put(col, MsunHisMirrorRowSupport.clampVarchar(String.valueOf(val), maxLen));
    }
}
