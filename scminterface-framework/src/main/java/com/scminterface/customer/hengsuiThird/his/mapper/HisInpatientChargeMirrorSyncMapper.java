package com.scminterface.customer.hengsuiThird.his.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.scminterface.customer.hengsuiThird.his.model.HisIdFingerprintRow;
import com.scminterface.customer.hengsuiThird.his.model.HisInpatientChargeMirrorRow;

/**
 * 衡水三院：HIS 住院计费镜像写入（SPD 库 his_inpatient_charge_mirror）
 */
@Mapper
public interface HisInpatientChargeMirrorSyncMapper
{
    int insertBatch(@Param("list") List<HisInpatientChargeMirrorRow> list);

    List<HisIdFingerprintRow> selectFingerprintsByHisIds(@Param("tenantId") String tenantId, @Param("hisIds") List<String> hisIds);
}
