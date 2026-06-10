package com.scminterface.customer.msun.spd.billpush.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SPD 出库/退库单据查询与 HIS 推送标志回写。
 */
@Mapper
public interface MsunSpdBillPushMapper
{
    List<Map<String, Object>> selectBillEntryRows(Map<String, Object> query);

    long countBillEntryRows(Map<String, Object> query);

    Map<String, Object> selectBillById(@Param("tenantId") String tenantId, @Param("billId") Long billId);

    List<Map<String, Object>> selectEntriesByBillId(@Param("tenantId") String tenantId, @Param("billId") Long billId);

    Map<String, Object> selectWarehouseById(@Param("tenantId") String tenantId, @Param("id") Long id);

    Map<String, Object> selectDepartmentById(@Param("tenantId") String tenantId, @Param("id") Long id);

    Map<String, Object> selectSupplierById(@Param("tenantId") String tenantId, @Param("id") Long id);

    Map<String, Object> selectMaterialById(@Param("tenantId") String tenantId, @Param("id") Long id);

    Map<String, Object> selectDepInventoryById(@Param("tenantId") String tenantId, @Param("id") Long id);

    Map<String, Object> selectBillEntryHisStockById(
            @Param("tenantId") String tenantId, @Param("entryId") Long entryId);

    Map<String, Object> selectOutboundEntryHisStockByDepInventoryId(
            @Param("tenantId") String tenantId, @Param("depInventoryId") Long depInventoryId);

    int updateEntryHisPrepare(Map<String, Object> row);

    int updateEntryHisPharmacyStock(Map<String, Object> row);

    int updateEntryHisStockIds(Map<String, Object> row);

    int updateEntryHisPushResult(Map<String, Object> row);

    int updateEntryHisPushStatus(Map<String, Object> row);

    int updateBillHisPushStatus(Map<String, Object> row);

    int updateDepInventoryHisStock(Map<String, Object> row);
}
