package com.scminterface.customer.msun.mirror.support;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 从 classpath 加载众阳 HIS 镜像表 DDL（01 建表 / 02 增量字段）。
 */
@Component
public class MsunHisMirrorSchemaScriptLoader
{
    private static final Logger log = LoggerFactory.getLogger(MsunHisMirrorSchemaScriptLoader.class);

    private static final Pattern SEGMENT_SPLIT = Pattern.compile("\\R/\\R");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+`([^`]+)`",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ADD_COLUMN_CALL = Pattern.compile(
            "CALL\\s+add_mirror_column\\('([^']+)',\\s*'([^']+)',\\s*'([^']*)',\\s*'([^']*)',\\s*'([^']*)'\\)",
            Pattern.CASE_INSENSITIVE);

    private static final String TABLE_SCRIPT = "sql/mysql/msun_his_mirror/01_table.sql";
    private static final String COLUMN_SCRIPT = "sql/mysql/msun_his_mirror/02_column.sql";

    private volatile Map<String, String> createSqlByTable;
    private volatile Map<String, List<MsunHisMirrorColumnPatch>> columnPatchesByTable;

    public Map<String, String> getCreateSqlByTable()
    {
        ensureLoaded();
        return createSqlByTable;
    }

    public List<MsunHisMirrorColumnPatch> getColumnPatches(String tableName)
    {
        ensureLoaded();
        List<MsunHisMirrorColumnPatch> list = columnPatchesByTable.get(tableName);
        return list == null ? Collections.emptyList() : list;
    }

    public String getCreateSql(String tableName)
    {
        ensureLoaded();
        return createSqlByTable.get(tableName);
    }

    private void ensureLoaded()
    {
        if (createSqlByTable != null)
        {
            return;
        }
        synchronized (this)
        {
            if (createSqlByTable != null)
            {
                return;
            }
            createSqlByTable = parseCreateScripts(readClasspath(TABLE_SCRIPT));
            columnPatchesByTable = parseColumnPatches(readClasspath(COLUMN_SCRIPT));
            log.info("众阳HIS镜像DDL已加载：建表 {} 张，增量字段覆盖 {} 张表",
                    createSqlByTable.size(), columnPatchesByTable.size());
        }
    }

    private static Map<String, String> parseCreateScripts(String sqlText)
    {
        Map<String, String> map = new LinkedHashMap<>();
        for (String segment : splitSegments(sqlText))
        {
            if (isBlank(segment))
            {
                continue;
            }
            Matcher m = CREATE_TABLE.matcher(segment);
            if (m.find())
            {
                map.put(m.group(1), segment.trim());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, List<MsunHisMirrorColumnPatch>> parseColumnPatches(String sqlText)
    {
        Map<String, List<MsunHisMirrorColumnPatch>> map = new HashMap<>();
        for (String segment : splitSegments(sqlText))
        {
            if (isBlank(segment))
            {
                continue;
            }
            Matcher m = ADD_COLUMN_CALL.matcher(segment);
            while (m.find())
            {
                MsunHisMirrorColumnPatch patch = new MsunHisMirrorColumnPatch(
                        m.group(1), m.group(2), m.group(3), m.group(4), m.group(5));
                map.computeIfAbsent(patch.getTableName(), k -> new ArrayList<>()).add(patch);
            }
        }
        Map<String, List<MsunHisMirrorColumnPatch>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<MsunHisMirrorColumnPatch>> e : map.entrySet())
        {
            frozen.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    private static String readClasspath(String path)
    {
        try
        {
            ClassPathResource res = new ClassPathResource(path);
            if (!res.exists())
            {
                log.warn("众阳HIS镜像DDL脚本不存在: {}", path);
                return "";
            }
            try (InputStream in = res.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("读取镜像DDL失败: " + path, ex);
        }
    }

    private static List<String> splitSegments(String sqlText)
    {
        if (sqlText == null || sqlText.isEmpty())
        {
            return Collections.emptyList();
        }
        String[] parts = SEGMENT_SPLIT.split(sqlText);
        List<String> list = new ArrayList<>(parts.length);
        Collections.addAll(list, parts);
        return list;
    }

    private static boolean isBlank(String s)
    {
        return s == null || s.trim().isEmpty();
    }
}
