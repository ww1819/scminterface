package com.scminterface.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 稳态桥配置：forward=透传到公网云端；local=本机直接分发（联调/同机）
 */
@Component
@ConfigurationProperties(prefix = "scminterface.bridge")
public class BridgeProperties
{
    /** forward | local */
    private String mode = "forward";

    /** 相对公网基址的云端桥路径前缀（无首尾斜杠） */
    private String cloudPath = "api/cloud/spd/bridge/v1";

    /** 可选：与云端约定的共享密钥；空则不校验 */
    private String token = "";

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public String getCloudPath()
    {
        return cloudPath;
    }

    public void setCloudPath(String cloudPath)
    {
        this.cloudPath = cloudPath;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public boolean isLocalMode()
    {
        return "local".equalsIgnoreCase(mode);
    }
}
