package com.scminterface.framework.web.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * ZS 第三方推送订单落库
 */
@Mapper
public interface ZsTpOrderMapper
{
    /**
     * 按第三方服务标识 + 单号查询未删除主表 id
     */
    String selectActiveOrderIdByCustomerAndDh(@Param("customer") String customer, @Param("dh") String dh);

    int insertOrder(Map<String, Object> row);

    int insertDetail(Map<String, Object> row);

    int softDeleteOrderById(@Param("id") String id, @Param("delBy") String delBy);

    int softDeleteDetailsByOrderId(@Param("orderId") String orderId, @Param("delBy") String delBy);
}
