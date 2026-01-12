package com.scminterface.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP客户端配置
 * 
 * @author scminterface
 */
@Configuration
public class HttpClientConfig
{
    @Bean
    public RestTemplate restTemplate()
    {
        return new RestTemplate(clientHttpRequestFactory());
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(30000); // 读取超时时间30秒
        factory.setConnectTimeout(10000); // 连接超时时间10秒
        return factory;
    }
}

