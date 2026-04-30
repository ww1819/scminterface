package com.scminterface.framework.web.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SCM 主数据：按编码解析医院/供应商主键（供 ZS 订单落库等）
 */
@Mapper
public interface ScmPartyLookupMapper
{
    /**
     * 按医院编码查询未删除医院的 hospital_id（字符串）
     */
    String selectHospitalIdByHospitalCode(@Param("hospitalCode") String hospitalCode);

    /**
     * 按供应商编码查询未删除供应商的 supplier_id（字符串）
     */
    String selectSupplierIdBySupplierCode(@Param("supplierCode") String supplierCode);

    /**
     * 医院与供应商在平台已建立有效关联（绑定+审核通过+启用）
     */
    int countHospitalSupplierActive(@Param("hospitalCode") String hospitalCode,
        @Param("supplierCode") String supplierCode);

    /**
     * 平台供应商主数据行（未删除）
     */
    Map<String, Object> selectScmSupplierRowByCode(@Param("supplierCode") String supplierCode);

    /**
     * 某医院编码下已关联的平台供应商简要列表
     */
    List<Map<String, Object>> selectSuppliersByHospitalCode(@Param("hospitalCode") String hospitalCode);

    int insertScmSupplierExportLog(@Param("id") String id,
        @Param("hospitalCode") String hospitalCode,
        @Param("supplierCode") String supplierCode,
        @Param("exportScope") String exportScope,
        @Param("spdTenantId") String spdTenantId,
        @Param("requestIp") String requestIp,
        @Param("createBy") String createBy);
}
