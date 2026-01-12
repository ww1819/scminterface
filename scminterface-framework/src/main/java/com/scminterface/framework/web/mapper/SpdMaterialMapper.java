package com.scminterface.framework.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SPD耗材产品Mapper接口
 * 
 * @author scminterface
 */
@Mapper
public interface SpdMaterialMapper
{
    /**
     * 根据供应商ID查询耗材产品列表
     * 
     * @param supplierId 供应商ID
     * @return 耗材产品列表
     */
    List<Map<String, Object>> selectMaterialListBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 根据供应商ID查询供应商信息
     * 
     * @param supplierId 供应商ID
     * @return 供应商信息
     */
    Map<String, Object> selectSupplierById(@Param("supplierId") Long supplierId);
}

