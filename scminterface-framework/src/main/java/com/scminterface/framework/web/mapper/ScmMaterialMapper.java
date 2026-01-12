package com.scminterface.framework.web.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SCM产品证件Mapper接口
 * 
 * @author scminterface
 */
@Mapper
public interface ScmMaterialMapper
{
    /**
     * 根据供应商名称查询供应商ID
     * 
     * @param supplierName 供应商名称
     * @return 供应商信息
     */
    Map<String, Object> selectSupplierByName(@Param("supplierName") String supplierName);

    /**
     * 根据生产厂家名称查询厂家ID
     * 
     * @param manufacturerName 生产厂家名称
     * @return 厂家ID
     */
    Long selectManufacturerByName(@Param("manufacturerName") String manufacturerName);

    /**
     * 检查供应商是否已有该耗材（供应商ID + 产品名称、规格、型号、价格相同）
     * 
     * @param supplierId 供应商ID
     * @param materialName 产品名称
     * @param specification 规格
     * @param model 型号
     * @param price 价格
     * @return 存在的记录数
     */
    int checkSupplierMaterialExists(@Param("supplierId") Long supplierId,
                                    @Param("materialName") String materialName,
                                    @Param("specification") String specification,
                                    @Param("model") String model,
                                    @Param("price") java.math.BigDecimal price);

    /**
     * 查询material_id
     * 
     * @param params 包含materialName, specification, model等
     * @return material_id
     */
    Long getMaterialId(Map<String, Object> params);

    /**
     * 插入material_dict
     * 
     * @param material 物资信息
     * @return 结果
     */
    int insertMaterial(Map<String, Object> material);
}

