package com.scminterface.framework.web.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSON;
import com.scminterface.common.bridge.BridgeAckRequest;
import com.scminterface.common.bridge.BridgeConstants;
import com.scminterface.common.bridge.BridgeInvokeRequest;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.bridge.BridgeActionDispatcher;
import com.scminterface.framework.bridge.BridgeInboxService;
import com.scminterface.framework.config.properties.BridgeProperties;

/**
 * 云端稳态桥入口（公网 interface / scm 实例）。院内前置机透传至此。
 */
@RestController
@RequestMapping("/api/cloud/spd/bridge/v1")
public class CloudSpdBridgeController
{
    @Autowired
    private BridgeActionDispatcher bridgeActionDispatcher;

    @Autowired
    private BridgeInboxService bridgeInboxService;

    @Autowired
    private BridgeProperties bridgeProperties;

    @PostMapping("/invoke")
    public AjaxResult invoke(@RequestBody BridgeInvokeRequest body, HttpServletRequest request)
    {
        return bridgeActionDispatcher.invoke(body, request);
    }

    @GetMapping("/pull")
    public AjaxResult pull(@RequestParam("hospitalCode") String hospitalCode,
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @RequestParam(value = "limit", required = false) Integer limit,
        HttpServletRequest request)
    {
        return bridgeActionDispatcher.pull(hospitalCode, tenantId, limit, request);
    }

    @PostMapping("/ack")
    public AjaxResult ack(@RequestBody BridgeAckRequest body, HttpServletRequest request)
    {
        List<String> ids = BridgeActionDispatcher.normalizeMessageIds(body.getMessageId(), body.getMessageIds());
        return bridgeActionDispatcher.ack(body.getHospitalCode(), ids, request);
    }

    /**
     * 云端业务写入收件箱（供 SCM 侧事件投递；院内通过 pull 消费）
     */
    @PostMapping("/enqueue")
    public AjaxResult enqueue(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        AjaxResult auth = checkToken(request);
        if (auth != null)
        {
            return auth;
        }
        String hospitalCode = body.get("hospitalCode") != null ? String.valueOf(body.get("hospitalCode")).trim() : null;
        String tenantId = body.get("tenantId") != null ? String.valueOf(body.get("tenantId")).trim() : null;
        String msgType = body.get("msgType") != null ? String.valueOf(body.get("msgType")).trim() : null;
        Object payload = body.get("payload");
        String payloadJson = payload == null ? null
            : (payload instanceof String ? (String) payload : JSON.toJSONString(payload));
        try
        {
            String id = bridgeInboxService.enqueue(hospitalCode, tenantId, msgType, payloadJson, "api");
            return AjaxResult.success(id);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    private AjaxResult checkToken(HttpServletRequest request)
    {
        String expected = bridgeProperties.getToken();
        if (StringUtils.isEmpty(expected))
        {
            return null;
        }
        String got = request != null ? request.getHeader(BridgeConstants.HEADER_BRIDGE_TOKEN) : null;
        if (!expected.equals(got))
        {
            return AjaxResult.error(401, "桥接令牌无效");
        }
        return null;
    }
}
