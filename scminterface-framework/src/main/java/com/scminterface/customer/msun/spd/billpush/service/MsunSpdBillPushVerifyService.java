package com.scminterface.customer.msun.spd.billpush.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.service.MsunSpdQueryService;
import com.scminterface.customer.msun.spd.billpush.MsunSpdBillPushConstants;
import com.scminterface.customer.msun.support.MsunHisBatchStockSupport;
import com.scminterface.customer.msun.support.MsunHisResponseSupport;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 单据推送 HIS 成功后的即时校验（2.5.102 + 出库 2.5.43），仅标注 {@code his_push_msg}，不回滚推送状态。
 */
@Service
public class MsunSpdBillPushVerifyService
{
    private static final Logger log = LoggerFactory.getLogger(MsunSpdBillPushVerifyService.class);
    private static final int VERIFY_WINDOW_MINUTES = 15;
    private static final String DATE_FMT = "yyyy-MM-dd HH:mm:ss";

    private final MsunSpdQueryService queryService;
    private final MsunSpdBillPushExecutor executor;

    public MsunSpdBillPushVerifyService(MsunSpdQueryService queryService, MsunSpdBillPushExecutor executor)
    {
        this.queryService = queryService;
        this.executor = executor;
    }

    public void verifyAfterPush(
            MsunHospitalRuntime runtime,
            String tenantId,
            Long billId,
            String billNo,
            Object auditDate,
            List<Map<String, Object>> entries,
            String storageDeptHisId,
            String pharmacyDeptHisId,
            boolean outbound)
    {
        if (billId == null || entries == null || entries.isEmpty())
        {
            return;
        }
        try
        {
            List<String> billWarnings = new ArrayList<>();
            JSONArray ykDetails = fetchYkInstockDetails(runtime, billNo, auditDate, storageDeptHisId, outbound);
            if (ykDetails == null)
            {
                billWarnings.add(MsunSpdBillPushConstants.VERIFY_MSG_QUERY_FAILED + "(2.5.102)");
            }
            for (Map<String, Object> entry : entries)
            {
                Long entryId = toLong(entry.get("entry_id"));
                if (entryId == null)
                {
                    continue;
                }
                List<String> entryWarnings = new ArrayList<>();
                if (ykDetails == null)
                {
                    entryWarnings.add(MsunSpdBillPushConstants.VERIFY_MSG_YK_DETAIL_MISSING);
                }
                else if (!matchYkInstockDetail(ykDetails, billId, entryId, tenantId))
                {
                    entryWarnings.add(MsunSpdBillPushConstants.VERIFY_MSG_YK_DETAIL_MISSING);
                }
                if (outbound)
                {
                    verifyOutboundStocks(runtime, entry, pharmacyDeptHisId, entryWarnings);
                }
                String msg = entryWarnings.isEmpty() ? null : String.join("；", entryWarnings);
                Map<String, Object> row = new HashMap<>(8);
                row.put("tenantId", tenantId);
                row.put("entryId", entryId);
                row.put("hisPushStatus", MsunSpdBillPushConstants.PUSH_SUCCESS);
                row.put("hisPushMsg", msg);
                executor.updateEntryHisPushStatus(row);
                if (msg != null)
                {
                    billWarnings.add("明细" + entryId + ":" + msg);
                }
            }
            if (!billWarnings.isEmpty())
            {
                String billMsg = "推送成功，校验异常：" + String.join("；", billWarnings);
                if (billMsg.length() > 480)
                {
                    billMsg = billMsg.substring(0, 480) + "…";
                }
                Map<String, Object> billRow = new HashMap<>(8);
                billRow.put("tenantId", tenantId);
                billRow.put("billId", billId);
                billRow.put("hisPushStatus", MsunSpdBillPushConstants.PUSH_SUCCESS);
                billRow.put("hisPushMsg", billMsg);
                executor.updateBillHisPushStatus(billRow);
            }
        }
        catch (Exception ex)
        {
            log.warn("众阳HIS推送后校验异常 billId={} err={}", billId, ex.getMessage());
            String msg = MsunSpdBillPushConstants.VERIFY_MSG_QUERY_FAILED + ":" + ex.getMessage();
            if (msg.length() > 480)
            {
                msg = msg.substring(0, 480) + "…";
            }
            Map<String, Object> billRow = new HashMap<>(8);
            billRow.put("tenantId", tenantId);
            billRow.put("billId", billId);
            billRow.put("hisPushStatus", MsunSpdBillPushConstants.PUSH_SUCCESS);
            billRow.put("hisPushMsg", msg);
            executor.updateBillHisPushStatus(billRow);
        }
    }

