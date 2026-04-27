package com.scminterface.framework.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;

/**
 * ZS 订单转发控制器（透明代理）。
 * 客户端只需变更访问路径前缀，原有入参与回参处理逻辑保持不变。
 */
@Api(tags = "ZS订单转发")
@RestController
@RequestMapping("/api/scm/zs/order")
public class ZSOrderController
{
    private static final Logger log = LoggerFactory.getLogger(ZSOrderController.class);

    private static final String ZS_BASE_URL = "http://106.53.83.190:8088";
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 通配转发：
     * 客户端原请求 http://106.53.83.190:8088/{path}
     * 可替换为      http://本服务/api/scm/zs/order/{path}
     */
    @ApiOperation("透明转发到ZS服务")
    @RequestMapping("/**")
    public ResponseEntity<byte[]> forward(HttpServletRequest request, @RequestBody(required = false) byte[] body)
    {
        String requestUri = request.getRequestURI();
        String prefix = request.getContextPath() + "/api/scm/zs/order";
        String relativePath = requestUri.startsWith(prefix) ? requestUri.substring(prefix.length()) : "";
        if (!StringUtils.hasText(relativePath))
        {
            relativePath = "/";
        }
        String query = request.getQueryString();
        String targetUrl = ZS_BASE_URL + relativePath + (StringUtils.hasText(query) ? "?" + query : "");
        if (!StringUtils.hasText(targetUrl))
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("目标地址不能为空".getBytes());
        }
        String safeTargetUrl = Objects.requireNonNull(targetUrl);

        HttpMethod method = HttpMethod.resolve(request.getMethod());
        if (method == null)
        {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("不支持的HTTP方法".getBytes());
        }

        HttpHeaders headers = copyRequestHeaders(request);
        headers.remove(HttpHeaders.HOST);
        headers.remove(HttpHeaders.CONTENT_LENGTH);

        try
        {
            log.info("ZS转发开始，method={}, targetUrl={}", method, targetUrl);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                safeTargetUrl,
                method,
                new org.springframework.http.HttpEntity<>(body, headers),
                byte[].class
            );
            log.info("ZS转发成功，targetUrl={}, status={}", targetUrl, response.getStatusCodeValue());
            return ResponseEntity.status(response.getStatusCode())
                .headers(filterResponseHeaders(response.getHeaders()))
                .body(response.getBody());
        }
        catch (HttpStatusCodeException e)
        {
            log.warn("ZS转发返回错误状态，targetUrl={}, status={}", targetUrl, e.getRawStatusCode());
            HttpHeaders errorHeaders = e.getResponseHeaders() != null ? filterResponseHeaders(e.getResponseHeaders()) : new HttpHeaders();
            return ResponseEntity.status(e.getStatusCode())
                .headers(errorHeaders)
                .body(e.getResponseBodyAsByteArray());
        }
        catch (ResourceAccessException e)
        {
            log.error("ZS转发异常，targetUrl={}", targetUrl, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(("转发失败: " + e.getMessage()).getBytes());
        }
    }

    private HttpHeaders copyRequestHeaders(HttpServletRequest request)
    {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null)
        {
            return headers;
        }
        while (headerNames.hasMoreElements())
        {
            String name = headerNames.nextElement();
            Enumeration<String> values = request.getHeaders(name);
            if (values == null)
            {
                continue;
            }
            headers.put(name, Collections.list(values));
        }
        return headers;
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders source)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(source);
        headers.remove(HttpHeaders.TRANSFER_ENCODING);
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        return headers;
    }
}
