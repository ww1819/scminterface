package com.scminterface.customer.hengsuiThird.his.mapper;

import java.util.Date;
import org.apache.ibatis.annotations.Param;

/**
 * 计费抓取批次日志（与 SPD his_charge_fetch_batch 对齐）。
 */
public interface HisChargeFetchBatchSyncMapper
{
    int insertFetchBatch(
        @Param("id") String id,
        @Param("tenantId") String tenantId,
        @Param("chargeKind") String chargeKind,
        @Param("windowStart") Date windowStart,
        @Param("windowEnd") Date windowEnd,
        @Param("insertedCount") int insertedCount,
        @Param("skippedCount") int skippedCount,
        @Param("driftCount") int driftCount,
        @Param("remark") String remark,
        @Param("createBy") String createBy,
        @Param("createTime") Date createTime);
}
