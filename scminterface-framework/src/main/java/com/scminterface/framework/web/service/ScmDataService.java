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
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.web.mapper.ScmMaterialMapper;

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
}

