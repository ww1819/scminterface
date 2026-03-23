package com.scminterface.framework.web.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SCM 订单写入 Mapper
 */
@Mapper
public interface ScmOrderMapper
{
    /**
     * 根据订单号查询订单ID
     *
     * @param orderNo 订单号
     * @return 订单ID
     */
    Long selectOrderIdByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 新增订单主表
     *
     * @param order 订单数据
     * @return 影响行数
     */
    int insertOrder(Map<String, Object> order);

    /**
     * 更新订单主表
     *
     * @param order 订单数据
     * @return 影响行数
     */
    int updateOrder(Map<String, Object> order);

    /**
     * 删除订单明细
     *
     * @param orderId 订单ID
     * @return 影响行数
     */
    int deleteOrderDetailsByOrderId(@Param("orderId") Long orderId);

    /**
     * 插入订单明细
     *
     * @param detail 明细数据
     * @return 影响行数
     */
    int insertOrderDetail(Map<String, Object> detail);
}

