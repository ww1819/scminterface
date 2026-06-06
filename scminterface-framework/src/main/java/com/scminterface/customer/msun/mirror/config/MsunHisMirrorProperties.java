package com.scminterface.customer.msun.mirror.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 众阳 HIS 查询回参落镜像库开关。
 */
@ConfigurationProperties(prefix = "scminterface.vendor.msun.mirror")
public class MsunHisMirrorProperties
{
    /** 是否将查询接口 hisBody.data 写入 SPD 库中的 m_* 镜像表 */
    private boolean enabled = true;

    /**
     * 调用/探针/查询前按需检测并创建缺失的镜像表及增量字段（仅创建实际用到的表）。
     */
    private boolean autoSchema = true;

    /** 自动建表失败时是否中断当前请求（false 仅记日志，与 scm.schema.fail-on-error 一致） */
    private boolean schemaFailOnError = false;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isAutoSchema()
    {
        return autoSchema;
    }

    public void setAutoSchema(boolean autoSchema)
    {
        this.autoSchema = autoSchema;
    }

    public boolean isSchemaFailOnError()
    {
        return schemaFailOnError;
    }

    public void setSchemaFailOnError(boolean schemaFailOnError)
    {
        this.schemaFailOnError = schemaFailOnError;
    }
}
