package com.scminterface.framework.web.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.core.domain.MaterialArchiveDTO;
import com.scminterface.common.core.domain.PurchaseOrderDTO;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.web.mapper.ScmMaterialMapper;
import com.scminterface.framework.web.mapper.ScmOrderMapper;

/**
 * SCM数据服务
 * 
 * @author scminterface
 */
@Service
public class ScmDataService
{
    private static final Logger log = LoggerFactory.getLogger(ScmDataService.class);

    @Autowired
    private ScmMaterialMapper scmMaterialMapper;

    @Autowired
    private ScmOrderMapper scmOrderMapper;

    /**
     * 保存示例数据
     * 
     * @param data 数据
     */
    public void saveExampleData(Map<String, Object> data)
    {
        // TODO: 实现真实的数据库保存逻辑
        System.out.println("保存数据到SCM数据库: " + data);
    }

    /**
     * 保存档案数据到SCM
     * 
     * @param archiveDTO 档案数据
     * @return 保存结果统计
     */
    @DataSource(DataSourceType.SCM)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveMaterialArchive(MaterialArchiveDTO archiveDTO)
    {
        log.info("开始保存档案数据，供应商: {}", archiveDTO.getSupplierName());

        // 1. 按供应商名称匹配SCM中的供应商
        Map<String, Object> supplier = scmMaterialMapper.selectSupplierByName(archiveDTO.getSupplierName());
        if (supplier == null || supplier.isEmpty())
        {
            log.error("SCM中不存在该供应商: {}", archiveDTO.getSupplierName());
            throw new RuntimeException("SCM中不存在该供应商: " + archiveDTO.getSupplierName());
        }

        Long supplierId = ((Number) supplier.get("supplierId")).longValue();
        log.info("匹配到供应商，供应商ID: {}", supplierId);

        // 2. 遍历档案列表，保存数据
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        List<String> skipMessages = new java.util.ArrayList<>();
        List<String> errorMessages = new java.util.ArrayList<>();

        List<MaterialArchiveDTO.MaterialArchiveItem> archiveList = archiveDTO.getArchiveList();
        if (archiveList == null || archiveList.isEmpty())
        {
            log.warn("档案列表为空");
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", 0);
            result.put("skipCount", 0);
            result.put("errorCount", 0);
            result.put("message", "档案列表为空");
            return result;
        }

        for (MaterialArchiveDTO.MaterialArchiveItem item : archiveList)
        {
            try
            {
                // 检查该供应商是否已有该耗材（产品名称、规格、型号、价格相同）
                int supplierExistsCount = scmMaterialMapper.checkSupplierMaterialExists(
                    supplierId,
                    item.getMaterialName(),
                    item.getSpecification(),
                    item.getModel(),
                    item.getPrice()
                );

                if (supplierExistsCount > 0)
                {
                    skipCount++;
                    skipMessages.add(String.format("供应商已有该耗材: %s-%s-%s-%s", 
                        item.getMaterialName(), item.getSpecification(), item.getModel(), item.getPrice()));
                    log.debug("供应商已有该耗材，跳过: {}", item.getMaterialName());
                    continue;
                }

                // 插入material_dict表
                Map<String, Object> material = new HashMap<>();
                material.put("materialName", item.getMaterialName());
                material.put("specification", item.getSpecification());
                material.put("model", item.getModel());
                material.put("unit", item.getUnit());
                material.put("supplierId", supplierId);
                
                // 根据manufacturerName查询manufacturer_id
                if (item.getManufacturerName() != null && !item.getManufacturerName().isEmpty())
                {
                    Long manufacturerId = scmMaterialMapper.selectManufacturerByName(item.getManufacturerName());
                    if (manufacturerId != null)
                    {
                        material.put("manufacturerId", manufacturerId);
                    }
                }
                
                material.put("purchasePrice", item.getPrice());
                material.put("createBy", "system");

                int insertResult = scmMaterialMapper.insertMaterial(material);
                if (insertResult > 0)
                {
                    successCount++;
                    log.debug("保存成功: {}", item.getMaterialName());
                }
                else
                {
                    errorCount++;
                    errorMessages.add(String.format("保存失败: %s", item.getMaterialName()));
                    log.error("保存失败: {}", item.getMaterialName());
                }
            }
            catch (Exception e)
            {
                errorCount++;
                errorMessages.add(String.format("保存异常: %s - %s", item.getMaterialName(), e.getMessage()));
                log.error("保存档案异常: {}", item.getMaterialName(), e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("skipCount", skipCount);
        result.put("errorCount", errorCount);
        result.put("totalCount", archiveList.size());
        result.put("skipMessages", skipMessages);
        result.put("errorMessages", errorMessages);

        log.info("保存完成，成功: {}, 跳过: {}, 失败: {}, 总计: {}", 
            successCount, skipCount, errorCount, archiveList.size());

        return result;
    }

    /**
     * 保存采购订单到SCM
     *
     * @param orders 订单列表
     * @return 结果统计
     */
    @DataSource(DataSourceType.SCM)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> savePurchaseOrders(java.util.List<PurchaseOrderDTO> orders)
    {
        Map<String, Object> result = new HashMap<>();
        if (orders == null || orders.isEmpty())
        {
            result.put("totalCount", 0);
            result.put("successCount", 0);
            result.put("message", "订单列表为空");
            return result;
        }

        int successCount = 0;
        int errorCount = 0;
        java.util.List<String> errorMessages = new java.util.ArrayList<>();

        for (PurchaseOrderDTO order : orders)
        {
            try
            {
                if (order.getOrderNo() == null || order.getOrderNo().trim().isEmpty())
                {
                    throw new RuntimeException("订单号为空");
                }

                Long orderId = scmOrderMapper.selectOrderIdByOrderNo(order.getOrderNo());

                String supplierName = trimToNull(order.getSupplierName());
                if (supplierName == null)
                {
                    throw new RuntimeException("无对应供应商（SPD 订单未带出供应商名称，无法与 SCM 匹配）");
                }
                String hospitalName = trimToNull(order.getHospitalName());
                if (hospitalName == null)
                {
                    throw new RuntimeException("无对应医院（SPD 订单未带出医院/客户名称，无法与 SCM 匹配）");
                }

                Map<String, Object> scmSupplier = scmMaterialMapper.selectSupplierByName(supplierName);
                if (scmSupplier == null || scmSupplier.isEmpty())
                {
                    throw new RuntimeException("无对应供应商（SCM 中未找到名称：" + supplierName + "）");
                }
                Long scmSupplierId = ((Number) scmSupplier.get("supplierId")).longValue();

                Map<String, Object> scmHospital = scmMaterialMapper.selectHospitalByName(hospitalName);
                if (scmHospital == null || scmHospital.isEmpty())
                {
                    throw new RuntimeException("无对应医院（SCM 中未找到名称：" + hospitalName + "）");
                }
                Long scmHospitalId = ((Number) scmHospital.get("hospitalId")).longValue();

                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("orderNo", order.getOrderNo());
                orderMap.put("hospitalId", scmHospitalId);
                orderMap.put("supplierId", scmSupplierId);
                orderMap.put("warehouseName", trimToNull(order.getWarehouseName()));
                orderMap.put("orderDate", order.getOrderDate());
                orderMap.put("orderAmount", order.getTotalAmount());
                // SPD“已审核”到 SCM 默认转为“待接收”
                orderMap.put("orderStatus", "0");
                orderMap.put("applyDept", order.getDepartmentId() != null ? String.valueOf(order.getDepartmentId()) : null);
                orderMap.put("remark", order.getRemark());
                orderMap.put("createBy", "spd-sync");
                orderMap.put("updateBy", "spd-sync");

                if (orderId == null)
                {
                    scmOrderMapper.insertOrder(orderMap);
                    Object idObj = orderMap.get("orderId");
                    if (idObj instanceof Number)
                    {
                        orderId = ((Number) idObj).longValue();
                    }
                    else
                    {
                        orderId = null;
                    }
                    if (orderId == null)
                    {
                        orderId = scmOrderMapper.selectOrderIdByOrderNo(order.getOrderNo());
                    }
                }
                else
                {
                    orderMap.put("orderId", orderId);
                    scmOrderMapper.updateOrder(orderMap);
                    scmOrderMapper.deleteOrderDetailsByOrderId(orderId);
                }

                if (orderId == null)
                {
                    throw new RuntimeException("订单主表写入失败: " + order.getOrderNo());
                }

                if (order.getItems() != null)
                {
                    for (PurchaseOrderDTO.PurchaseOrderItem item : order.getItems())
                    {
                        if (item.getMaterialId() == null)
                        {
                            throw new RuntimeException("订单明细物资ID为空: " + order.getOrderNo());
                        }

                        Map<String, Object> detailMap = new HashMap<>();
                        detailMap.put("orderId", orderId);
                        detailMap.put("materialId", item.getMaterialId());
                        detailMap.put("materialCode", item.getMaterialCode());
                        detailMap.put("materialName", item.getMaterialName());
                        detailMap.put("specification", item.getSpecification());
                        detailMap.put("model", null);
                        detailMap.put("unit", item.getUnit());
                        detailMap.put("purchasePrice", item.getUnitPrice());
                        detailMap.put("orderQuantity", item.getQuantity());
                        detailMap.put("remainingQuantity", item.getQuantity() != null ? item.getQuantity().intValue() : 0);
                        detailMap.put("amount", item.getAmount());
                        detailMap.put("manufacturerName", item.getManufacturerName());
                        detailMap.put("registerNo", item.getRegisterNo());
                        detailMap.put("remark", item.getRemark());
                        detailMap.put("createBy", "spd-sync");

                        scmOrderMapper.insertOrderDetail(detailMap);
                    }
                }

                successCount++;
                log.info("采购订单写入SCM成功，orderNo: {}", order.getOrderNo());
            }
            catch (Exception e)
            {
                errorCount++;
                String orderNo = order != null ? order.getOrderNo() : "unknown";
                errorMessages.add(orderNo + ": " + e.getMessage());
                log.error("采购订单写入SCM失败，orderNo: {}", orderNo, e);
            }
        }

        result.put("totalCount", orders.size());
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("errorMessages", errorMessages);
        result.put("message", String.format("采购订单接收完成，总计: %d, 成功: %d, 失败: %d", orders.size(), successCount, errorCount));
        return result;
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
}

