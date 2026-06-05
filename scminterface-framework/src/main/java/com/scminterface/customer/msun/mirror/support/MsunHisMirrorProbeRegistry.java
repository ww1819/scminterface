package com.scminterface.customer.msun.mirror.support;

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
                spec("m_dept", "科室主表", true),
                spec("m_dept_category_rel", "科室分类关联", false));
        register("identities", "2.1.12", "人员身份镜像",
                spec("m_user_identity", "身份主表", true),
                spec("m_user_identity_account", "身份账号", false));
        register("drugDict", "2.5.44", "药品材料字典镜像", spec("m_drug_dict", "字典主表", true));
        register("dictCategory", "2.5.58", "分类字典镜像", spec("m_dict_category", "分类主表", true));
        register("suppliers", "2.5.62", "供应商镜像", spec("m_supplier", "供应商主表", true));
        register("producers", "2.5.63", "生产厂商镜像", spec("m_producer", "厂商主表", true));
        register("mergeStocks", "2.5.82", "合并库存镜像", spec("m_merge_stock", "合并库存", true));
        register("batchStocks", "2.5.43", "批次库存镜像", spec("m_drug_batch_stock", "批次库存", true));
        register("ykInstock", "2.5.102", "一级库入退库镜像",
                spec("m_yk_instock", "入退库主表", true),
                spec("m_yk_instock_detail", "入退库明细", false));
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
