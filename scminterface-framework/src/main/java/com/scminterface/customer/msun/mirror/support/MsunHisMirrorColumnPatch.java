package com.scminterface.customer.msun.mirror.support;

/**
 * 镜像表增量字段定义（对应 02_column.sql 中 add_mirror_column 调用）。
 */
public final class MsunHisMirrorColumnPatch
{
    private final String tableName;
    private final String columnName;
    private final String columnType;
    private final String columnComment;
    private final String afterColumn;

    public MsunHisMirrorColumnPatch(
            String tableName,
            String columnName,
            String columnType,
            String columnComment,
            String afterColumn)
    {
        this.tableName = tableName;
        this.columnName = columnName;
        this.columnType = columnType;
        this.columnComment = columnComment;
        this.afterColumn = afterColumn;
    }

    public String getTableName()
    {
        return tableName;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public String getColumnType()
    {
        return columnType;
    }

    public String getColumnComment()
    {
        return columnComment;
    }

    public String getAfterColumn()
    {
        return afterColumn;
    }
}
