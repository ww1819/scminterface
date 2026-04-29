package com.scminterface.framework.web.mapper;

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
}
