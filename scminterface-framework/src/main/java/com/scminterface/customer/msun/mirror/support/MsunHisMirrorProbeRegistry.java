package com.scminterface.customer.msun.mirror.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探针页 apiKey 与镜像表、HIS 接口编号映射。
 */
public final class MsunHisMirrorProbeRegistry
{
    private static final Map<String, ProbeMirrorSpec> SPECS = new LinkedHashMap<>();

    static
    {
        register("depts", "2.1.9", "科室镜像",
                spec(MsunHisMirrorTableNames.DEPT, "科室主表", true),
                spec(MsunHisMirrorTableNames.DEPT_CATEGORY_REL, "科室分类关联", false));
        register("identities", "2.1.12", "人员身份镜像",
                spec(MsunHisMirrorTableNames.USER_IDENTITY, "身份主表", true),
                spec(MsunHisMirrorTableNames.USER_IDENTITY_ACCOUNT, "身份账号", false));
        register("drugDict", "2.5.44", "药品材料字典镜像", spec(MsunHisMirrorTableNames.DRUG_DICT, "字典主表", true));
        register("dictCategory", "2.5.58", "分类字典镜像", spec(MsunHisMirrorTableNames.DICT_CATEGORY, "分类主表", true));
        register("suppliers", "2.5.62", "供应商镜像", spec(MsunHisMirrorTableNames.SUPPLIER, "供应商主表", true));
        register("producers", "2.5.63", "生产厂商镜像", spec(MsunHisMirrorTableNames.PRODUCER, "厂商主表", true));
        register("mergeStocks", "2.5.82", "合并库存镜像", spec(MsunHisMirrorTableNames.MERGE_STOCK, "合并库存", true));
        register("batchStocks", "2.5.43", "批次库存镜像", spec(MsunHisMirrorTableNames.DRUG_BATCH_STOCK, "批次库存", true));
        register("ykInstock", "2.5.102", "一级库入退库镜像",
                spec(MsunHisMirrorTableNames.YK_INSTOCK, "入退库主表", true),
                spec(MsunHisMirrorTableNames.YK_INSTOCK_DETAIL, "入退库明细", false));
    }

    private MsunHisMirrorProbeRegistry()
    {
    }

    public static ProbeMirrorSpec specOf(String probeKey)
    {
        return SPECS.get(probeKey);
    }

    public static boolean isQueryable(String probeKey)
    {
        return SPECS.containsKey(probeKey);
    }

    public static List<String> tableNamesForProbe(String probeKey)
    {
        ProbeMirrorSpec spec = specOf(probeKey);
        if (spec == null)
        {
            return Collections.emptyList();
        }
        List<String> tables = new ArrayList<>(spec.getTables().size());
        for (MirrorTableSpec t : spec.getTables())
        {
            tables.add(t.getTable());
        }
        return tables;
    }

    /** 主数据 SPD 同步所用的主镜像表（filterByApiCode=true 的首表）。 */
    public static String primaryTableForProbe(String probeKey)
    {
        ProbeMirrorSpec spec = specOf(probeKey);
        if (spec == null)
        {
            return null;
        }
        for (MirrorTableSpec t : spec.getTables())
        {
            if (t.isFilterByApiCode())
            {
                return t.getTable();
            }
        }
        return spec.getTables().isEmpty() ? null : spec.getTables().get(0).getTable();
    }

    public static boolean supportsSpdMasterSync(String probeKey)
    {
        ProbeMirrorSpec spec = specOf(probeKey);
        return spec != null && com.scminterface.customer.msun.spd.sync.support.MsunSpdMasterSyncSupport
                .isMasterDataApi(spec.getApiCode());
    }

    private static void register(String probeKey, String apiCode, String title, MirrorTableSpec... tables)
    {
        SPECS.put(probeKey, new ProbeMirrorSpec(probeKey, apiCode, title, Arrays.asList(tables)));
    }

    private static MirrorTableSpec spec(String table, String label, boolean filterByApiCode)
    {
        return new MirrorTableSpec(table, label, filterByApiCode);
    }

    public static final class ProbeMirrorSpec
    {
        private final String probeKey;
        private final String apiCode;
        private final String title;
        private final List<MirrorTableSpec> tables;

        ProbeMirrorSpec(String probeKey, String apiCode, String title, List<MirrorTableSpec> tables)
        {
            this.probeKey = probeKey;
            this.apiCode = apiCode;
            this.title = title;
            this.tables = Collections.unmodifiableList(tables);
        }

        public String getProbeKey()
        {
            return probeKey;
        }

        public String getApiCode()
        {
            return apiCode;
        }

        public String getTitle()
        {
            return title;
        }

        public List<MirrorTableSpec> getTables()
        {
            return tables;
        }
    }

    public static final class MirrorTableSpec
    {
        private final String table;
        private final String label;
        private final boolean filterByApiCode;

        MirrorTableSpec(String table, String label, boolean filterByApiCode)
        {
            this.table = table;
            this.label = label;
            this.filterByApiCode = filterByApiCode;
        }

        public String getTable()
        {
            return table;
        }

        public String getLabel()
        {
            return label;
        }

        public boolean isFilterByApiCode()
        {
            return filterByApiCode;
        }
    }
}
