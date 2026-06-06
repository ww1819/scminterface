package com.scminterface.customer.msun.mirror.service;

import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorColumnPatch;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorSchemaScriptLoader;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorTableNames;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * SPD 库镜像表结构检测与按需建表/补列（独立 Bean 以保证 {@link DataSource} 切面生效）。
 */
@Service
public class MsunHisMirrorSchemaExecutor
{
    private static final Logger log = LoggerFactory.getLogger(MsunHisMirrorSchemaExecutor.class);

    private final ObjectProvider<javax.sql.DataSource> spdDataSourceProvider;
    private final MsunHisMirrorSchemaScriptLoader scriptLoader;

    public MsunHisMirrorSchemaExecutor(
            @Qualifier("spdDataSource") ObjectProvider<javax.sql.DataSource> spdDataSourceProvider,
            MsunHisMirrorSchemaScriptLoader scriptLoader)
    {
        this.spdDataSourceProvider = spdDataSourceProvider;
        this.scriptLoader = scriptLoader;
    }

    @com.scminterface.common.annotation.DataSource(DataSourceType.SPD)
    public void ensureTable(String tableName)
    {
        javax.sql.DataSource ds = spdDataSourceProvider.getIfAvailable();
        if (ds == null)
        {
            throw new IllegalStateException("SPD 数据源未配置，无法自动建镜像表");
        }
        String createSql = scriptLoader.getCreateSql(tableName);
        if (createSql == null)
        {
            throw new IllegalArgumentException("未找到镜像表建表脚本: " + tableName);
        }
        try (Connection conn = ds.getConnection())
        {
            conn.setAutoCommit(true);
            if (!tableExists(conn, tableName))
            {
                executeStatement(conn, createSql);
                log.info("众阳HIS镜像表已自动创建: {}", tableName);
            }
            applyColumnPatches(conn, tableName, scriptLoader.getColumnPatches(tableName));
            if (MsunHisMirrorTableNames.PUSH_LOG.equals(tableName))
            {
                widenPushLogMsgColumn(conn);
            }
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("镜像表结构补全失败 [" + tableName + "]: " + ex.getMessage(), ex);
        }
    }

    private static void applyColumnPatches(Connection conn, String tableName, List<MsunHisMirrorColumnPatch> patches)
            throws Exception
    {
        if (patches == null || patches.isEmpty())
        {
            return;
        }
        if (!tableExists(conn, tableName))
        {
            return;
        }
        for (MsunHisMirrorColumnPatch patch : patches)
        {
            if (!columnExists(conn, tableName, patch.getColumnName()))
            {
                executeStatement(conn, buildAlterSql(conn, tableName, patch));
                log.info("众阳HIS镜像表已自动补列: {}.{}", tableName, patch.getColumnName());
            }
        }
    }

    private static String buildAlterSql(Connection conn, String tableName, MsunHisMirrorColumnPatch patch)
            throws Exception
    {
        StringBuilder sql = new StringBuilder(160);
        sql.append("ALTER TABLE `").append(tableName).append("` ADD COLUMN `")
                .append(patch.getColumnName()).append("` ").append(patch.getColumnType());
        if (!containsNotNull(patch.getColumnType()))
        {
            sql.append(" NULL");
        }
        sql.append(" COMMENT '").append(escapeComment(patch.getColumnComment())).append("'");
        String after = patch.getAfterColumn();
        if (after != null && !after.isEmpty() && columnExists(conn, tableName, after))
        {
            sql.append(" AFTER `").append(after).append('`');
        }
        return sql.toString();
    }

    private static String escapeComment(String comment)
    {
        return comment == null ? "" : comment.replace("'", "''");
    }

    private static boolean tableExists(Connection conn, String tableName) throws Exception
    {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws Exception
    {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static void executeStatement(Connection conn, String sql) throws Exception
    {
        try (Statement st = conn.createStatement())
        {
            st.execute(sql);
        }
    }

    private static boolean containsNotNull(String columnType)
    {
        return columnType != null && columnType.toUpperCase().contains("NOT NULL");
    }

    /** 已有库 push_msg 多为 VARCHAR(500)，HIS 失败原因过长；启动后首次推送时自动扩为 TEXT。 */
    private static void widenPushLogMsgColumn(Connection conn) throws Exception
    {
        if (!tableExists(conn, MsunHisMirrorTableNames.PUSH_LOG))
        {
            return;
        }
        String sql = "SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'push_msg'";
        try (PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, MsunHisMirrorTableNames.PUSH_LOG);
            try (ResultSet rs = ps.executeQuery())
            {
                if (!rs.next())
                {
                    return;
                }
                String dataType = rs.getString(1);
                if (dataType != null && "varchar".equalsIgnoreCase(dataType.trim()))
                {
                    executeStatement(conn, "ALTER TABLE `" + MsunHisMirrorTableNames.PUSH_LOG
                            + "` MODIFY COLUMN `push_msg` TEXT NULL COMMENT '失败原因'");
                    log.info("众阳HIS推送日志表 push_msg 已扩为 TEXT");
                }
            }
        }
    }
}
