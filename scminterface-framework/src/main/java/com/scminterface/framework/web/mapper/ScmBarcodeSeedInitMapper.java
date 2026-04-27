package com.scminterface.framework.web.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 条码种子表初始化（与 scm 库 scm_barcode_seed 一致，供接口落单后补种子行）
 */
@Mapper
public interface ScmBarcodeSeedInitMapper
{
    int ensureTenantSeed(@Param("id") String id,
        @Param("tenantId") String tenantId,
        @Param("warehouseId") String warehouseId,
        @Param("highLowFlag") String highLowFlag);

    int ensureZsCustomerSeed(@Param("id") String id,
        @Param("tenantId") String tenantId,
        @Param("zsCustomerId") String zsCustomerId,
        @Param("warehouseId") String warehouseId,
        @Param("highLowFlag") String highLowFlag);
}
