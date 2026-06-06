package com.scminterface.customer.msun.mirror.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MyBatis 动态 upsert SQL（表名白名单，防注入）。
 * Provider 方法须为 public static，否则 MyBatis 无法反射调用（私有构造会触发 IllegalAccessException）。
 */
public final class MsunHisMirrorSqlProvider
{
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(MsunHisMirrorTableNames.allTableNames());

    private MsunHisMirrorSqlProvider()
    {
    }

    @SuppressWarnings("unchecked")
    public static String upsertMirrorRow(Map<String, Object> params)
    {
        String table = (String) params.get("table");
        Map<String, Object> row = (Map<String, Object>) params.get("row");
        if (!ALLOWED_TABLES.contains(table))
        {
            throw new IllegalArgumentException("非法镜像表: " + table);
        }
        if (row == null || row.isEmpty())
        {
            throw new IllegalArgumentException("镜像行数据为空");
        }
        List<String> cols = new ArrayList<>();
        for (String key : row.keySet())
        {
            if (MsunHisMirrorRowSupport.isValidColumn(key))
            {
                cols.add(key);
            }
        }
        if (cols.isEmpty())
        {
            throw new IllegalArgumentException("镜像行无有效列");
        }
        StringBuilder sql = new StringBuilder(256 + cols.size() * 24);
        sql.append("INSERT INTO `").append(table).append("` (");
        for (int i = 0; i < cols.size(); i++)
        {
            if (i > 0)
            {
                sql.append(',');
            }
            sql.append('`').append(cols.get(i)).append('`');
        }
        sql.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++)
        {
            if (i > 0)
            {
                sql.append(',');
            }
            sql.append("#{row.").append(cols.get(i)).append('}');
        }
        sql.append(") ON DUPLICATE KEY UPDATE ");
        boolean first = true;
        for (String col : cols)
        {
            if ("mirror_id".equals(col) || "rel_id".equals(col) || "detail_id".equals(col) || "batch_id".equals(col)
                    || "insert_time".equals(col))
            {
                continue;
            }
            if (!first)
            {
                sql.append(',');
            }
            sql.append('`').append(col).append("` = VALUES(`").append(col).append("`)");
            first = false;
        }
        return sql.toString();
    }

    public static String deleteYkInstockDetails(Map<String, Object> params)
    {
        return "DELETE FROM " + MsunHisMirrorTableNames.YK_INSTOCK_DETAIL
                + " WHERE hospital_key = #{hospitalKey} AND active_env = #{activeEnv} "
                + "AND storage_instock_id = #{storageInstockId}";
    }

    public static String countMirrorRows(Map<String, Object> params)
    {
        return buildMirrorSelectSql(params, true);
    }

    public static String listMirrorRows(Map<String, Object> params)
    {
        return buildMirrorSelectSql(params, false);
    }

    private static String buildMirrorSelectSql(Map<String, Object> params, boolean countOnly)
    {
        String table = (String) params.get("table");
        if (!ALLOWED_TABLES.contains(table))
        {
            throw new IllegalArgumentException("非法镜像表: " + table);
        }
        StringBuilder sql = new StringBuilder(160);
        if (countOnly)
        {
            sql.append("SELECT COUNT(*) FROM `").append(table).append("` WHERE hospital_key = #{hospitalKey} ");
        }
        else
        {
            sql.append("SELECT * FROM `").append(table).append("` WHERE hospital_key = #{hospitalKey} ");
        }
        sql.append("AND tenant_id = #{tenantId} AND active_env = #{activeEnv}");
        if (params.get("apiCode") != null)
        {
            sql.append(" AND api_code = #{apiCode}");
        }
        if (!countOnly)
        {
            sql.append(" ORDER BY update_time DESC LIMIT #{limit} OFFSET #{offset}");
        }
        return sql.toString();
    }

    @SuppressWarnings("unchecked")
    public static String insertMirrorRow(Map<String, Object> params)
    {
        String table = (String) params.get("table");
        Map<String, Object> row = (Map<String, Object>) params.get("row");
        if (!ALLOWED_TABLES.contains(table))
        {
            throw new IllegalArgumentException("非法镜像表: " + table);
        }
        if (row == null || row.isEmpty())
        {
            throw new IllegalArgumentException("镜像行数据为空");
        }
        List<String> cols = new ArrayList<>();
        for (String key : row.keySet())
        {
            if (MsunHisMirrorRowSupport.isValidColumn(key))
            {
                cols.add(key);
            }
        }
        StringBuilder sql = new StringBuilder(128 + cols.size() * 20);
        sql.append("INSERT INTO `").append(table).append("` (");
        for (int i = 0; i < cols.size(); i++)
        {
            if (i > 0)
            {
                sql.append(',');
            }
            sql.append('`').append(cols.get(i)).append('`');
        }
        sql.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++)
        {
            if (i > 0)
            {
                sql.append(',');
            }
            sql.append("#{row.").append(cols.get(i)).append('}');
        }
        sql.append(')');
        return sql.toString();
    }

    public static String queryEntryHisMirror(Map<String, Object> params)
    {
        StringBuilder sql = new StringBuilder(512);
        sql.append("SELECT * FROM ").append(MsunHisMirrorTableNames.DRUG_BATCH_STOCK)
                .append(" WHERE hospital_key = #{hospitalKey} ");
        sql.append("AND tenant_id = #{tenantId} AND active_env = #{activeEnv} ");
        if (params.get("pharmacyStockId") != null && !"".equals(String.valueOf(params.get("pharmacyStockId"))))
        {
            sql.append("AND (pharmacy_stock_id = #{pharmacyStockId} OR stock_id = #{pharmacyStockId}) ");
        }
        if (params.get("deptId") != null)
        {
            sql.append("AND dept_id = #{deptId} ");
        }
        if (params.get("drugId") != null)
        {
            sql.append("AND drug_id = #{drugId} ");
        }
        if (params.get("drugSpecPackingId") != null)
        {
            sql.append("AND drug_spec_packing_id = #{drugSpecPackingId} ");
        }
        if (params.get("batchNumber") != null)
        {
            sql.append("AND batch_number = #{batchNumber} ");
        }
        sql.append("ORDER BY update_time DESC LIMIT 20");
        return sql.toString();
    }

    public static String queryBillHisMirror(Map<String, Object> params)
    {
        StringBuilder sql = new StringBuilder(512);
        sql.append("SELECT log_id, hospital_key, tenant_id, active_env, spd_bill_id, spd_entry_id, ");
        sql.append("bill_no, bill_type, api_code, his_trace_id, push_status, push_msg, insert_time, ");
        sql.append("request_json, response_json FROM ").append(MsunHisMirrorTableNames.PUSH_LOG)
                .append(" WHERE hospital_key = #{hospitalKey} ");
        sql.append("AND tenant_id = #{tenantId} AND active_env = #{activeEnv} ");
        if (params.get("billId") != null && !"".equals(String.valueOf(params.get("billId"))))
        {
            sql.append("AND spd_bill_id = #{billId} ");
        }
        if (params.get("billType") != null && !"".equals(String.valueOf(params.get("billType"))))
        {
            sql.append("AND bill_type = #{billType} ");
        }
        sql.append("ORDER BY insert_time DESC LIMIT 50");
        return sql.toString();
    }
}
