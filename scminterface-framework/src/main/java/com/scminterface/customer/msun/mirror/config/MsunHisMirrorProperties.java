package com.scminterface.customer.msun.mirror.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 众阳 HIS 查询回参落镜像库开关。
 */
@ConfigurationProperties(prefix = "scminterface.vendor.msun.mirror")
public class MsunHisMirrorProperties
{
    /** 是否将查询接口 hisBody.data 写入 SPD 库中的 m_* 镜像表（须已手工建表） */
    private boolean enabled = true;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
