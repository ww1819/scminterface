package com.scminterface.customer.msun.mirror.support;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HIS 探针调用后的镜像落库与 SPD 主数据同步结果（供联调页展示）。
 */
public class MsunHisMirrorSyncOutcome
{
    private String apiCode;
    private String syncBatchNo;
    private boolean mirrorEnabled;
    private boolean spdSyncEnabled;
    private boolean spdDataSourceAvailable;
    private int mirrorRows;
    private int spdRows;
    private String mirrorSkippedReason;
    private String mirrorError;
    private String spdSyncError;
    private String spdNote;

    public String getApiCode()
    {
        return apiCode;
    }

    public void setApiCode(String apiCode)
    {
        this.apiCode = apiCode;
    }

    public String getSyncBatchNo()
    {
        return syncBatchNo;
    }

    public void setSyncBatchNo(String syncBatchNo)
    {
        this.syncBatchNo = syncBatchNo;
    }

    public boolean isMirrorEnabled()
    {
        return mirrorEnabled;
    }

    public void setMirrorEnabled(boolean mirrorEnabled)
    {
        this.mirrorEnabled = mirrorEnabled;
    }

    public boolean isSpdSyncEnabled()
    {
        return spdSyncEnabled;
    }

    public void setSpdSyncEnabled(boolean spdSyncEnabled)
    {
        this.spdSyncEnabled = spdSyncEnabled;
    }

    public boolean isSpdDataSourceAvailable()
    {
        return spdDataSourceAvailable;
    }

    public void setSpdDataSourceAvailable(boolean spdDataSourceAvailable)
    {
        this.spdDataSourceAvailable = spdDataSourceAvailable;
    }

    public int getMirrorRows()
    {
        return mirrorRows;
    }

    public void setMirrorRows(int mirrorRows)
    {
        this.mirrorRows = mirrorRows;
    }

    public int getSpdRows()
    {
        return spdRows;
    }

    public void setSpdRows(int spdRows)
    {
        this.spdRows = spdRows;
    }

    public String getMirrorSkippedReason()
    {
        return mirrorSkippedReason;
    }

    public void setMirrorSkippedReason(String mirrorSkippedReason)
    {
        this.mirrorSkippedReason = mirrorSkippedReason;
    }

    public String getMirrorError()
    {
        return mirrorError;
    }

    public void setMirrorError(String mirrorError)
    {
        this.mirrorError = mirrorError;
    }

    public String getSpdSyncError()
    {
        return spdSyncError;
    }

    public void setSpdSyncError(String spdSyncError)
    {
        this.spdSyncError = spdSyncError;
    }

    public String getSpdNote()
    {
        return spdNote;
    }

    public void setSpdNote(String spdNote)
    {
        this.spdNote = spdNote;
    }

    public Map<String, Object> toMap()
    {
        Map<String, Object> map = new LinkedHashMap<>(16);
        map.put("apiCode", apiCode);
        map.put("syncBatchNo", syncBatchNo);
        map.put("mirrorEnabled", mirrorEnabled);
        map.put("spdSyncEnabled", spdSyncEnabled);
        map.put("spdDataSourceAvailable", spdDataSourceAvailable);
        map.put("mirrorRows", mirrorRows);
        map.put("spdRows", spdRows);
        if (mirrorSkippedReason != null)
        {
            map.put("mirrorSkippedReason", mirrorSkippedReason);
        }
        if (mirrorError != null)
        {
            map.put("mirrorError", mirrorError);
        }
        if (spdSyncError != null)
        {
            map.put("spdSyncError", spdSyncError);
        }
        if (spdNote != null)
        {
            map.put("spdNote", spdNote);
        }
        return map;
    }
}