    private void verifyOutboundStocks(
            MsunHospitalRuntime runtime,
            Map<String, Object> entry,
            String pharmacyDeptHisId,
            List<String> entryWarnings)
    {
        Long drugId = toLong(entry.get("his_drug_id"));
        Long specId = toLong(entry.get("his_drug_spec_packing_id"));
        if (drugId == null || specId == null)
        {
            Long materialId = toLong(entry.get("material_id"));
            if (materialId != null)
            {
                Map<String, Object> material = executor.selectMaterialById(runtime.getTenantId(), materialId);
                if (material != null)
                {
                    if (drugId == null)
                    {
                        drugId = toLong(material.get("his_id"));
                    }
                    if (specId == null)
                    {
                        specId = toLong(material.get("his_spec_packing_id"));
                    }
                }
            }
        }
        Long deptId = toLong(pharmacyDeptHisId);
        if (deptId == null || drugId == null || specId == null)
        {
            entryWarnings.add(MsunSpdBillPushConstants.VERIFY_MSG_BATCH_STOCK_MISSING + "(缺HIS对照)");
            return;
        }
        JSONArray batchRows = fetchBatchStockRows(runtime, deptId, drugId, specId);
        if (batchRows == null)
        {
            entryWarnings.add(MsunSpdBillPushConstants.VERIFY_MSG_BATCH_STOCK_MISSING + "(查询失败)");
            return;
        }
        if (!matchBatchStock(batchRows, entry))
        {
            entryWarnings.add(MsunSpdBillPushConstants.VERIFY_MSG_BATCH_STOCK_MISSING);
        }
    }

    private JSONArray fetchYkInstockDetails(
            MsunHospitalRuntime runtime,
            String billNo,
            Object auditDate,
            String storageDeptHisId,
            boolean outbound) throws Exception
    {
        String[] window = buildVerifyTimeWindow(parseAuditDate(auditDate));
        Long deptId = toLong(storageDeptHisId);
        JSONObject wrapped = queryService.queryYkInstock(
                runtime, deptId, window[0], window[1], billNo, outbound ? "0" : "1");
        JSONArray headers = extractHisDataArray(wrapped);
        if (headers == null)
        {
            return null;
        }
        JSONArray allDetails = new JSONArray();
        for (int i = 0; i < headers.size(); i++)
        {
            JSONObject header = headers.getJSONObject(i);
            if (header == null)
            {
                continue;
            }
            JSONArray details = header.getJSONArray("stockDetailList");
            if (details != null)
            {
                for (int j = 0; j < details.size(); j++)
                {
                    allDetails.add(details.get(j));
                }
            }
        }
        return allDetails;
    }

    private JSONArray fetchBatchStockRows(MsunHospitalRuntime runtime, Long deptId, Long drugId, Long specId)
    {
        try
        {
            JSONObject wrapped = queryService.queryDrugBatchStocks(runtime, deptId, drugId, specId);
            return extractHisDataArray(wrapped);
        }
        catch (Exception ex)
        {
            log.warn("2.5.43 推送后校验查询失败 deptId={} err={}", deptId, ex.getMessage());
            return null;
        }
    }

