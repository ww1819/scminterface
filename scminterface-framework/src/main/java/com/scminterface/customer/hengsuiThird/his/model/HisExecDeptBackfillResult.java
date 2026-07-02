package com.scminterface.customer.hengsuiThird.his.model;

/**
 * 历史计费镜像补全执行科室结果。
 */
public class HisExecDeptBackfillResult
{
    private int updatedCount;
    private int skippedCount;
    private int hisMissingExecCount;
    private int notFoundCount;
    private int unifiedSyncedCount;

    public int getUpdatedCount()
    {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount)
    {
        this.updatedCount = updatedCount;
    }

    public int getSkippedCount()
    {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount)
    {
        this.skippedCount = skippedCount;
    }

    public int getHisMissingExecCount()
    {
        return hisMissingExecCount;
    }

    public void setHisMissingExecCount(int hisMissingExecCount)
    {
        this.hisMissingExecCount = hisMissingExecCount;
    }

    public int getNotFoundCount()
    {
        return notFoundCount;
    }

    public void setNotFoundCount(int notFoundCount)
    {
        this.notFoundCount = notFoundCount;
    }

    public int getUnifiedSyncedCount()
    {
        return unifiedSyncedCount;
    }

    public void setUnifiedSyncedCount(int unifiedSyncedCount)
    {
        this.unifiedSyncedCount = unifiedSyncedCount;
    }
}
