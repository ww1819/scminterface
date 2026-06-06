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
 */
public final class MsunHisMirrorTableLinkage
{
    private static final Map<String, List<String>> LINKED_TABLES;

    static
    {
        Map<String, Set<String>> builder = new LinkedHashMap<>();

        linkPair(builder, MsunHisMirrorTableNames.DEPT, MsunHisMirrorTableNames.DEPT_CATEGORY_REL);
        linkToAll(builder, MsunHisMirrorTableNames.SYNC_BATCH,
                MsunHisMirrorTableNames.DEPT, MsunHisMirrorTableNames.DEPT_CATEGORY_REL);

        linkPair(builder, MsunHisMirrorTableNames.USER_IDENTITY, MsunHisMirrorTableNames.USER_IDENTITY_ACCOUNT);
        linkToAll(builder, MsunHisMirrorTableNames.SYNC_BATCH,
                MsunHisMirrorTableNames.USER_IDENTITY, MsunHisMirrorTableNames.USER_IDENTITY_ACCOUNT);

        linkPair(builder, MsunHisMirrorTableNames.YK_INSTOCK, MsunHisMirrorTableNames.YK_INSTOCK_DETAIL);
        linkToAll(builder, MsunHisMirrorTableNames.SYNC_BATCH,
                MsunHisMirrorTableNames.YK_INSTOCK, MsunHisMirrorTableNames.YK_INSTOCK_DETAIL);

        for (String flat : new String[] {
                MsunHisMirrorTableNames.DRUG_DICT, MsunHisMirrorTableNames.DICT_CATEGORY,
                MsunHisMirrorTableNames.SUPPLIER, MsunHisMirrorTableNames.PRODUCER,
                MsunHisMirrorTableNames.MERGE_STOCK, MsunHisMirrorTableNames.DRUG_BATCH_STOCK
        })
        {
            linkToAll(builder, MsunHisMirrorTableNames.SYNC_BATCH, flat);
        }

        LINKED_TABLES = freeze(builder);
    }

    private MsunHisMirrorTableLinkage()
    {
    }

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
