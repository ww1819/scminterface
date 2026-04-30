package com.scminterface.framework.web.controller;

import java.util.List;
import java.util.Map;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.core.domain.PurchaseOrderDTO;
import com.scminterface.framework.web.service.SpdOrderPublishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SPD 采购订单发布控制器
 */
@Api(tags = "SPD采购订单发布")
@RestController
@RequestMapping("/api/spd/order")
public class SpdOrderController
{
    private static final Logger log = LoggerFactory.getLogger(SpdOrderController.class);

    @Autowired
    private SpdOrderPublishService spdOrderPublishService;

    /**
     * 发布选中的采购订单到SCM
     *
     * @param params 请求参数，包含订单ID列表 ids
     * @return 结果
     */
    @ApiOperation("发布采购订单到SCM")
    @PostMapping("/publish")
    public AjaxResult publishOrders(@RequestBody Map<String, Object> params)
    {
        Object idsObj = params.get("ids");
        if (!(idsObj instanceof List))
        {
            return AjaxResult.error("参数错误，ids 必须为数组");
        }

        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) idsObj;
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (Object o : rawIds)
        {
            if (o instanceof Number)
            {
                ids.add(((Number) o).longValue());
            }
            else if (o != null)
            {
                try
                {
                    ids.add(Long.parseLong(o.toString()));
                }
                catch (NumberFormatException e)
                {
                    log.warn("无法解析订单ID: {}", o);
                }
            }
        }

        return spdOrderPublishService.publishOrders(ids);
    }

    /**
     * SPD 携带完整订单 JSON（与平台 {@link PurchaseOrderDTO} 一致），前置机只做校验并转发平台。
     */
    @ApiOperation("SPD体推送采购订单到SCM（前置机不查SPD库）")
    @PostMapping("/publishPayload")
    public AjaxResult publishOrdersPayload(@RequestBody List<PurchaseOrderDTO> orders)
    {
        return spdOrderPublishService.publishOrdersFromPayload(orders);
    }
}

