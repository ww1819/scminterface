package com.scminterface.framework.web.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.core.domain.PurchaseOrderDTO;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.utils.HttpClientUtils;
import com.scminterface.framework.web.mapper.SpdPurchaseOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SPD 采购订单发布服务
 */
@Service
public class SpdOrderPublishService
{
    private static final Logger log = LoggerFactory.getLogger(SpdOrderPublishService.class);

    @Autowired
    private SpdPurchaseOrderMapper spdPurchaseOrderMapper;

    @Autowired
    private HttpClientUtils httpClientUtils;

    /**
     * 根据订单ID列表查询SPD订单并推送到SCM
     *
     * @param ids 订单ID列表
     * @return 推送结果
     */
    @DataSource(DataSourceType.SPD)
    public AjaxResult publishOrders(List<Long> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return AjaxResult.error("订单ID列表不能为空");
        }

        log.info("开始发布SPD采购订单，ID列表: {}", ids);

        List<Map<String, Object>> orderList = spdPurchaseOrderMapper.selectPurchaseOrdersByIds(ids);
        if (orderList == null || orderList.isEmpty())
        {
            return AjaxResult.error("未查询到对应的采购订单");
        }

        List<Long> orderIds = new ArrayList<>();
        for (Map<String, Object> order : orderList)
        {
            Object idObj = order.get("id");
            if (idObj instanceof Number)
            {
                orderIds.add(((Number) idObj).longValue());
            }
        }

        List<Map<String, Object>> entryList = spdPurchaseOrderMapper.selectPurchaseOrderEntriesByOrderIds(orderIds);

        Map<Long, List<Map<String, Object>>> entryMap = new HashMap<>();
        for (Map<String, Object> entry : entryList)
        {
            Object parentIdObj = entry.get("parentId");
            if (parentIdObj instanceof Number)
            {
                Long parentId = ((Number) parentIdObj).longValue();
                entryMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(entry);
            }
        }

        List<PurchaseOrderDTO> dtoList = new ArrayList<>();
        for (Map<String, Object> order : orderList)
        {
            PurchaseOrderDTO dto = new PurchaseOrderDTO();

            Long orderId = getLong(order, "id");
            dto.setOrderId(orderId);
            dto.setOrderNo(getString(order, "orderNo"));
            dto.setPlanNo(getString(order, "planNo"));
            dto.setSupplierId(getLong(order, "supplierId"));
            dto.setSupplierName(trimToNull(getString(order, "supplierName")));
            dto.setHospitalName(trimToNull(getString(order, "hospitalName")));
            dto.setWarehouseId(getLong(order, "warehouseId"));
            dto.setWarehouseName(trimToNull(getString(order, "warehouseName")));
            dto.setDepartmentId(getLong(order, "departmentId"));
            dto.setOrderDate((java.util.Date) order.get("orderDate"));
            dto.setTotalAmount(getBigDecimal(order, "totalAmount"));
            dto.setOrderStatus(getString(order, "orderStatus"));
            dto.setRemark(getString(order, "remark"));

            List<Map<String, Object>> orderEntries = entryMap.getOrDefault(orderId, new ArrayList<>());
            List<PurchaseOrderDTO.PurchaseOrderItem> items = new ArrayList<>();
            for (Map<String, Object> entry : orderEntries)
            {
                PurchaseOrderDTO.PurchaseOrderItem item = new PurchaseOrderDTO.PurchaseOrderItem();
                item.setEntryId(getLong(entry, "entryId"));
                item.setMaterialId(getLong(entry, "materialId"));
                item.setMaterialCode(getString(entry, "materialCode"));
                item.setMaterialName(getString(entry, "materialName"));
                item.setSpecification(getString(entry, "specification"));
                item.setUnit(getString(entry, "unit"));
                item.setQuantity(getBigDecimal(entry, "quantity"));
                item.setUnitPrice(getBigDecimal(entry, "unitPrice"));
                item.setAmount(getBigDecimal(entry, "amount"));
                item.setRemark(getString(entry, "remark"));
                items.add(item);
            }
            dto.setItems(items);
            dtoList.add(dto);
        }

        log.info("组装完成采购订单DTO列表，数量: {}", dtoList.size());
        return httpClientUtils.pushPurchaseOrders(dtoList);
    }

    private String getString(Map<String, Object> map, String key)
    {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static String trimToNull(String s)
    {
        if (s == null)
        {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Long getLong(Map<String, Object> map, String key)
    {
        Object value = map.get(key);
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        if (value != null)
        {
            try
            {
                return Long.parseLong(value.toString());
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        return null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key)
    {
        Object value = map.get(key);
        if (value == null)
        {
            return null;
        }
        if (value instanceof BigDecimal)
        {
            return (BigDecimal) value;
        }
        if (value instanceof Number)
        {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try
        {
            return new BigDecimal(value.toString());
        }
        catch (Exception e)
        {
            return null;
        }
    }
}

