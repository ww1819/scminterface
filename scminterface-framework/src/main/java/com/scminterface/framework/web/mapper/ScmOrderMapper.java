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
     * 按 SPD 租户 + 订单号查订单 ID（幂等更新用；兼容 spd_tenant_id 为空时用 tenant_id 匹配）。
     */
    Long selectOrderIdByTenantAndOrderNo(
        @Param("spdTenantId") String spdTenantId,
        @Param("orderNo") String orderNo);

    /**
     * 按订单号查首条未删订单（仅用于迁移期冲突提示，新逻辑不用于更新）。
     *
     * @deprecated 请使用 {@link #selectOrderIdByTenantAndOrderNo}
     */
    @Deprecated
    Long selectOrderIdByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 读取订单租户/医院快照，用于拒绝跨租户覆盖。
     */
    Map<String, Object> selectOrderSnapshotByOrderId(@Param("orderId") Long orderId);

    /**
     * 新增订单主表
     */
    int insertOrder(Map<String, Object> order);

    /**
     * 更新订单主表
     */
    int updateOrder(Map<String, Object> order);

    /**
     * 删除订单明细
     */
    int deleteOrderDetailsByOrderId(@Param("orderId") Long orderId);

    /**
     * 插入订单明细
     */
    int insertOrderDetail(Map<String, Object> detail);
}
