package com.scminterface.customer.msun.spd.sync.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 众阳镜像 m_* → SPD 主数据 fd_* / sys_user 写入。
 */
@Mapper
public interface MsunSpdMasterSyncMapper
{
    List<Map<String, Object>> listMirrorRowsByBatch(
            @Param("table") String table,
            @Param("hospitalKey") String hospitalKey,
            @Param("tenantId") String tenantId,
            @Param("activeEnv") String activeEnv,
            @Param("syncBatchNo") String syncBatchNo);

    Long selectFdDepartmentIdByHisId(@Param("tenantId") String tenantId, @Param("hisId") String hisId);

    Long selectFdSupplierIdByHisId(@Param("tenantId") String tenantId, @Param("hisId") String hisId);

    Long selectFdFactoryIdByHisId(@Param("tenantId") String tenantId, @Param("hisId") String hisId);

    Long selectFdWarehouseCategoryIdByHisId(@Param("tenantId") String tenantId, @Param("hisId") String hisId);

    Long selectFdUnitIdByHisUnitId(@Param("tenantId") String tenantId, @Param("hisUnitId") String hisUnitId);

    Long selectSysUserIdByHisIdentity(@Param("customerId") String customerId, @Param("hisIdentityId") String hisIdentityId);

    int upsertFdDepartment(Map<String, Object> row);

    int updateFdDepartmentParent(
            @Param("tenantId") String tenantId,
            @Param("hisDeptId") String hisDeptId,
            @Param("hisParentId") String hisParentId);

    int upsertFdSupplier(Map<String, Object> row);

    int upsertFdFactory(Map<String, Object> row);

    int upsertFdWarehouseCategory(Map<String, Object> row);

    int upsertFdUnit(Map<String, Object> row);

    int upsertFdMaterial(Map<String, Object> row);

    int upsertSysUser(Map<String, Object> row);

    int insertSysUserDepartmentIfAbsent(Map<String, Object> row);
}
