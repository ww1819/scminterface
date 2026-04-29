package com.scminterface.framework.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import com.scminterface.common.core.domain.PurchaseOrderDTO;
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

    /**
     * 调用公网interface接口推送采购订单
     *
     * @param orders 采购订单列表
     * @return 响应结果
     */
    public AjaxResult pushPurchaseOrders(java.util.List<PurchaseOrderDTO> orders)
    {
        String url = publicInterfaceProperties.getUrl();
        if (url == null || url.isEmpty() || url.contains("公网IP"))
        {
            log.error("公网interface URL未配置或配置不正确");
            return AjaxResult.error("公网interface URL未配置");
        }

        if (!url.endsWith("/"))
        {
            url += "/";
        }
        url += "api/scm/pushPurchaseOrders";

        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<java.util.List<PurchaseOrderDTO>> requestEntity = new HttpEntity<>(orders, headers);

            log.info("调用公网interface接口推送采购订单: {}", url);
            ResponseEntity<AjaxResult> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    AjaxResult.class
            );

            AjaxResult result = response.getBody();
            log.info("公网interface接口响应(采购订单): {}", result);
            return result != null ? result : AjaxResult.error("接口返回为空");
        }
        catch (RestClientException e)
        {
            log.error("调用公网interface接口推送采购订单异常: {}", e.getMessage(), e);
            return AjaxResult.error("调用公网interface接口失败: " + e.getMessage());
        }
        catch (Exception e)
        {
            log.error("调用公网interface接口推送采购订单发生未知异常: {}", e.getMessage(), e);
            return AjaxResult.error("调用公网interface接口失败: " + e.getMessage());
        }
    }

    /**
     * 转发调用公网接口：查询配送单（支持输入码/配送单号）
     */
    public AjaxResult querySpdDelivery(String keyword)
    {
        String baseUrl = publicInterfaceProperties.getUrl();
        if (baseUrl == null || baseUrl.isEmpty() || baseUrl.contains("公网IP"))
        {
            log.error("公网interface URL未配置或配置不正确");
            return AjaxResult.error("公网interface URL未配置");
        }
        if (!baseUrl.endsWith("/"))
        {
            baseUrl += "/";
        }
        try
        {
            String url = baseUrl + "api/scm/spd/delivery/query?keyword="
                + URLEncoder.encode(keyword == null ? "" : keyword, "UTF-8");
            log.info("调用公网interface接口查询配送单: {}", url);
            ResponseEntity<AjaxResult> response = restTemplate.exchange(url, HttpMethod.GET, null, AjaxResult.class);
            AjaxResult result = response.getBody();
            log.info("公网interface接口响应(配送单查询): {}", result);
            return result != null ? result : AjaxResult.error("接口返回为空");
        }
        catch (Exception e)
        {
            log.error("调用公网interface接口查询配送单异常: {}", e.getMessage(), e);
            return AjaxResult.error("调用公网interface接口失败: " + e.getMessage());
        }
    }

    /**
     * 转发调用公网接口：下载配送单XML
     */
    public ResponseEntity<byte[]> downloadSpdDeliveryXml(String deliveryNo)
    {
        String baseUrl = publicInterfaceProperties.getUrl();
        if (baseUrl == null || baseUrl.isEmpty() || baseUrl.contains("公网IP"))
        {
            return ResponseEntity.badRequest().body("公网interface URL未配置".getBytes(StandardCharsets.UTF_8));
        }
        if (!baseUrl.endsWith("/"))
        {
            baseUrl += "/";
        }
        try
        {
            String url = baseUrl + "api/scm/spd/delivery/download?deliveryNo="
                + URLEncoder.encode(deliveryNo == null ? "" : deliveryNo, "UTF-8");
            log.info("调用公网interface接口下载配送单: {}", url);
            return restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
        }
        catch (Exception e)
        {
            log.error("调用公网interface接口下载配送单异常: {}", e.getMessage(), e);
            return ResponseEntity.status(502).body(("调用公网interface接口失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}

