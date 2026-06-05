package com.scminterface.customer.msun.spd.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 众阳 HIS 镜像落库后同步至 SPD 主数据表（fd_* / sys_user）开关。
 */
@ConfigurationProperties(prefix = "scminterface.vendor.msun.spd-master-sync")
public class MsunSpdMasterSyncProperties
{
    /** 镜像 m_* 写入成功后是否 upsert 至 SPD 主数据表 */
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
