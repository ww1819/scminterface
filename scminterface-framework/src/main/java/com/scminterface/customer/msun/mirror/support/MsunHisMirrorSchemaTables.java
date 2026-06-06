package com.scminterface.customer.msun.mirror.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口 / 探针与镜像表入口映射；联动表由 {@link MsunHisMirrorTableLinkage} 自动展开。
 */
public final class MsunHisMirrorSchemaTables
{
    private static final Map<String, List<String>> API_SEED_TABLES;

    static
    {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("2.1.9", Collections.singletonList(MsunHisMirrorTableNames.DEPT));
        map.put("2.1.12", Collections.singletonList(MsunHisMirrorTableNames.USER_IDENTITY));
        map.put("2.5.44", Collections.singletonList(MsunHisMirrorTableNames.DRUG_DICT));
        map.put("2.5.58", Collections.singletonList(MsunHisMirrorTableNames.DICT_CATEGORY));
        map.put("2.5.62", Collections.singletonList(MsunHisMirrorTableNames.SUPPLIER));
        map.put("2.5.63", Collections.singletonList(MsunHisMirrorTableNames.PRODUCER));
        map.put("2.5.82", Collections.singletonList(MsunHisMirrorTableNames.MERGE_STOCK));
        map.put("2.5.43", Collections.singletonList(MsunHisMirrorTableNames.DRUG_BATCH_STOCK));
        map.put("2.5.102", Collections.singletonList(MsunHisMirrorTableNames.YK_INSTOCK));
        map.put("2.5.41", Collections.singletonList(MsunHisMirrorTableNames.PUSH_LOG));
        map.put("2.5.42", Collections.singletonList(MsunHisMirrorTableNames.PUSH_LOG));
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
        return MsunHisMirrorTableLinkage.expandWithLinkages(
                MsunHisMirrorProbeRegistry.tableNamesForProbe(probeKey));
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
