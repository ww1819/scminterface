package com.scminterface.framework.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.framework.config.properties.PublicInterfaceProperties;

/**
 * HTTP客户端工具类
 * 用于调用公网interface接口
 * 
 * @author scminterface
 */
@Component
public class HttpClientUtils
{
    private static final Logger log = LoggerFactory.getLogger(HttpClientUtils.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PublicInterfaceProperties publicInterfaceProperties;

    /**
     * 调用公网interface接口推送档案数据
     * 
     * @param data 档案数据
     * @return 响应结果
     */
    public AjaxResult pushMaterialArchive(Object data)
    {
        String url = publicInterfaceProperties.getUrl();
        if (url == null || url.isEmpty() || url.contains("公网IP"))
        {
            log.error("公网interface URL未配置或配置不正确");
            return AjaxResult.error("公网interface URL未配置");
        }

        // 确保URL以/结尾
        if (!url.endsWith("/"))
        {
            url += "/";
        }
        url += "api/scm/pushMaterialArchive";

        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<Object> requestEntity = new HttpEntity<>(data, headers);

            log.info("调用公网interface接口: {}", url);
            ResponseEntity<AjaxResult> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    AjaxResult.class
            );

            AjaxResult result = response.getBody();
            log.info("公网interface接口响应: {}", result);
            return result != null ? result : AjaxResult.error("接口返回为空");
        }
        catch (RestClientException e)
        {
            log.error("调用公网interface接口异常: {}", e.getMessage(), e);
            return AjaxResult.error("调用公网interface接口失败: " + e.getMessage());
        }
        catch (Exception e)
        {
            log.error("调用公网interface接口发生未知异常: {}", e.getMessage(), e);
            return AjaxResult.error("调用公网interface接口失败: " + e.getMessage());
        }
    }
}

