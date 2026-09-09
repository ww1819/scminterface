package com.scminterface.framework.bridge;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.scminterface.common.bridge.BridgeActions;
import com.scminterface.common.bridge.BridgeConstants;
import com.scminterface.common.bridge.BridgeInvokeRequest;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.core.domain.PurchaseOrderDTO;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.config.properties.BridgeProperties;
import com.scminterface.framework.domain.zs.ScmDeliveryListItemRow;
import com.scminterface.framework.web.service.ScmSupplierInterfaceService;
import com.scminterface.framework.web.service.SpdDeliveryService;
import com.scminterface.framework.web.service.SpdOrderPublishService;

/**
 * 云端 action 分发（业务逻辑只在此扩展，前置机透传不解析）
 */
@Service
public class BridgeActionDispatcher
{
    @Autowired
    private BridgeProperties bridgeProperties;

    @Autowired
    private ScmSupplierInterfaceService scmSupplierInterfaceService;

    @Autowired
    private SpdDeliveryService spdDeliveryService;

    @Autowired
    private SpdOrderPublishService spdOrderPublishService;

    @Autowired
    private BridgeInboxService bridgeInboxService;

    public AjaxResult invoke(BridgeInvokeRequest req, HttpServletRequest httpRequest)
    {
        AjaxResult auth = checkToken(httpRequest);
        if (auth != null)
        {
            return auth;
        }
        if (req == null || StringUtils.isEmpty(req.getAction()))
        {
            return AjaxResult.error("action 不能为空");
        }
        String action = req.getAction().trim();
        Map<String, Object> payload = req.getPayload() != null ? req.getPayload() : new HashMap<>();
        String hospitalCode = firstNonEmpty(req.getHospitalCode(), str(payload.get("hospitalCode")));
        String tenantId = firstNonEmpty(req.getTenantId(), str(payload.get("spdTenantId")), str(payload.get("tenantId")));

        try
        {
            switch (action)
            {
                case BridgeActions.SUPPLIER_LIST_BY_HOSPITAL:
                    if (StringUtils.isEmpty(hospitalCode))
                    {
                        return AjaxResult.error("hospitalCode 不能为空");
                    }
                    return AjaxResult.success(scmSupplierInterfaceService.listSuppliersByHospital(hospitalCode));

                case BridgeActions.SUPPLIER_PROFILE:
                {
                    String supplierCode = firstNonEmpty(str(payload.get("supplierCode")), str(payload.get("scmSupplierCode")));
                    if (StringUtils.isEmpty(hospitalCode) || StringUtils.isEmpty(supplierCode))
                    {
                        return AjaxResult.error("hospitalCode、supplierCode 不能为空");
                    }
                    String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
                    Map<String, Object> data = scmSupplierInterfaceService.buildSupplierProfile(
                        hospitalCode, supplierCode, tenantId, ip, "bridge");
                    if (data == null)
                    {
                        return AjaxResult.error("未找到平台供应商：" + supplierCode);
                    }
                    return AjaxResult.success(data);
                }

                case BridgeActions.DELIVERY_QUERY:
                {
                    String keyword = str(payload.get("keyword"));
                    if (StringUtils.isEmpty(keyword))
                    {
                        return AjaxResult.error("keyword 不能为空");
                    }
                    List<ScmDeliveryListItemRow> list = spdDeliveryService.listByKeyword(keyword);
                    return AjaxResult.success(list);
                }

                case BridgeActions.DELIVERY_DOWNLOAD:
                {
                    String deliveryNo = str(payload.get("deliveryNo"));
                    if (StringUtils.isEmpty(deliveryNo))
                    {
                        return AjaxResult.error("deliveryNo 不能为空");
                    }
                    String xml = spdDeliveryService.buildDeliveryXml(deliveryNo, tenantId);
                    Map<String, Object> file = new LinkedHashMap<>();
                    file.put("deliveryNo", deliveryNo);
                    file.put("contentType", "application/xml;charset=UTF-8");
                    file.put("fileName", "zs-delivery-" + deliveryNo.trim().replaceAll("[\\\\/:*?\"<>|]", "_") + ".xml");
                    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
                    file.put("contentBase64", Base64.getEncoder().encodeToString(bytes));
                    file.put("contentText", xml);
                    return AjaxResult.success(file);
                }

                case BridgeActions.ORDER_PUBLISH_PAYLOAD:
                {
                    Object ordersObj = payload.get("orders");
                    if (ordersObj == null)
                    {
                        return AjaxResult.error("payload.orders 不能为空");
                    }
                    List<PurchaseOrderDTO> orders = JSON.parseArray(JSON.toJSONString(ordersObj), PurchaseOrderDTO.class);
                    if (orders == null || orders.isEmpty())
                    {
                        return AjaxResult.error("订单列表为空");
                    }
                    return spdOrderPublishService.publishOrdersFromPayload(orders);
                }

                default:
                    return AjaxResult.error("未知 action：" + action);
            }
        }
        catch (Exception e)
        {
            return AjaxResult.error("桥分发失败：" + e.getMessage());
        }
    }

    public AjaxResult pull(String hospitalCode, String tenantId, Integer limit, HttpServletRequest httpRequest)
    {
        AjaxResult auth = checkToken(httpRequest);
        if (auth != null)
        {
            return auth;
        }
        if (StringUtils.isEmpty(hospitalCode))
        {
            return AjaxResult.error("hospitalCode 不能为空");
        }
        int lim = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        return AjaxResult.success(bridgeInboxService.pullPending(hospitalCode, tenantId, lim));
    }

    public AjaxResult ack(String hospitalCode, List<String> messageIds, HttpServletRequest httpRequest)
    {
        AjaxResult auth = checkToken(httpRequest);
        if (auth != null)
        {
            return auth;
        }
        if (StringUtils.isEmpty(hospitalCode))
        {
            return AjaxResult.error("hospitalCode 不能为空");
        }
        if (messageIds == null || messageIds.isEmpty())
        {
            return AjaxResult.error("messageIds 不能为空");
        }
        int n = bridgeInboxService.ack(hospitalCode, messageIds);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("acked", n);
        return AjaxResult.success(data);
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

    private static String str(Object o)
    {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static String firstNonEmpty(String... vals)
    {
        if (vals == null)
        {
            return null;
        }
        for (String v : vals)
        {
            if (StringUtils.isNotEmpty(v))
            {
                return v.trim();
            }
        }
        return null;
    }

    public static List<String> normalizeMessageIds(String one, List<String> many)
    {
        List<String> out = new ArrayList<>();
        if (many != null)
        {
            for (String id : many)
            {
                if (StringUtils.isNotEmpty(id))
                {
                    out.add(id.trim());
                }
            }
        }
        if (StringUtils.isNotEmpty(one))
        {
            out.add(one.trim());
        }
        return out;
    }
}
