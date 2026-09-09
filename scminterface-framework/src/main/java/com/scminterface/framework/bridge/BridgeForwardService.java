package com.scminterface.framework.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.scminterface.common.bridge.BridgeAckRequest;
import com.scminterface.common.bridge.BridgeConstants;
import com.scminterface.common.bridge.BridgeInvokeRequest;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.config.properties.BridgeProperties;
import com.scminterface.framework.config.properties.PublicInterfaceProperties;

/**
 * 院内前置机：将 bridge 请求透传到云端（不解析业务 payload）
 */
@Service
public class BridgeForwardService
{
    private static final Logger log = LoggerFactory.getLogger(BridgeForwardService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PublicInterfaceProperties publicInterfaceProperties;

    @Autowired
    private BridgeProperties bridgeProperties;

    public AjaxResult forwardInvoke(BridgeInvokeRequest body)
    {
        return postJson("/invoke", body);
    }

    public AjaxResult forwardPull(String hospitalCode, String tenantId, Integer limit)
    {
        String url = buildUrl("/pull");
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("hospitalCode", hospitalCode == null ? "" : hospitalCode);
        if (StringUtils.isNotEmpty(tenantId))
        {
            b.queryParam("tenantId", tenantId);
        }
        if (limit != null)
        {
            b.queryParam("limit", limit);
        }
        try
        {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            log.info("桥透传 pull: {}", b.toUriString());
            ResponseEntity<AjaxResult> resp = restTemplate.exchange(b.toUriString(), HttpMethod.GET, entity, AjaxResult.class);
            return resp.getBody() != null ? resp.getBody() : AjaxResult.error("云端无响应");
        }
        catch (Exception e)
        {
            log.error("桥透传 pull 失败: {}", e.getMessage(), e);
            return AjaxResult.error("桥透传失败：" + e.getMessage());
        }
    }

    public AjaxResult forwardAck(BridgeAckRequest body)
    {
        return postJson("/ack", body);
    }

    private AjaxResult postJson(String subPath, Object body)
    {
        String url = buildUrl(subPath);
        try
        {
            HttpHeaders headers = authHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            log.info("桥透传 POST: {}", url);
            ResponseEntity<AjaxResult> resp = restTemplate.exchange(url, HttpMethod.POST, entity, AjaxResult.class);
            return resp.getBody() != null ? resp.getBody() : AjaxResult.error("云端无响应");
        }
        catch (Exception e)
        {
            log.error("桥透传失败 {}: {}", url, e.getMessage(), e);
            return AjaxResult.error("桥透传失败：" + e.getMessage());
        }
    }

    private HttpHeaders authHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isNotEmpty(bridgeProperties.getToken()))
        {
            headers.set(BridgeConstants.HEADER_BRIDGE_TOKEN, bridgeProperties.getToken());
        }
        return headers;
    }

    private String buildUrl(String subPath)
    {
        String base = publicInterfaceProperties.getUrl();
        if (base == null || base.isEmpty() || base.contains("公网IP"))
        {
            throw new IllegalStateException("公网interface URL未配置");
        }
        if (!base.endsWith("/"))
        {
            base += "/";
        }
        String cloudPath = bridgeProperties.getCloudPath();
        if (cloudPath.startsWith("/"))
        {
            cloudPath = cloudPath.substring(1);
        }
        if (cloudPath.endsWith("/"))
        {
            cloudPath = cloudPath.substring(0, cloudPath.length() - 1);
        }
        if (!subPath.startsWith("/"))
        {
            subPath = "/" + subPath;
        }
        return base + cloudPath + subPath;
    }
}
