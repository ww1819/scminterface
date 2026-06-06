package com.scminterface.customer.msun.mirror.support;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 镜像表联动关系：落库/查询时主表与从表、批次日志表等须同时存在。
 * <p>
 * 与 {@link com.scminterface.customer.msun.mirror.service.MsunHisMirrorSyncExecutor} 写入逻辑对齐。
 */
public final class MsunHisMirrorTableLinkage
{
    private static final String SYNC_BATCH = "m_sync_batch";

    private static final Map<String, List<String>> LINKED_TABLES;

    static
    {
        Map<String, Set<String>> builder = new LinkedHashMap<>();

        // 2.1.9 科室 + 分类关联
        linkPair(builder, "m_dept", "m_dept_category_rel");
        linkToAll(builder, SYNC_BATCH, "m_dept", "m_dept_category_rel");

        // 2.1.12 身份 + 账号
        linkPair(builder, "m_user_identity", "m_user_identity_account");
        linkToAll(builder, SYNC_BATCH, "m_user_identity", "m_user_identity_account");

        // 2.5.102 入退库主表 + 明细
        linkPair(builder, "m_yk_instock", "m_yk_instock_detail");
        linkToAll(builder, SYNC_BATCH, "m_yk_instock", "m_yk_instock_detail");

        // 单表落库均写 m_sync_batch
        for (String flat : new String[] {
                "m_drug_dict", "m_dict_category", "m_supplier", "m_producer",
                "m_merge_stock", "m_drug_batch_stock"
        })
        {
            linkToAll(builder, SYNC_BATCH, flat);
        }

        LINKED_TABLES = freeze(builder);
    }

    private MsunHisMirrorTableLinkage()
    {
    }

    /**
     * 将种子表扩展为含所有联动表的稳定有序列表（传递闭包）。
     */
    public static List<String> expandWithLinkages(Collection<String> seedTables)
    {
        if (seedTables == null || seedTables.isEmpty())
        {
            return Collections.emptyList();
        }
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (String seed : seedTables)
        {
            if (seed != null && !seed.isEmpty())
            {
                queue.add(seed);
            }
        }
        while (!queue.isEmpty())
        {
            String table = queue.poll();
            if (!resolved.add(table))
            {
                continue;
            }
            List<String> linked = LINKED_TABLES.get(table);
            if (linked == null)
            {
                continue;
            }
            for (String next : linked)
            {
                if (!resolved.contains(next))
                {
                    queue.add(next);
                }
            }
        }
        return new ArrayList<>(resolved);
    }

    public static List<String> linkedTablesOf(String tableName)
    {
        if (tableName == null)
        {
            return Collections.emptyList();
        }
        List<String> list = LINKED_TABLES.get(tableName);
        return list == null ? Collections.emptyList() : list;
    }

    private static void linkPair(Map<String, Set<String>> builder, String a, String b)
    {
        addLink(builder, a, b);
        addLink(builder, b, a);
    }

    private static void linkToAll(Map<String, Set<String>> builder, String target, String... sources)
    {
        for (String source : sources)
        {
            addLink(builder, source, target);
        }
    }

    private static void addLink(Map<String, Set<String>> builder, String from, String to)
    {
        builder.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
    }

    private static Map<String, List<String>> freeze(Map<String, Set<String>> builder)
    {
        Map<String, List<String>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : builder.entrySet())
        {
            frozen.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue())));
        }
        return Collections.unmodifiableMap(frozen);
    }
}
