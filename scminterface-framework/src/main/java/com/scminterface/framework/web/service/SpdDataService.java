package com.scminterface.framework.web.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.core.domain.MaterialArchiveDTO;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.web.mapper.SpdMaterialMapper;

/**
 * SPD数据服务
 * 
 * @author scminterface
 */
@Service
public class SpdDataService
{
    private static final Logger log = LoggerFactory.getLogger(SpdDataService.class);

    @Autowired
    private SpdMaterialMapper spdMaterialMapper;

    /**
     * 获取示例数据
     * 
     * @return 数据
     */
    public Map<String, Object> getExampleData()
    {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("message", "这是从SPD数据库查询的示例数据");
        data.put("timestamp", System.currentTimeMillis());
        // TODO: 实现真实的数据库查询逻辑
        return data;
    }

    /**
     * 根据供应商ID查询档案数据并转换为DTO
     * 
     * @param supplierId 供应商ID
     * @return 档案DTO
     */
    @DataSource(DataSourceType.SPD)
    public MaterialArchiveDTO pushMaterialArchive(Long supplierId)
    {
        log.info("开始查询供应商档案，供应商ID: {}", supplierId);

        // 查询供应商信息
        Map<String, Object> supplier = spdMaterialMapper.selectSupplierById(supplierId);
        if (supplier == null || supplier.isEmpty())
        {
            log.error("供应商不存在，供应商ID: {}", supplierId);
            throw new RuntimeException("供应商不存在");
        }

        // 查询该供应商的所有耗材产品
        List<Map<String, Object>> materialList = spdMaterialMapper.selectMaterialListBySupplierId(supplierId);
        if (materialList == null || materialList.isEmpty())
        {
            log.warn("供应商没有耗材产品，供应商ID: {}", supplierId);
            materialList = new ArrayList<>();
        }

        // 转换为DTO
        MaterialArchiveDTO dto = new MaterialArchiveDTO();
        dto.setSupplierId(supplierId);
        dto.setSupplierName((String) supplier.get("supplierName"));

        List<MaterialArchiveDTO.MaterialArchiveItem> archiveList = new ArrayList<>();
        for (Map<String, Object> material : materialList)
        {
            MaterialArchiveDTO.MaterialArchiveItem item = new MaterialArchiveDTO.MaterialArchiveItem();
            
            // 基本信息
            item.setMaterialName(getStringValue(material, "materialName"));
            item.setSpecification(getStringValue(material, "specification"));
            item.setModel(getStringValue(material, "model"));
            item.setPrice(getBigDecimalValue(material, "price"));
            
            // 证件信息
            item.setRegisterNo(getStringValue(material, "registerNo"));
            item.setRegisterName(getStringValue(material, "registerName"));
            item.setUdiCode(getStringValue(material, "udiCode"));
            item.setExpireDate(getDateValue(material, "expireDate"));
            
            // 价格信息
            item.setSalePrice(getBigDecimalValue(material, "salePrice"));
            item.setBidPrice(getBigDecimalValue(material, "bidPrice"));
            
            // 其他信息
            item.setUnit(getStringValue(material, "unit"));
            item.setManufacturerName(getStringValue(material, "manufacturerName"));
            item.setProductCategory(getStringValue(material, "productCategory"));
            item.setMedicalName(getStringValue(material, "medicalName"));
            item.setMedicalNo(getStringValue(material, "medicalNo"));
            item.setBrand(getStringValue(material, "brand"));
            item.setUseto(getStringValue(material, "useto"));
            item.setQuality(getStringValue(material, "quality"));
            item.setFunction(getStringValue(material, "function"));
            item.setIsWay(getStringValue(material, "isWay"));
            item.setCountryNo(getStringValue(material, "countryNo"));
            item.setCountryName(getStringValue(material, "countryName"));
            item.setDescription(getStringValue(material, "description"));
            item.setPinyinCode(getStringValue(material, "pinyinCode"));
            
            archiveList.add(item);
        }

        dto.setArchiveList(archiveList);
        log.info("查询完成，供应商: {}, 档案数量: {}", dto.getSupplierName(), archiveList.size());
        
        return dto;
    }

    /**
     * 获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key)
    {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取BigDecimal值
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key)
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
        return null;
    }

    /**
     * 获取Date值
     */
    private Date getDateValue(Map<String, Object> map, String key)
    {
        Object value = map.get(key);
        if (value == null)
        {
            return null;
        }
        if (value instanceof Date)
        {
            return (Date) value;
        }
        return null;
    }
}

