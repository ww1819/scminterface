package com.scminterface.framework.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SPD 采购订单Mapper
 */
@Mapper
public interface SpdPurchaseOrderMapper
{
    /**
     * 根据ID列表查询采购订单主表
     *
     * @param ids 订单ID列表
     * @return 订单主表数据列表
     */
    List<Map<String, Object>> selectPurchaseOrdersByIds(@Param("ids") List<Long> ids);

    /**
     * 根据订单ID列表查询采购订单明细
     *
     * @param orderIds 订单ID列表
     * @return 订单明细数据列表
     */
    List<Map<String, Object>> selectPurchaseOrderEntriesByOrderIds(@Param("orderIds") List<Long> orderIds);
}

