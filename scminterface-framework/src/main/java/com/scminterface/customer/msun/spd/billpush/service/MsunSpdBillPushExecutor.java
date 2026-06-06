package com.scminterface.customer.msun.spd.billpush.service;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.msun.spd.billpush.mapper.MsunSpdBillPushMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * SPD 库单据查询与 HIS 标志回写（独立 Bean 保证 {@link DataSource} 切面生效）。
 */
@Service
public class MsunSpdBillPushExecutor
{
    private final MsunSpdBillPushMapper billPushMapper;

    public MsunSpdBillPushExecutor(MsunSpdBillPushMapper billPushMapper)
    {
        this.billPushMapper = billPushMapper;
    }

    @DataSource(DataSourceType.SPD)
    public List<Map<String, Object>> listBillEntryRows(Map<String, Object> query)
    {
        return billPushMapper.selectBillEntryRows(query);
    }

    @DataSource(DataSourceType.SPD)
    public long countBillEntryRows(Map<String, Object> query)
    {
        return billPushMapper.countBillEntryRows(query);
    }

    @DataSource(DataSourceType.SPD)
    public Map<String, Object> selectBillById(String tenantId, Long billId)
    {
        return billPushMapper.selectBillById(tenantId, billId);
    }

    @DataSource(DataSourceType.SPD)
    public List<Map<String, Object>> selectEntriesByBillId(String tenantId, Long billId)
    {
        return billPushMapper.selectEntriesByBillId(tenantId, billId);
    }

    @DataSource(DataSourceType.SPD)
    public Map<String, Object> selectWarehouseById(String tenantId, Long id)
    {
        return billPushMapper.selectWarehouseById(tenantId, id);
    }

    @DataSource(DataSourceType.SPD)
    public Map<String, Object> selectDepartmentById(String tenantId, Long id)
    {
        return billPushMapper.selectDepartmentById(tenantId, id);
    }

    @DataSource(DataSourceType.SPD)
    public Map<String, Object> selectSupplierById(String tenantId, Long id)
    {
        return billPushMapper.selectSupplierById(tenantId, id);
    }

    @DataSource(DataSourceType.SPD)
    public Map<String, Object> selectMaterialById(String tenantId, Long id)
    {
        return billPushMapper.selectMaterialById(tenantId, id);
    }

    @DataSource(DataSourceType.SPD)
    public Map<String, Object> selectDepInventoryById(String tenantId, Long id)
    {
        return billPushMapper.selectDepInventoryById(tenantId, id);
    }

    @DataSource(DataSourceType.SPD)
    public int updateEntryHisPrepare(Map<String, Object> row)
    {
        return billPushMapper.updateEntryHisPrepare(row);
    }

    @DataSource(DataSourceType.SPD)
    public int updateEntryHisPharmacyStock(Map<String, Object> row)
    {
        return billPushMapper.updateEntryHisPharmacyStock(row);
    }

    @DataSource(DataSourceType.SPD)
    public int updateEntryHisStockIds(Map<String, Object> row)
    {
        return billPushMapper.updateEntryHisStockIds(row);
    }

    @DataSource(DataSourceType.SPD)
    public int updateEntryHisPushResult(Map<String, Object> row)
    {
        return billPushMapper.updateEntryHisPushResult(row);
    }

    @DataSource(DataSourceType.SPD)
    public int updateEntryHisPushStatus(Map<String, Object> row)
    {
        return billPushMapper.updateEntryHisPushStatus(row);
    }

    @DataSource(DataSourceType.SPD)
    public int updateBillHisPushStatus(Map<String, Object> row)
    {
        return billPushMapper.updateBillHisPushStatus(row);
    }

    @DataSource(DataSourceType.SPD)
    public int updateDepInventoryHisStock(Map<String, Object> row)
    {
        return billPushMapper.updateDepInventoryHisStock(row);
    }
}
