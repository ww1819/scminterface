package com.scminterface.customer.msun.mirror.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * MyBatis 动态 upsert SQL（表名白名单，防注入）。
 */
public final class MsunHisMirrorSqlProvider
{
    private static final Set<String> ALLOWED_TABLES = new HashSet<>();

    static
    {
        ALLOWED_TABLES.add("m_sync_batch");
        ALLOWED_TABLES.add("m_dept");
        ALLOWED_TABLES.add("m_dept_category_rel");
        ALLOWED_TABLES.add("m_user_identity");
        ALLOWED_TABLES.add("m_user_identity_account");
        ALLOWED_TABLES.add("m_drug_dict");
        ALLOWED_TABLES.add("m_dict_category");
        ALLOWED_TABLES.add("m_supplier");
        ALLOWED_TABLES.add("m_producer");
        ALLOWED_TABLES.add("m_yk_instock");
        ALLOWED_TABLES.add("m_yk_instock_detail");
        ALLOWED_TABLES.add("m_drug_batch_stock");
    }

    private MsunHisMirrorSqlProvider()
    {
    }

    @SuppressWarnings("unchecked")
    public String upsertMirrorRow(Map<String, Object> params)
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

    public String deleteYkInstockDetails(Map<String, Object> params)
    {
        return "DELETE FROM m_yk_instock_detail WHERE hospital_key = #{hospitalKey} AND active_env = #{activeEnv} "
                + "AND storage_instock_id = #{storageInstockId}";
    }

    public String countMirrorRows(Map<String, Object> params)
    {
        return buildMirrorSelectSql(params, true);
    }

    public String listMirrorRows(Map<String, Object> params)
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
}
