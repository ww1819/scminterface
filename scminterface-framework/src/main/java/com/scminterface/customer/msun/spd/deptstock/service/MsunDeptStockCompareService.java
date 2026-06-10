package com.scminterface.customer.msun.spd.deptstock.service;

import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.datasource.DataSourceAvailability;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MsunDeptStockCompareService
{
    private final MsunDeptStockCompareExecutor executor;
    private final DataSourceAvailability dataSourceAvailability;

    public MsunDeptStockCompareService(
            MsunDeptStockCompareExecutor executor,
            DataSourceAvailability dataSourceAvailability)
    {
        this.executor = executor;
        this.dataSourceAvailability = dataSourceAvailability;
    }

    public Map<String, Object> queryList(MsunHospitalRuntime runtime, Map<String, Object> params)
    {
        assertSpdEnabled();
        Map<String, Object> query = buildQuery(runtime, params);
        long total = executor.countRows(stripPaging(query));
        List<Map<String, Object>> rows = executor.listRows(query);
        if (rows != null)
        {
            for (Map<String, Object> row : rows)
            {
                enrichQty(row);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>(8);
        result.put("total", total);
        result.put("rows", rows);
        result.put("pageNum", query.get("pageNum"));
        result.put("pageSize", query.get("pageSize"));
        return result;
    }

    private Map<String, Object> buildQuery(MsunHospitalRuntime runtime, Map<String, Object> params)
    {
        Map<String, Object> query = new HashMap<>(16);
        query.put("tenantId", runtime.getTenantId());
        query.put("activeEnv", StringUtils.isNotEmpty(runtime.getActiveEnv()) ? runtime.getActiveEnv() : "prod");
        query.put("departmentId", toLong(params.get("departmentId")));
        query.put("departmentKeyword", trim(params.get("departmentKeyword")));
        query.put("materialKeyword", trim(params.get("materialKeyword")));
        query.put("specKeyword", trim(params.get("specKeyword")));
        int pageNum = Math.max(toInt(params.get("pageNum"), 1), 1);
        int pageSize = Math.min(Math.max(toInt(params.get("pageSize"), 20), 1), 200);
        // PageHelper（supportMethodsArguments=true）根据 pageNum/pageSize 自动追加 LIMIT；count 须 stripPaging
        query.put("pageNum", pageNum);
        query.put("pageSize", pageSize);
        return query;
    }

    private static Map<String, Object> stripPaging(Map<String, Object> query)
    {
        Map<String, Object> copy = new HashMap<>(query);
        copy.remove("pageNum");
        copy.remove("pageSize");
        return copy;
    }

    private static void enrichQty(Map<String, Object> row)
    {
        BigDecimal spd = toDecimal(row.get("spd_qty"));
        row.put("spd_qty", spd);
        BigDecimal hisBatch = toDecimal(row.get("his_batch_qty"));
        BigDecimal hisMerge = toDecimal(row.get("his_merge_qty"));
        BigDecimal his = null;
        String source = "none";
        if (hisBatch != null)
        {
            his = hisBatch;
            source = "batch";
        }
        else if (hisMerge != null)
        {
            his = hisMerge;
            source = "merge";
        }
        row.put("his_qty", his);
        row.put("his_qty_source", source);
        if (his != null)
        {
            row.put("qty_diff", spd.subtract(his));
        }
    }

    private void assertSpdEnabled()
    {
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            throw new IllegalStateException("spring.datasource.druid.spd.enabled=false，SPD 数据源未启用");
        }
    }

    private static String trim(Object v)
    {
        return v == null ? null : StringUtils.trimToNull(String.valueOf(v));
    }

    private static int toInt(Object v, int def)
    {
        if (v == null)
        {
            return def;
        }
        try
        {
            return Integer.parseInt(String.valueOf(v).trim());
        }
        catch (NumberFormatException ex)
        {
            return def;
        }
    }

    private static Long toLong(Object v)
    {
        if (v == null || StringUtils.isEmpty(String.valueOf(v).trim()))
        {
            return null;
        }
        try
        {
            return Long.parseLong(String.valueOf(v).trim());
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    private static BigDecimal toDecimal(Object v)
    {
        if (v == null)
        {
            return null;
        }
        if (v instanceof BigDecimal)
        {
            return (BigDecimal) v;
        }
        try
        {
            return new BigDecimal(String.valueOf(v).trim());
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}
