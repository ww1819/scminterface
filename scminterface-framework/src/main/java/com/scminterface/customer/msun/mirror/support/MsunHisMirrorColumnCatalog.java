package com.scminterface.customer.msun.mirror.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 镜像表实列目录：落库前仅保留库中已有列，HIS 新增回参字段不进 INSERT（仍在 raw_item_json）。
 */
@Component
public class MsunHisMirrorColumnCatalog
{
    private static final Logger log = LoggerFactory.getLogger(MsunHisMirrorColumnCatalog.class);

    private final ObjectProvider<DataSource> spdDataSourceProvider;
    private final ConcurrentHashMap<String, Set<String>> columnsByTable = new ConcurrentHashMap<>();
    private final Set<String> warnedKeys = ConcurrentHashMap.newKeySet();

    public MsunHisMirrorColumnCatalog(
            @Qualifier("spdDataSource") ObjectProvider<DataSource> spdDataSourceProvider)
    {
        this.spdDataSourceProvider = spdDataSourceProvider;
    }

    /** schema 补列后清除缓存，下次按最新列过滤。 */
    public void invalidate(String tableName)
    {
        if (tableName != null && !tableName.isEmpty())
        {
            columnsByTable.remove(tableName);
        }
    }

    /**
     * 去掉表中不存在的列；完整 HIS 行仍在 {@code raw_item_json}。
     * 读列失败时不做过滤（避免误伤），由原 SQL 暴露问题。
     */
    public Map<String, Object> retainExistingColumns(String tableName, Map<String, Object> row)
    {
        if (row == null || row.isEmpty() || tableName == null || tableName.isEmpty())
        {
            return row;
        }
        Set<String> existing = columnsOf(tableName);
        if (existing.isEmpty())
        {
            return row;
        }
        Map<String, Object> kept = new LinkedHashMap<>(row.size());
        Set<String> skipped = new LinkedHashSet<>();
        for (Map.Entry<String, Object> e : row.entrySet())
        {
            String col = e.getKey();
            if (existing.contains(col))
            {
                kept.put(col, e.getValue());
            }
            else
            {
                skipped.add(col);
            }
        }
        if (!skipped.isEmpty())
        {
            String warnKey = tableName + ":" + skipped;
            if (warnedKeys.add(warnKey))
            {
                log.warn("HIS回参含未建镜像列，已忽略写入（完整 JSON 仍在 raw_item_json）；"
                                + "请补 01_table.sql / 02_column.sql 后重启。table={} skipped={}",
                        tableName, skipped);
            }
        }
        return kept;
    }

    public Set<String> columnsOf(String tableName)
    {
        if (tableName == null || tableName.isEmpty())
        {
            return Collections.emptySet();
        }
        Set<String> cached = columnsByTable.get(tableName);
        if (cached != null)
        {
            return cached;
        }
        synchronized (columnsByTable)
        {
            cached = columnsByTable.get(tableName);
            if (cached != null)
            {
                return cached;
            }
            Set<String> loaded = loadColumns(tableName);
            if (!loaded.isEmpty())
            {
                Set<String> frozen = Collections.unmodifiableSet(loaded);
                columnsByTable.put(tableName, frozen);
                return frozen;
            }
            return Collections.emptySet();
        }
    }

    private Set<String> loadColumns(String tableName)
    {
        DataSource ds = spdDataSourceProvider.getIfAvailable();
        if (ds == null)
        {
            log.warn("SPD 数据源不可用，无法加载镜像表列目录: {}", tableName);
            return Collections.emptySet();
        }
        String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        Set<String> cols = new HashSet<>();
        try (Connection conn = ds.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    String name = rs.getString(1);
                    if (name != null && !name.isEmpty())
                    {
                        cols.add(name);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            log.warn("加载镜像表列目录失败 table={} err={}", tableName, ex.getMessage());
            return Collections.emptySet();
        }
        return cols;
    }
}
