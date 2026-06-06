package com.scminterface.customer.msun.mirror.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口 / 探针与镜像表入口映射；联动表由 {@link MsunHisMirrorTableLinkage} 自动展开。
 */
public final class MsunHisMirrorSchemaTables
{
    /** 各 API 落库时的主入口表（联动表自动补全） */
    private static final Map<String, List<String>> API_SEED_TABLES;

    static
    {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("2.1.9", Collections.singletonList("m_dept"));
        map.put("2.1.12", Collections.singletonList("m_user_identity"));
        map.put("2.5.44", Collections.singletonList("m_drug_dict"));
        map.put("2.5.58", Collections.singletonList("m_dict_category"));
        map.put("2.5.62", Collections.singletonList("m_supplier"));
        map.put("2.5.63", Collections.singletonList("m_producer"));
        map.put("2.5.82", Collections.singletonList("m_merge_stock"));
        map.put("2.5.43", Collections.singletonList("m_drug_batch_stock"));
        map.put("2.5.102", Collections.singletonList("m_yk_instock"));
        map.put("2.5.41", Collections.singletonList("m_his_push_log"));
        map.put("2.5.42", Collections.singletonList("m_his_push_log"));
        API_SEED_TABLES = Collections.unmodifiableMap(map);
    }

    private MsunHisMirrorSchemaTables()
    {
    }

    public static List<String> tablesForApi(String apiCode)
    {
        if (apiCode == null)
        {
            return Collections.emptyList();
        }
        List<String> seeds = API_SEED_TABLES.get(apiCode.trim());
        if (seeds == null)
        {
            return Collections.emptyList();
        }
        return MsunHisMirrorTableLinkage.expandWithLinkages(seeds);
    }

    public static List<String> tablesForProbe(String probeKey)
    {
        MsunHisMirrorProbeRegistry.ProbeMirrorSpec spec = MsunHisMirrorProbeRegistry.specOf(probeKey);
        if (spec == null)
        {
            return Collections.emptyList();
        }
        List<String> seeds = new ArrayList<>(spec.getTables().size());
        for (MsunHisMirrorProbeRegistry.MirrorTableSpec t : spec.getTables())
        {
            seeds.add(t.getTable());
        }
        return MsunHisMirrorTableLinkage.expandWithLinkages(seeds);
    }

    public static List<String> tablesForTable(String tableName)
    {
        if (tableName == null || tableName.isEmpty())
        {
            return Collections.emptyList();
        }
        return MsunHisMirrorTableLinkage.expandWithLinkages(Collections.singletonList(tableName));
    }
}
