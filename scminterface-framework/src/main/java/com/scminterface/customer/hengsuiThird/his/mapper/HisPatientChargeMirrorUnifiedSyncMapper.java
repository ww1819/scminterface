package com.scminterface.customer.hengsuiThird.his.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.scminterface.customer.hengsuiThird.his.model.HisPatientChargeMirrorUnifiedRow;

/**
 * 衡水三院：HIS 计费镜像统一表写入（SPD 库 his_patient_charge_mirror_unified）
 */
@Mapper
public interface HisPatientChargeMirrorUnifiedSyncMapper
{
    int insertBatch(@Param("list") List<HisPatientChargeMirrorUnifiedRow> list);

    int updateInpatientExecDeptIfMissing(
        @Param("tenantId") String tenantId,
        @Param("hisChargeId") String hisChargeId,
        @Param("execDeptId") String execDeptId,
        @Param("execDeptName") String execDeptName,
        @Param("rowFingerprint") String rowFingerprint);

    int updateOutpatientExecDeptIfMissing(
        @Param("tenantId") String tenantId,
        @Param("hisChargeId") String hisChargeId,
        @Param("execDeptId") String execDeptId,
        @Param("execDeptName") String execDeptName,
        @Param("rowFingerprint") String rowFingerprint);

    int syncInpatientExecDeptFromMirror(@Param("tenantId") String tenantId);

    int syncOutpatientExecDeptFromMirror(@Param("tenantId") String tenantId);
}