    private static boolean matchYkInstockDetail(
            JSONArray ykDetails, Long billId, Long entryId, String tenantId)
    {
        String spdDetailId = MsunSpdBillPushConstants.buildSpdDetailId(billId, entryId);
        String memo = MsunSpdBillPushConstants.buildEntryMemo(tenantId, entryId);
        String legacyDetailId = String.valueOf(entryId);
        for (int i = 0; i < ykDetails.size(); i++)
        {
            JSONObject detail = ykDetails.getJSONObject(i);
            if (detail == null)
            {
                continue;
            }
            String rowSpdDetailId = detail.getString("spdDetailId");
            if (StringUtils.isNotEmpty(spdDetailId) && spdDetailId.equals(rowSpdDetailId))
            {
                return true;
            }
            if (legacyDetailId.equals(rowSpdDetailId))
            {
                return true;
            }
            if (memo.equals(detail.getString("memo")))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean matchBatchStock(JSONArray batchRows, Map<String, Object> entry)
    {
        if (batchRows == null || batchRows.isEmpty())
        {
            return false;
        }
        String pharmacyStockId = str(entry.get("his_pharmacy_stock_id"));
        String stockQueryId = str(entry.get("his_stock_query_id"));
        String batchNumber = str(entry.get("batch_number"));
        for (int i = 0; i < batchRows.size(); i++)
        {
            JSONObject row = batchRows.getJSONObject(i);
            if (row == null)
            {
                continue;
            }
            if (StringUtils.isNotEmpty(stockQueryId))
            {
                String rowQueryId = MsunHisBatchStockSupport.resolveStockQueryId(row);
                if (stockQueryId.equals(rowQueryId))
                {
                    return hasPositiveStock(row);
                }
            }
            else if (StringUtils.isNotEmpty(pharmacyStockId))
            {
                String psId = row.getString("pharmacyStockId");
                if (pharmacyStockId.equals(psId))
                {
                    return hasPositiveStock(row);
                }
            }
            else if (StringUtils.isNotEmpty(batchNumber))
            {
                String batch = row.getString("ycBatchNo");
                if (batchNumber.equals(batch))
                {
                    return hasPositiveStock(row);
                }
            }
            else if (hasPositiveStock(row))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPositiveStock(JSONObject row)
    {
        BigDecimal amt = MsunHisBatchStockSupport.resolveStockAmount(row);
        if (amt == null)
        {
            return true;
        }
        return amt.compareTo(BigDecimal.ZERO) > 0;
    }

    private static JSONArray extractHisDataArray(JSONObject wrapped)
    {
        JSONObject hisBody = MsunHisResponseSupport.resolveHisBody(wrapped);
        if (hisBody == null || !Boolean.TRUE.equals(hisBody.getBoolean("success")))
        {
            return null;
        }
        return hisBody.getJSONArray("data");
    }

    private static String[] buildVerifyTimeWindow(Date anchor)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(anchor != null ? anchor : new Date());
        cal.add(Calendar.MINUTE, -VERIFY_WINDOW_MINUTES);
        String start = formatDate(cal.getTime());
        cal.setTime(anchor != null ? anchor : new Date());
        cal.add(Calendar.MINUTE, VERIFY_WINDOW_MINUTES);
        String end = formatDate(cal.getTime());
        return new String[] { start, end };
    }

    private static Date parseAuditDate(Object auditDate)
    {
        if (auditDate == null)
        {
            return new Date();
        }
        if (auditDate instanceof Date)
        {
            return (Date) auditDate;
        }
        try
        {
            return new SimpleDateFormat(DATE_FMT).parse(String.valueOf(auditDate));
        }
        catch (Exception ex)
        {
            return new Date();
        }
    }

    private static String formatDate(Date date)
    {
        return new SimpleDateFormat(DATE_FMT).format(date);
    }

    private static Long toLong(Object val)
    {
        if (val == null)
        {
            return null;
        }
        if (val instanceof Number)
        {
            return ((Number) val).longValue();
        }
        try
        {
            return Long.parseLong(String.valueOf(val).trim());
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    private static String str(Object val)
    {
        return val == null ? null : String.valueOf(val).trim();
    }
}
