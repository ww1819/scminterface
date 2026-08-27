package com.scminterface.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 公网 SCM 应用（非库），用于订单落库后回调微信通知。
 */
@Component
@ConfigurationProperties(prefix = "scminterface.scm.app")
public class ScmAppNotifyProperties
{
    /** SCM 应用根地址，例如 http://127.0.0.1:8888 ，无尾斜杠 */
    private String url;

    /** 与 SCM scm.wechat.mp.internal-api-key 一致 */
    private String apiKey;

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public boolean isConfigured()
    {
        return url != null && !url.trim().isEmpty() && apiKey != null && !apiKey.trim().isEmpty();
    }

    public String normalizedUrl()
    {
        if (url == null)
        {
            return null;
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
