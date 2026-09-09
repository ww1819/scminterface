package com.scminterface.framework.web.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HospitalMaterialArchiveMapper
{
    Map<String, Object> selectArchiveByHospitalAndSpdMaterial(@Param("hospitalCode") String hospitalCode,
        @Param("spdMaterialId") String spdMaterialId);

    Map<String, Object> selectArchiveById(@Param("id") String id);

    List<Map<String, Object>> selectArchivesForPull(@Param("hospitalCode") String hospitalCode,
        @Param("scmSupplierCode") String scmSupplierCode,
        @Param("keyword") String keyword);

    int insertArchive(Map<String, Object> row);

    int updateArchive(Map<String, Object> row);

    int insertPushBatch(Map<String, Object> row);

    int updatePushBatch(Map<String, Object> row);

    int insertPushItem(Map<String, Object> row);

    int insertChangeLog(Map<String, Object> row);

    int insertModifyApply(Map<String, Object> row);

    Map<String, Object> selectModifyApplyById(@Param("id") String id);

    List<Map<String, Object>> selectPendingApplies(@Param("archiveId") String archiveId,
        @Param("scmSupplierCode") String scmSupplierCode);

    int voidPendingApplies(@Param("archiveId") String archiveId,
        @Param("auditBy") String auditBy,
        @Param("auditRemark") String auditRemark);

    int updateModifyApply(Map<String, Object> row);

    List<Map<String, Object>> selectModifyApplies(@Param("hospitalCode") String hospitalCode,
        @Param("scmSupplierCode") String scmSupplierCode,
        @Param("applyStatus") String applyStatus);
}
