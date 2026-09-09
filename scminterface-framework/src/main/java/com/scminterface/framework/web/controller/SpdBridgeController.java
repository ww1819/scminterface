package com.scminterface.framework.web.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scminterface.common.bridge.BridgeAckRequest;
import com.scminterface.common.bridge.BridgeInvokeRequest;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.framework.bridge.BridgeActionDispatcher;
import com.scminterface.framework.bridge.BridgeForwardService;
import com.scminterface.framework.config.properties.BridgeProperties;

/**
 * 院内稳态桥：SPD 调用入口。默认透传云端；mode=local 时本机分发。
 */
@RestController
@RequestMapping("/api/bridge/v1")
public class SpdBridgeController
{
    @Autowired
    private BridgeProperties bridgeProperties;

    @Autowired
    private BridgeForwardService bridgeForwardService;

    @Autowired
    private BridgeActionDispatcher bridgeActionDispatcher;

    @PostMapping("/invoke")
    public AjaxResult invoke(@RequestBody BridgeInvokeRequest body, HttpServletRequest request)
    {
        if (bridgeProperties.isLocalMode())
        {
            return bridgeActionDispatcher.invoke(body, request);
        }
        return bridgeForwardService.forwardInvoke(body);
    }

    @GetMapping("/pull")
    public AjaxResult pull(@RequestParam("hospitalCode") String hospitalCode,
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @RequestParam(value = "limit", required = false) Integer limit,
        HttpServletRequest request)
    {
        if (bridgeProperties.isLocalMode())
        {
            return bridgeActionDispatcher.pull(hospitalCode, tenantId, limit, request);
        }
        return bridgeForwardService.forwardPull(hospitalCode, tenantId, limit);
    }

    @PostMapping("/ack")
    public AjaxResult ack(@RequestBody BridgeAckRequest body, HttpServletRequest request)
    {
        List<String> ids = BridgeActionDispatcher.normalizeMessageIds(body.getMessageId(), body.getMessageIds());
        if (bridgeProperties.isLocalMode())
        {
            return bridgeActionDispatcher.ack(body.getHospitalCode(), ids, request);
        }
        BridgeAckRequest forward = new BridgeAckRequest();
        forward.setHospitalCode(body.getHospitalCode());
        forward.setTenantId(body.getTenantId());
        forward.setMessageIds(ids);
        return bridgeForwardService.forwardAck(forward);
    }
}
