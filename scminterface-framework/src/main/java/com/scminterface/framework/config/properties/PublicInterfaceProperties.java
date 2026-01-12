package com.scminterface.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 公网接口配置属性
 * 
 * @author scminterface
 */
@Component
@ConfigurationProperties(prefix = "scminterface.public.interface")
public class PublicInterfaceProperties
{
    /** 公网接口URL */
    private String url;

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
}

