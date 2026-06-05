package com.scminterface.customer.msun.mirror.service;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.mapper.MsunHisMirrorMapper;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorProbeRegistry;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorProbeRegistry.MirrorTableSpec;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorProbeRegistry.ProbeMirrorSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 镜像表只读查询（独立 Bean 以保证 {@link DataSource} 切面生效）。
 */
@Service
public class MsunHisMirrorQueryExecutor
{
    private static final int MAX_LIMIT = 200;

    private final MsunHisMirrorMapper mirrorMapper;

    public MsunHisMirrorQueryExecutor(MsunHisMirrorMapper mirrorMapper)
    {
        this.mirrorMapper = mirrorMapper;
    }

    @DataSource(DataSourceType.SPD)
    public Map<String, Object> queryByProbeKey(
            MsunHospitalRuntime runtime,
            String probeKey,
            int limit,
            int offset)
    {
        ProbeMirrorSpec spec = MsunHisMirrorProbeRegistry.specOf(probeKey);
        if (spec == null)
        {
            throw new IllegalArgumentException("不支持的镜像查询 probeKey: " + probeKey);
        }
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        int safeOffset = Math.max(offset, 0);

        Map<String, Object> result = new LinkedHashMap<>(12);
        result.put("probeKey", probeKey);
        result.put("apiCode", spec.getApiCode());
        result.put("title", spec.getTitle());
        result.put("hospitalKey", runtime.getHospitalKey());
        result.put("tenantId", runtime.getTenantId());
        result.put("activeEnv", runtime.getActiveEnv());
        result.put("limit", safeLimit);
        result.put("offset", safeOffset);

        List<Map<String, Object>> tableViews = new ArrayList<>(spec.getTables().size());
        for (MirrorTableSpec tableSpec : spec.getTables())
        {
            Map<String, Object> query = baseScope(runtime);
            query.put("table", tableSpec.getTable());
            if (tableSpec.isFilterByApiCode())
            {
                query.put("apiCode", spec.getApiCode());
            }
            long total = mirrorMapper.countMirrorRows(query);
            query.put("limit", safeLimit);
            query.put("offset", safeOffset);
            List<Map<String, Object>> rows = mirrorMapper.listMirrorRows(query);

            Map<String, Object> view = new LinkedHashMap<>(8);
            view.put("table", tableSpec.getTable());
            view.put("label", tableSpec.getLabel());
            view.put("total", total);
            view.put("limit", safeLimit);
            view.put("offset", safeOffset);
            view.put("rows", rows);
            tableViews.add(view);
        }
        result.put("tables", tableViews);
        return result;
    }

    private static Map<String, Object> baseScope(MsunHospitalRuntime runtime)
    {
        Map<String, Object> scope = new HashMap<>(4);
        scope.put("hospitalKey", runtime.getHospitalKey());
        scope.put("tenantId", runtime.getTenantId());
        scope.put("activeEnv", runtime.getActiveEnv());
        return scope;
    }
}
