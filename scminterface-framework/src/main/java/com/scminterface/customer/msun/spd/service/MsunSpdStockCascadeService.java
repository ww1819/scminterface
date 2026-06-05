package com.scminterface.customer.msun.spd.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.service.MsunHisMirrorSyncService;
import com.scminterface.customer.msun.service.MsunSpdQueryService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 2.5.82 合并库存落库后，按行自动链式调用 2.5.43 批次库存并落镜像库。
 */
@Service
public class MsunSpdStockCascadeService
{
    private static final Logger log = LoggerFactory.getLogger(MsunSpdStockCascadeService.class);
    private static final int DEFAULT_MAX_CASCADE = 500;
    private static final long CASCADE_DELAY_MS = 100L;

    private final MsunSpdQueryService spdQueryService;
    private final MsunHisMirrorSyncService mirrorSyncService;

    public MsunSpdStockCascadeService(MsunSpdQueryService spdQueryService, MsunHisMirrorSyncService mirrorSyncService)
    {
        this.spdQueryService = spdQueryService;
        this.mirrorSyncService = mirrorSyncService;
    }

    public JSONObject cascadeBatchStocks(MsunHospitalRuntime runtime, JSONObject mergeWrapped, int maxCascade)
    {
        JSONObject summary = new JSONObject();
        summary.put("enabled", true);
        summary.put("maxCascade", maxCascade > 0 ? maxCascade : DEFAULT_MAX_CASCADE);
        summary.put("requested", 0);
        summary.put("success", 0);
        summary.put("failed", 0);
        summary.put("skipped", 0);
        summary.put("batchRows", 0);
        JSONArray details = new JSONArray();
        summary.put("details", details);

        JSONArray mergeRows = extractDataArray(mergeWrapped);
        if (mergeRows == null || mergeRows.isEmpty())
        {
            summary.put("message", "合并库存 data 为空，跳过链式批次查询");
            return summary;
        }

        Set<String> seen = new LinkedHashSet<>();
        int limit = maxCascade > 0 ? maxCascade : DEFAULT_MAX_CASCADE;
        int requested = 0;

        for (int i = 0; i < mergeRows.size() && requested < limit; i++)
        {
            JSONObject row = mergeRows.getJSONObject(i);
            if (row == null)
            {
                continue;
            }
            Long deptId = row.getLong("deptId");
            Long drugId = row.getLong("drugId");
            Long drugSpecPackingId = row.getLong("drugSpecPackingId");
            if (deptId == null || drugId == null || drugSpecPackingId == null)
            {
                summary.put("skipped", summary.getIntValue("skipped") + 1);
                continue;
            }
            String key = deptId + "|" + drugId + "|" + drugSpecPackingId;
            if (!seen.add(key))
            {
                continue;
            }
            requested++;
            summary.put("requested", requested);

            JSONObject item = new JSONObject();
            item.put("deptId", deptId);
            item.put("drugId", drugId);
            item.put("drugSpecPackingId", drugSpecPackingId);
            try
            {
                JSONObject batchWrapped = spdQueryService.queryDrugBatchStocks(runtime, deptId, drugId, drugSpecPackingId);
                mirrorSyncService.syncQueryResult(runtime, "2.5.43", batchWrapped);
                JSONArray batchRows = extractDataArray(batchWrapped);
                int batchCount = batchRows == null ? 0 : batchRows.size();
                summary.put("batchRows", summary.getIntValue("batchRows") + batchCount);
                item.put("ok", true);
                item.put("batchCount", batchCount);
                JSONObject hisBody = batchWrapped.getJSONObject("hisBody");
                if (hisBody != null)
                {
                    item.put("traceId", hisBody.getString("traceId"));
                }
                summary.put("success", summary.getIntValue("success") + 1);
            }
            catch (Exception ex)
            {
                item.put("ok", false);
                item.put("error", ex.getMessage());
                summary.put("failed", summary.getIntValue("failed") + 1);
                log.warn("链式2.5.43失败 deptId={} drugId={} specId={} err={}",
                        deptId, drugId, drugSpecPackingId, ex.getMessage());
            }
            details.add(item);

            if (requested < limit && i < mergeRows.size() - 1)
            {
                sleepQuietly(CASCADE_DELAY_MS);
            }
        }
        summary.put("message", "链式批次库存完成");
        return summary;
    }

    private static JSONArray extractDataArray(JSONObject wrapped)
    {
        if (wrapped == null)
        {
            return null;
        }
        JSONObject hisBody = wrapped.getJSONObject("hisBody");
        if (hisBody == null)
        {
            return null;
        }
        return hisBody.getJSONArray("data");
    }

    private static void sleepQuietly(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }
}
