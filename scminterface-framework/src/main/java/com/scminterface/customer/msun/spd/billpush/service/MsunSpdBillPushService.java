package com.scminterface.customer.msun.spd.billpush.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.support.MsunHisDateTimeSupport;
import com.scminterface.customer.msun.support.MsunHisBatchStockSupport;
import com.scminterface.customer.msun.support.MsunHisJsonSupport;
import com.scminterface.customer.msun.support.MsunHisResponseSupport;
import com.scminterface.customer.msun.service.MsunSpdQueryService;
import com.scminterface.customer.msun.spd.billpush.MsunSpdBillPushConstants;
import com.scminterface.customer.msun.spd.service.MsunSpdPushInvokeResult;
import com.scminterface.customer.msun.spd.service.MsunSpdPushService;
import com.scminterface.framework.datasource.DataSourceAvailability;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SPD 出库/退库单据查询与 HIS 推送编排（查询 SPD → 调众阳 → 回写 SPD 主从表标志）。
 */
@Service
public class MsunSpdBillPushService
{
    private static final Logger log = LoggerFactory.getLogger(MsunSpdBillPushService.class);

    private static final List<Integer> DEFAULT_BILL_TYPES = Arrays.asList(201, 401);

    private final MsunSpdBillPushExecutor executor;
    private final MsunSpdPushService pushService;
    private final MsunSpdQueryService queryService;
    private final MsunSpdBillPushVerifyService verifyService;
    private final DataSourceAvailability dataSourceAvailability;

    public MsunSpdBillPushService(
            MsunSpdBillPushExecutor executor,
            MsunSpdPushService pushService,
            MsunSpdQueryService queryService,
            MsunSpdBillPushVerifyService verifyService,
            DataSourceAvailability dataSourceAvailability)
    {
        this.executor = executor;
        this.pushService = pushService;
        this.queryService = queryService;
        this.verifyService = verifyService;
        this.dataSourceAvailability = dataSourceAvailability;
    }

    public Map<String, Object> queryBillEntries(MsunHospitalRuntime runtime, Map<String, Object> params)
    {
        assertSpdEnabled();
        Map<String, Object> query = buildQuery(runtime.getTenantId(), params);
        long total = executor.countBillEntryRows(stripPagingParams(query));
        List<Map<String, Object>> rows = executor.listBillEntryRows(query);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("rows", rows);
        result.put("pageNum", query.get("pageNum"));
        result.put("pageSize", query.get("pageSize"));
        return result;
    }

    public Map<String, Object> pushBills(MsunHospitalRuntime runtime, List<Long> billIds)
    {
        assertSpdEnabled();
        if (billIds == null || billIds.isEmpty())
        {
            throw new IllegalArgumentException("请指定要推送的单据");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        int pushed = 0;
        int skipped = 0;
        int fail = 0;
        for (Long billId : billIds)
        {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("billId", billId);
            Map<String, Object> detail = pushOneBill(runtime, billId);
            one.putAll(detail);
            if (Boolean.FALSE.equals(detail.get("success")))
            {
                fail++;
            }
            else if (Boolean.TRUE.equals(detail.get("skipped")))
            {
                skipped++;
            }
            else
            {
                pushed++;
            }
            results.add(one);
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", billIds.size());
        summary.put("pushedCount", pushed);
        summary.put("skipCount", skipped);
        summary.put("failCount", fail);
        summary.put("successCount", pushed);
        summary.put("message", buildPushSummaryMessage(pushed, skipped, fail));
        summary.put("results", results);
        return summary;
    }

    private Map<String, Object> pushOneBill(MsunHospitalRuntime runtime, Long billId)
    {
        String tenantId = runtime.getTenantId();
        Map<String, Object> bill = executor.selectBillById(tenantId, billId);
        if (bill == null || bill.isEmpty())
        {
            throw new IllegalArgumentException("单据不存在 id=" + billId);
        }
        Integer billStatus = toInteger(bill.get("bill_status"));
        if (billStatus == null || billStatus != 2)
        {
            throw new IllegalArgumentException("未审核单据不允许推送HIS，单号=" + str(bill.get("bill_no")));
        }
        Integer billType = toInteger(bill.get("bill_type"));
        if (billType == null)
        {
            throw new IllegalArgumentException("单据类型未知");
        }
        List<Map<String, Object>> entries = executor.selectEntriesByBillId(tenantId, billId);
        List<Map<String, Object>> toPush = filterEntriesForPush(entries);
        if (toPush.isEmpty())
        {
            Map<String, Object> skip = new LinkedHashMap<>();
            skip.put("success", true);
            skip.put("status", "skipped");
            skip.put("billNo", bill.get("bill_no"));
            skip.put("billType", billType);
            skip.put("skipped", true);
            skip.put("entryTotal", entries.size());
            skip.put("alreadyPushedEntryCount", countAlreadyPushedEntries(entries));
            skip.put("pushedEntryCount", 0);
            skip.put("message", "无待推送明细（均已成功），未调用 HIS");
            return skip;
        }

        markBillPushing(tenantId, billId);
        Map<String, Object> hisInvoke = null;
        Map<String, Object> pendingHisBody = null;
        try
        {
            MsunSpdPushInvokeResult invoke = null;
            if (billType == 201)
            {
                Map<String, Object> body = buildOutboundBody(runtime, bill, toPush);
                pendingHisBody = stripLogMetaFromBody(body);
                invoke = pushService.pushDrugStocksNew(runtime, body, extractLogMeta(bill));
                hisInvoke = invoke.getDebug();
                applyOutboundResponse(tenantId, billId, toPush, invoke.getWrappedResponse());
            }
            else if (billType == 401)
            {
                backfillReturnEntryHisStockIds(tenantId, toPush);
                Map<String, Object> body = buildReturnBody(runtime, bill, toPush);
                pendingHisBody = stripLogMetaFromBody(body);
                invoke = pushService.pushDrugStocksReturn(runtime, body, extractLogMeta(bill));
                hisInvoke = invoke.getDebug();
                applyReturnResponse(tenantId, toPush, invoke.getWrappedResponse());
            }
            else
            {
                throw new IllegalArgumentException("不支持推送的单据类型 billType=" + billType);
            }
            String traceId = MsunHisResponseSupport.extractTraceId(invoke.getWrappedResponse());
            markBillSuccess(tenantId, billId, traceId);
            String storageHisId = resolveWarehouseHisId(tenantId, toLong(bill.get("warehouse_id")));
            String pharmacyHisId = resolveDepartmentHisId(tenantId, toLong(bill.get("department_id")));
            verifyService.verifyAfterPush(
                    runtime, tenantId, billId, str(bill.get("bill_no")), bill.get("audit_date"),
                    toPush, storageHisId, pharmacyHisId, billType == 201);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("status", "pushed");
            ok.put("billNo", bill.get("bill_no"));
            ok.put("billType", billType);
            ok.put("skipped", false);
            ok.put("entryTotal", entries.size());
            ok.put("pushedEntryCount", toPush.size());
            ok.put("traceId", traceId);
            ok.put("hisInvoke", hisInvoke);
            return ok;
        }
        catch (Exception ex)
        {
            String brief = summarizePushError(ex);
            log.warn("单据推送失败 billId={} err={}", billId, brief, ex);
            markBillFailed(tenantId, billId, toPush, brief);
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("success", false);
            fail.put("status", "failed");
            fail.put("skipped", false);
            fail.put("billNo", bill.get("bill_no"));
            fail.put("billType", billType);
            fail.put("message", brief);
            if (hisInvoke != null)
            {
                fail.put("hisInvoke", hisInvoke);
            }
            else if (pendingHisBody != null)
            {
                Map<String, Object> partial = new LinkedHashMap<>(4);
                partial.put("apiCode", billType == 401 ? "2.5.42" : "2.5.41");
                partial.put("requestBodySummary", MsunHisJsonSupport.truncateForLog(pendingHisBody, 1024));
                partial.put("note", "HIS 未调用或调用前失败，仅展示已组装的入参");
                fail.put("hisInvoke", partial);
            }
            return fail;
        }
    }

    private static Map<String, Object> stripLogMetaFromBody(Map<String, Object> body)
    {
        if (body == null)
        {
            return null;
        }
        Map<String, Object> copy = new LinkedHashMap<>(body);
        copy.remove("_spdLogMeta");
        return copy;
    }

    private void assertSpdEnabled()
    {
        if (!dataSourceAvailability.isAvailable(DataSourceType.SPD))
        {
            throw new IllegalStateException("spring.datasource.druid.spd.enabled=false，SPD 数据源未启用");
        }
    }

    private Map<String, Object> buildQuery(String tenantId, Map<String, Object> params)
    {
        Map<String, Object> query = new HashMap<>(16);
        query.put("tenantId", tenantId);
        query.put("billNo", trim(params.get("billNo")));
        query.put("materialName", trim(params.get("materialName")));
        query.put("materialSpeci", trim(params.get("materialSpeci")));
        query.put("departmentName", trim(params.get("departmentName")));
        query.put("warehouseName", trim(params.get("warehouseName")));
        query.put("hisPushStatus", trim(params.get("hisPushStatus")));
        Integer billType = toInteger(params.get("billType"));
        if (billType != null)
        {
            query.put("billType", billType);
        }
        else
        {
            query.put("billTypes", DEFAULT_BILL_TYPES);
        }
        Integer billStatus = toInteger(params.get("billStatus"));
        query.put("billStatus", billStatus != null ? billStatus : 2);
        int pageNum = toInt(params.get("pageNum"), 1);
        int pageSize = Math.min(toInt(params.get("pageSize"), 20), 200);
        query.put("pageNum", pageNum);
        query.put("pageSize", pageSize);
        return query;
    }

    /** count 查询不能带 pageNum/pageSize，否则 PageHelper 会追加 LIMIT */
    private static Map<String, Object> stripPagingParams(Map<String, Object> query)
    {
        Map<String, Object> copy = new HashMap<>(query);
        copy.remove("pageNum");
        copy.remove("pageSize");
        copy.remove("limit");
        copy.remove("offset");
        return copy;
    }

    private static String buildPushSummaryMessage(int pushed, int skipped, int fail)
    {
        if (fail > 0 && pushed == 0 && skipped == 0)
        {
            return "推送失败：" + fail + " 张单据";
        }
        if (pushed == 0 && skipped > 0 && fail == 0)
        {
            return "已跳过：" + skipped + " 张单据明细均已推送成功，未重复调用 HIS";
        }
        StringBuilder sb = new StringBuilder("推送完成");
        if (pushed > 0)
        {
            sb.append("：成功 ").append(pushed);
        }
        if (skipped > 0)
        {
            sb.append(pushed > 0 ? "，跳过 " : "：跳过 ").append(skipped);
        }
        if (fail > 0)
        {
            sb.append("，失败 ").append(fail);
        }
        return sb.toString();
    }

    private static int countAlreadyPushedEntries(List<Map<String, Object>> entries)
    {
        if (entries == null || entries.isEmpty())
        {
            return 0;
        }
        int count = 0;
        for (Map<String, Object> e : entries)
        {
            if (e == null)
            {
                continue;
            }
            String st = str(e.get("his_push_status"));
            if (MsunSpdBillPushConstants.PUSH_SUCCESS.equals(st))
            {
                count++;
            }
        }
        return count;
    }

    private List<Map<String, Object>> filterEntriesForPush(List<Map<String, Object>> entries)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        if (entries == null)
        {
            return list;
        }
        for (Map<String, Object> e : entries)
        {
            if (e == null)
            {
                continue;
            }
            String st = str(e.get("his_push_status"));
            if (st == null || st.isEmpty())
            {
                st = MsunSpdBillPushConstants.PUSH_NOT;
            }
            if (MsunSpdBillPushConstants.PUSH_SUCCESS.equals(st))
            {
                continue;
            }
            list.add(e);
        }
        return list;
    }

    private Map<String, Object> buildOutboundBody(
            MsunHospitalRuntime runtime, Map<String, Object> bill, List<Map<String, Object>> entries)
    {
        String tenantId = runtime.getTenantId();
        Long warehouseId = toLong(bill.get("warehouse_id"));
        Long departmentId = toLong(bill.get("department_id"));
        String storageHisId = resolveWarehouseHisId(tenantId, warehouseId);
        String pharmacyHisId = resolveDepartmentHisId(tenantId, departmentId);
        String supplierHisId = resolveSupplierHisId(tenantId, bill, entries);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("supplierId", MsunHisJsonSupport.requireSnowflakeId(supplierHisId, "供应商HIS对照"));
        body.put("storageDeptId", MsunHisJsonSupport.requireSnowflakeId(storageHisId, "仓库HIS药库科室"));
        body.put("pharmacyDeptId", MsunHisJsonSupport.requireSnowflakeId(pharmacyHisId, "科室HIS对照"));
        body.put("invoiceCode", bill.get("bill_no"));
        body.put("inStockStatus", MsunSpdBillPushConstants.IN_STOCK_STATUS_PHARMACY);
        body.put("spdMainId", bill.get("bill_no"));
        body.put("saveCorrelationFlag", MsunSpdBillPushConstants.SAVE_CORRELATION_FLAG);

        Long billId = toLong(bill.get("id"));
        List<Map<String, Object>> details = new ArrayList<>();
        for (Map<String, Object> entry : entries)
        {
            Long entryId = toLong(entry.get("entry_id"));
            Long materialId = toLong(entry.get("material_id"));
            Map<String, Object> material = materialId != null
                    ? executor.selectMaterialById(tenantId, materialId) : null;
            if (material == null || material.isEmpty())
            {
                throw new IllegalArgumentException("耗材不存在 entryId=" + entryId);
            }
            String memo = MsunSpdBillPushConstants.buildEntryMemo(tenantId, entryId);
            String spdDetailId = MsunSpdBillPushConstants.buildSpdDetailId(billId, entryId);
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("drugId", MsunHisJsonSupport.requireSnowflakeId(str(material.get("his_id")), "耗材HIS drugId"));
            line.put("drugSpecPackingId",
                    MsunHisJsonSupport.requireSnowflakeId(str(material.get("his_spec_packing_id")), "耗材HIS规格"));
            line.put("quantity", entry.get("qty"));
            line.put("buyPrice", entry.get("unit_price"));
            line.put("retailPrice", entry.get("unit_price"));
            line.put("invoiceCode", bill.get("bill_no"));
            line.put("produceDate", MsunHisDateTimeSupport.formatOrNow(entry.get("begin_time")));
            line.put("effectiveDate", formatHisEffectiveDate(entry.get("end_time")));
            line.put("ycBatchNo", normalizeHisBatchNumber(entry.get("batch_number")));
            line.put("spdDetailId", spdDetailId);
            line.put("memo", memo);
            details.add(line);
            markEntryPrepare(tenantId, entryId, memo, spdDetailId,
                    str(material.get("his_id")), str(material.get("his_spec_packing_id")));
        }
        body.put("inStockDetailDTOList", details);
        body.put("_spdLogMeta", extractLogMeta(bill));
        return body;
    }

    private Map<String, Object> buildReturnBody(
            MsunHospitalRuntime runtime, Map<String, Object> bill, List<Map<String, Object>> entries)
    {
        String tenantId = runtime.getTenantId();
        Long billId = toLong(bill.get("id"));
        String storageHisId = resolveWarehouseHisId(tenantId, toLong(bill.get("warehouse_id")));
        String pharmacyHisId = resolveDepartmentHisId(tenantId, toLong(bill.get("department_id")));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("storageDeptId", MsunHisJsonSupport.requireSnowflakeId(storageHisId, "仓库HIS药库科室"));
        body.put("pharmacyDeptId", MsunHisJsonSupport.requireSnowflakeId(pharmacyHisId, "科室HIS对照"));
        body.put("isReturnToSupplier", MsunSpdBillPushConstants.RETURN_TO_SUPPLIER_YES);
        body.put("memo", str(bill.get("bill_no")));

        List<Map<String, Object>> details = new ArrayList<>();
        Long pharmacyDeptHisId = parseLongRequired(pharmacyHisId, "科室HIS对照");
        for (Map<String, Object> entry : entries)
        {
            Long entryId = toLong(entry.get("entry_id"));
            String pharmacyStockId = resolvePharmacyStockIdForPush(runtime, bill, entry, pharmacyDeptHisId);
            String detailMemo = MsunSpdBillPushConstants.buildSpdDetailId(billId, entryId);
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("pharmacyStockId", MsunHisJsonSupport.requireSnowflakeId(pharmacyStockId, "pharmacyStockId"));
            line.put("quantity", entry.get("qty"));
            line.put("memo", detailMemo);
            details.add(line);
            String drugId = str(entry.get("his_drug_id"));
            String specId = str(entry.get("his_drug_spec_packing_id"));
            if (StringUtils.isEmpty(drugId) || StringUtils.isEmpty(specId))
            {
                Long materialId = toLong(entry.get("material_id"));
                Map<String, Object> material = materialId != null
                        ? executor.selectMaterialById(tenantId, materialId) : null;
                if (material != null)
                {
                    if (StringUtils.isEmpty(drugId))
                    {
                        drugId = str(material.get("his_id"));
                    }
                    if (StringUtils.isEmpty(specId))
                    {
                        specId = str(material.get("his_spec_packing_id"));
                    }
                }
            }
            markEntryPrepare(tenantId, entryId, detailMemo, detailMemo, drugId, specId);
        }
        body.put("outStockDetailDTOList", details);
        body.put("_spdLogMeta", extractLogMeta(bill));
        return body;
    }

    private void applyOutboundResponse(
            String tenantId, Long billId, List<Map<String, Object>> entries, JSONObject response)
    {
        MsunHisResponseSupport.assertHisSuccess(response);
        JSONObject hisBody = MsunHisResponseSupport.resolveHisBody(response);
        if (hisBody == null)
        {
            throw new IllegalStateException("HIS推送响应格式异常");
        }
        JSONArray data = hisBody.getJSONArray("data");
        if (data == null)
        {
            throw new IllegalStateException("HIS未返回入库明细数据");
        }
        Map<String, JSONObject> byMemo = new HashMap<>();
        Map<String, JSONObject> bySpdDetail = new HashMap<>();
        for (int i = 0; i < data.size(); i++)
        {
            JSONObject row = data.getJSONObject(i);
            if (row == null)
            {
                continue;
            }
            if (StringUtils.isNotEmpty(row.getString("memo")))
            {
                byMemo.put(row.getString("memo"), row);
            }
            if (StringUtils.isNotEmpty(row.getString("spdDetailId")))
            {
                bySpdDetail.put(row.getString("spdDetailId"), row);
            }
        }
        for (Map<String, Object> entry : entries)
        {
            Long entryId = toLong(entry.get("entry_id"));
            String memo = MsunSpdBillPushConstants.buildEntryMemo(tenantId, entryId);
            String spdDetailId = MsunSpdBillPushConstants.buildSpdDetailId(billId, entryId);
            JSONObject row = byMemo.get(memo);
            if (row == null && StringUtils.isNotEmpty(spdDetailId))
            {
                row = bySpdDetail.get(spdDetailId);
            }
            if (row == null)
            {
                row = bySpdDetail.get(String.valueOf(entryId));
            }
            if (row == null)
            {
                throw new IllegalStateException("HIS回参未匹配明细 entryId=" + entryId);
            }
            String pharmacyStockId = firstNonEmpty(row.getString("pharmacyStockId"), row.getString("storageStockId"));
            markEntryPushResult(tenantId, entryId, pharmacyStockId,
                    row.getString("storageStockId"), MsunHisBatchStockSupport.resolveStockQueryId(row), null);
            Long depId = toLong(entry.get("dep_inventory_id"));
            if (depId == null)
            {
                depId = toLong(entry.get("kc_no"));
            }
            if (depId != null && StringUtils.isNotEmpty(pharmacyStockId))
            {
                Map<String, Object> depRow = new HashMap<>(8);
                depRow.put("tenantId", tenantId);
                depRow.put("depInventoryId", depId);
                depRow.put("hisPharmacyStockId", pharmacyStockId);
                depRow.put("hisStorageStockId", row.getString("storageStockId"));
                depRow.put("hisStockQueryId", MsunHisBatchStockSupport.resolveStockQueryId(row));
                executor.updateDepInventoryHisStock(depRow);
            }
        }
    }

    private void applyReturnResponse(String tenantId, List<Map<String, Object>> entries, JSONObject response)
    {
        MsunHisResponseSupport.assertHisSuccess(response);
        for (Map<String, Object> entry : entries)
        {
            Long entryId = toLong(entry.get("entry_id"));
            Map<String, Object> row = new HashMap<>(8);
            row.put("tenantId", tenantId);
            row.put("entryId", entryId);
            row.put("hisPushStatus", MsunSpdBillPushConstants.PUSH_SUCCESS);
            executor.updateEntryHisPushStatus(row);
        }
    }

    /**
     * 退库推送前：若明细 HIS 库存三字段为空，从关联科室库存或原出库明细回填并落库。
     */
    private void backfillReturnEntryHisStockIds(String tenantId, List<Map<String, Object>> entries)
    {
        if (entries == null || entries.isEmpty())
        {
            return;
        }
        for (Map<String, Object> entry : entries)
        {
            if (entry == null || !needsReturnEntryHisStockBackfill(entry))
            {
                continue;
            }
            HisStockIds stockIds = HisStockIds.fromEntry(entry);
            Long depId = resolveDepInventoryKey(entry);
            Map<String, Object> dep = depId != null ? executor.selectDepInventoryById(tenantId, depId) : null;
            stockIds = stockIds.mergeFrom(dep);

            if (stockIds.needsBackfill() && dep != null)
            {
                Long outboundEntryId = toLong(dep.get("bill_entry_id"));
                if (outboundEntryId != null)
                {
                    stockIds = stockIds.mergeFrom(executor.selectBillEntryHisStockById(tenantId, outboundEntryId));
                }
            }
            if (stockIds.needsBackfill() && depId != null)
            {
                stockIds = stockIds.mergeFrom(
                        executor.selectOutboundEntryHisStockByDepInventoryId(tenantId, depId));
            }
            String pharmacy = stockIds.pharmacy;
            String storage = stockIds.storage;
            String stockQuery = stockIds.stockQuery;
            if (hasReturnEntryHisStockChanged(entry, pharmacy, storage, stockQuery))
            {
                Long entryId = toLong(entry.get("entry_id"));
                entry.put("his_pharmacy_stock_id", pharmacy);
                entry.put("his_storage_stock_id", storage);
                entry.put("his_stock_query_id", stockQuery);
                if (entryId != null)
                {
                    Map<String, Object> row = new HashMap<>(12);
                    row.put("tenantId", tenantId);
                    row.put("entryId", entryId);
                    row.put("hisPharmacyStockId", pharmacy);
                    row.put("hisStorageStockId", storage);
                    row.put("hisStockQueryId", stockQuery);
                    executor.updateEntryHisStockIds(row);
                    log.info("退库明细回填HIS库存键 entryId={} depInventoryId={} stockQueryId={}",
                            entryId, depId, stockQuery);
                }
            }
        }
    }

    private static Long resolveDepInventoryKey(Map<String, Object> entry)
    {
        Long depId = toLong(entry.get("dep_inventory_id"));
        if (depId == null)
        {
            depId = toLong(entry.get("kc_no"));
        }
        return depId;
    }

    private static boolean needsReturnEntryHisStockBackfill(Map<String, Object> entry)
    {
        return needsReturnEntryHisStockBackfill(
                str(entry.get("his_pharmacy_stock_id")),
                str(entry.get("his_storage_stock_id")),
                str(entry.get("his_stock_query_id")));
    }

    private static boolean needsReturnEntryHisStockBackfill(String pharmacy, String storage, String stockQuery)
    {
        return StringUtils.isEmpty(pharmacy)
                || StringUtils.isEmpty(storage)
                || StringUtils.isEmpty(stockQuery);
    }

    private static boolean hasReturnEntryHisStockChanged(
            Map<String, Object> entry, String pharmacy, String storage, String stockQuery)
    {
        return !StringUtils.equals(str(entry.get("his_pharmacy_stock_id")), pharmacy)
                || !StringUtils.equals(str(entry.get("his_storage_stock_id")), storage)
                || !StringUtils.equals(str(entry.get("his_stock_query_id")), stockQuery);
    }

    /**
     * 2.5.42 入参 {@code pharmacyStockId}：传 {@code his_pharmacy_stock_id}（2.5.41 回参 pharmacyStockId；入药库时可兜底 storageStockId）。
     */
    private String resolvePharmacyStockIdForPush(
            MsunHospitalRuntime runtime,
            Map<String, Object> bill,
            Map<String, Object> entry,
            Long pharmacyDeptHisId)
    {
        String tenantId = runtime.getTenantId();
        String existing = resolveReturnPushStockIdLocal(tenantId, entry);
        if (StringUtils.isNotEmpty(existing))
        {
            BigDecimal hisQty = queryHisReturnableQty(runtime, pharmacyDeptHisId, entry, existing);
            assertReturnQtyWithinHis(entry, hisQty);
            return existing;
        }
        try
        {
            HisBatchStockMatch match = resolveFromHisBatchStocks(runtime, pharmacyDeptHisId, entry, null);
            persistResolvedReturnStock(tenantId, entry, match);
            assertReturnQtyWithinHis(entry, match.hisQty);
            return match.returnStockId;
        }
        catch (IllegalArgumentException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("2.5.43 查询HIS批次库存失败: " + ex.getMessage(), ex);
        }
    }

    private String resolveReturnPushStockIdLocal(String tenantId, Map<String, Object> entry)
    {
        String fromEntry = firstNonEmpty(
                str(entry.get("his_pharmacy_stock_id")),
                str(entry.get("his_storage_stock_id")));
        if (StringUtils.isNotEmpty(fromEntry))
        {
            return fromEntry;
        }
        Long depId = toLong(entry.get("dep_inventory_id"));
        if (depId == null)
        {
            depId = toLong(entry.get("kc_no"));
        }
        if (depId != null)
        {
            Map<String, Object> dep = executor.selectDepInventoryById(tenantId, depId);
            if (dep != null)
            {
                return firstNonEmpty(
                        str(dep.get("his_pharmacy_stock_id")),
                        str(dep.get("his_storage_stock_id")));
            }
        }
        return null;
    }

    private HisBatchStockMatch resolveFromHisBatchStocks(
            MsunHospitalRuntime runtime,
            Long pharmacyDeptHisId,
            Map<String, Object> entry,
            String filterReturnStockId) throws Exception
    {
        Long drugId = toLong(entry.get("his_drug_id"));
        Long specId = toLong(entry.get("his_drug_spec_packing_id"));
        if (drugId == null || specId == null)
        {
            Long materialId = toLong(entry.get("material_id"));
            Map<String, Object> material = materialId != null
                    ? executor.selectMaterialById(runtime.getTenantId(), materialId) : null;
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
        if (drugId == null || specId == null)
        {
            throw new IllegalArgumentException("耗材未维护 HIS drugId/drugSpecPackingId，entryId=" + entry.get("entry_id"));
        }
        JSONObject wrapped = queryService.queryDrugBatchStocks(runtime, pharmacyDeptHisId, drugId, specId);
        JSONArray data = extractBatchStockRows(wrapped);
        if (data == null || data.isEmpty())
        {
            throw new IllegalArgumentException("HIS 无可用批次库存，entryId=" + entry.get("entry_id"));
        }
        String batchNumber = str(entry.get("batch_number"));
        HisBatchStockMatch match = new HisBatchStockMatch();
        BigDecimal hisQty = BigDecimal.ZERO;
        for (int i = 0; i < data.size(); i++)
        {
            JSONObject row = data.getJSONObject(i);
            if (row == null)
            {
                continue;
            }
            if (StringUtils.isNotEmpty(batchNumber))
            {
                String batch = row.getString("ycBatchNo");
                if (batch != null && !batchNumber.equals(batch))
                {
                    continue;
                }
            }
            String returnStockId = MsunHisBatchStockSupport.resolveReturnPushStockId(row);
            if (StringUtils.isEmpty(returnStockId))
            {
                continue;
            }
            if (!MsunHisBatchStockSupport.matchesReturnStockFilter(row, filterReturnStockId))
            {
                continue;
            }
            match.returnStockId = returnStockId;
            match.pharmacyStockId = row.getString("pharmacyStockId");
            match.storageStockId = row.getString("storageStockId");
            match.stockQueryId = MsunHisBatchStockSupport.resolveStockQueryId(row);
            BigDecimal amt = MsunHisBatchStockSupport.resolveStockAmount(row);
            if (amt != null)
            {
                hisQty = hisQty.add(amt);
            }
            if (StringUtils.isNotEmpty(batchNumber))
            {
                break;
            }
        }
        if (StringUtils.isEmpty(match.returnStockId))
        {
            throw new IllegalArgumentException("HIS 批次库存未匹配 pharmacyStockId，entryId="
                    + entry.get("entry_id") + "，批号=" + batchNumber);
        }
        match.hisQty = hisQty;
        return match;
    }

    private BigDecimal queryHisReturnableQty(
            MsunHospitalRuntime runtime,
            Long pharmacyDeptHisId,
            Map<String, Object> entry,
            String returnStockId)
    {
        try
        {
            HisBatchStockMatch match = resolveFromHisBatchStocks(runtime, pharmacyDeptHisId, entry, returnStockId);
            return match.hisQty;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("2.5.43 校验HIS可退量失败: " + ex.getMessage(), ex);
        }
    }

    private static void assertReturnQtyWithinHis(Map<String, Object> entry, BigDecimal hisQty)
    {
        BigDecimal qty = toBigDecimal(entry.get("qty"));
        if (qty != null && hisQty != null && qty.compareTo(hisQty) > 0)
        {
            throw new IllegalArgumentException("退库数量超过HIS可退量，HIS库存=" + hisQty
                    + "，entryId=" + entry.get("entry_id"));
        }
    }

    private void persistResolvedReturnStock(String tenantId, Map<String, Object> entry, HisBatchStockMatch match)
    {
        Long entryId = toLong(entry.get("entry_id"));
        if (entryId != null)
        {
            Map<String, Object> row = new HashMap<>(12);
            row.put("tenantId", tenantId);
            row.put("entryId", entryId);
            row.put("hisPharmacyStockId", match.pharmacyStockId);
            row.put("hisStorageStockId", match.storageStockId);
            row.put("hisStockQueryId", match.stockQueryId);
            executor.updateEntryHisStockIds(row);
        }
        Long depId = toLong(entry.get("dep_inventory_id"));
        if (depId == null)
        {
            depId = toLong(entry.get("kc_no"));
        }
        if (depId != null)
        {
            Map<String, Object> depRow = new HashMap<>(12);
            depRow.put("tenantId", tenantId);
            depRow.put("depInventoryId", depId);
            depRow.put("hisPharmacyStockId", match.pharmacyStockId);
            depRow.put("hisStorageStockId", match.storageStockId);
            depRow.put("hisStockQueryId", match.stockQueryId);
            executor.updateDepInventoryHisStock(depRow);
        }
    }

    private static JSONArray extractBatchStockRows(JSONObject wrapped)
    {
        if (wrapped == null)
        {
            return null;
        }
        JSONObject hisBody = wrapped.getJSONObject("hisBody");
        if (hisBody == null || !Boolean.TRUE.equals(hisBody.getBoolean("success")))
        {
            String msg = hisBody != null ? hisBody.getString("message") : null;
            throw new IllegalStateException(msg != null ? msg : "2.5.43 查询失败");
        }
        return hisBody.getJSONArray("data");
    }

    private static BigDecimal toBigDecimal(Object val)
    {
        if (val == null)
        {
            return null;
        }
        if (val instanceof BigDecimal)
        {
            return (BigDecimal) val;
        }
        return new BigDecimal(String.valueOf(val));
    }

    private static final class HisBatchStockMatch
    {
        /** 2.5.42 入参 pharmacyStockId（药房批次 ID，入药库时可为 storageStockId） */
        private String returnStockId;
        private String pharmacyStockId;
        private String storageStockId;
        private String stockQueryId;
        private BigDecimal hisQty;
    }

    private static final class HisStockIds
    {
        private final String pharmacy;
        private final String storage;
        private final String stockQuery;

        private HisStockIds(String pharmacy, String storage, String stockQuery)
        {
            this.pharmacy = pharmacy;
            this.storage = storage;
            this.stockQuery = stockQuery;
        }

        private static HisStockIds fromEntry(Map<String, Object> entry)
        {
            return new HisStockIds(
                    str(entry.get("his_pharmacy_stock_id")),
                    str(entry.get("his_storage_stock_id")),
                    str(entry.get("his_stock_query_id")));
        }

        private HisStockIds mergeFrom(Map<String, Object> source)
        {
            if (source == null || source.isEmpty())
            {
                return this;
            }
            return new HisStockIds(
                    firstNonEmpty(pharmacy, str(source.get("his_pharmacy_stock_id"))),
                    firstNonEmpty(storage, str(source.get("his_storage_stock_id"))),
                    firstNonEmpty(stockQuery, str(source.get("his_stock_query_id"))));
        }

        private boolean needsBackfill()
        {
            return needsReturnEntryHisStockBackfill(pharmacy, storage, stockQuery);
        }
    }

    private String resolveWarehouseHisId(String tenantId, Long warehouseId)
    {
        if (warehouseId == null)
        {
            throw new IllegalArgumentException("仓库不能为空");
        }
        Map<String, Object> wh = executor.selectWarehouseById(tenantId, warehouseId);
        if (wh == null || StringUtils.isEmpty(str(wh.get("his_id"))))
        {
            throw new IllegalArgumentException("仓库未维护 HIS 药库科室对照(his_id)");
        }
        return str(wh.get("his_id"));
    }

    private String resolveDepartmentHisId(String tenantId, Long departmentId)
    {
        if (departmentId == null)
        {
            throw new IllegalArgumentException("科室不能为空");
        }
        Map<String, Object> dept = executor.selectDepartmentById(tenantId, departmentId);
        if (dept == null || StringUtils.isEmpty(str(dept.get("his_id"))))
        {
            throw new IllegalArgumentException("科室未维护 HIS 对照(his_id)");
        }
        return str(dept.get("his_id"));
    }

    private String resolveSupplierHisId(String tenantId, Map<String, Object> bill, List<Map<String, Object>> entries)
    {
        Long supId = toLong(bill.get("suppler_id"));
        if (supId == null && entries != null)
        {
            for (Map<String, Object> e : entries)
            {
                if (e != null && StringUtils.isNotEmpty(str(e.get("entry_supplier_id"))))
                {
                    supId = toLong(e.get("entry_supplier_id"));
                    break;
                }
            }
        }
        if (supId == null)
        {
            throw new IllegalArgumentException("出库单未指定供应商，无法推送HIS");
        }
        Map<String, Object> supplier = executor.selectSupplierById(tenantId, supId);
        if (supplier == null || StringUtils.isEmpty(str(supplier.get("his_id"))))
        {
            throw new IllegalArgumentException("供应商未维护 HIS 对照(his_id)");
        }
        return str(supplier.get("his_id"));
    }

    private void markBillPushing(String tenantId, Long billId)
    {
        Map<String, Object> row = baseBillRow(tenantId, billId);
        row.put("hisPushStatus", MsunSpdBillPushConstants.PUSHING);
        row.put("hisPushMsg", null);
        executor.updateBillHisPushStatus(row);
    }

    private void markBillSuccess(String tenantId, Long billId, String traceId)
    {
        Map<String, Object> row = baseBillRow(tenantId, billId);
        row.put("hisPushStatus", MsunSpdBillPushConstants.PUSH_SUCCESS);
        row.put("hisPushMsg", null);
        row.put("hisTraceId", traceId);
        executor.updateBillHisPushStatus(row);
    }

    private void markBillFailed(String tenantId, Long billId, List<Map<String, Object>> entries, String message)
    {
        Map<String, Object> row = baseBillRow(tenantId, billId);
        row.put("hisPushStatus", MsunSpdBillPushConstants.PUSH_FAILED);
        row.put("hisPushMsg", truncate(message, 480));
        executor.updateBillHisPushStatus(row);
        if (entries != null)
        {
            for (Map<String, Object> entry : entries)
            {
                Long entryId = toLong(entry.get("entry_id"));
                if (entryId == null)
                {
                    continue;
                }
                String st = str(entry.get("his_push_status"));
                if (MsunSpdBillPushConstants.PUSH_SUCCESS.equals(st))
                {
                    continue;
                }
                Map<String, Object> er = new HashMap<>(8);
                er.put("tenantId", tenantId);
                er.put("entryId", entryId);
                er.put("hisPushStatus", MsunSpdBillPushConstants.PUSH_FAILED);
                er.put("hisPushMsg", truncate(message, 480));
                executor.updateEntryHisPushStatus(er);
            }
        }
    }

    private void markEntryPrepare(
            String tenantId, Long entryId, String memo, String spdDetailId, String drugId, String specId)
    {
        Map<String, Object> row = new HashMap<>(12);
        row.put("tenantId", tenantId);
        row.put("entryId", entryId);
        row.put("hisMemo", memo);
        row.put("hisSpdDetailId", spdDetailId);
        row.put("hisDrugId", drugId);
        row.put("hisDrugSpecPackingId", specId);
        row.put("hisPushStatus", MsunSpdBillPushConstants.PUSHING);
        executor.updateEntryHisPrepare(row);
    }

    private void markEntryPushResult(
            String tenantId, Long entryId, String pharmacyStockId,
            String storageStockId, String stockQueryId, String msg)
    {
        Map<String, Object> row = new HashMap<>(12);
        row.put("tenantId", tenantId);
        row.put("entryId", entryId);
        row.put("hisPharmacyStockId", pharmacyStockId);
        row.put("hisStorageStockId", storageStockId);
        row.put("hisStockQueryId", stockQueryId);
        row.put("hisPushStatus", MsunSpdBillPushConstants.PUSH_SUCCESS);
        row.put("hisPushMsg", msg);
        executor.updateEntryHisPushResult(row);
    }

    private static Map<String, Object> baseBillRow(String tenantId, Long billId)
    {
        Map<String, Object> row = new HashMap<>(8);
        row.put("tenantId", tenantId);
        row.put("billId", billId);
        return row;
    }

    private static Map<String, Object> extractLogMeta(Map<String, Object> bill)
    {
        Map<String, Object> meta = new HashMap<>(4);
        meta.put("spdBillId", bill.get("id"));
        meta.put("billNo", bill.get("bill_no"));
        meta.put("billType", bill.get("bill_type"));
        return meta;
    }

    private static long parseLongRequired(String val, String label)
    {
        if (StringUtils.isEmpty(val))
        {
            throw new IllegalArgumentException(label + " 不能为空");
        }
        try
        {
            return Long.parseLong(val.trim());
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(label + " 格式非法: " + val);
        }
    }

    /** 有效期：空值用占位截止时间；无法解析时抛错便于排错。 */
    private static String formatHisEffectiveDate(Object endTime)
    {
        if (StringUtils.isEmpty(str(endTime)))
        {
            return MsunSpdBillPushConstants.DEFAULT_OUTBOUND_EFFECTIVE_DATE;
        }
        String formatted = MsunHisDateTimeSupport.format(endTime);
        if (formatted != null)
        {
            return formatted;
        }
        throw new IllegalArgumentException("明细有效期(end_time)格式无效，要求 yyyy-MM-dd HH:mm:ss，实际="
                + endTime);
    }

    /** 批号：空或空白时传 '/'，避免 HIS 空值校验失败。 */
    private static String normalizeHisBatchNumber(Object batchNumber)
    {
        String batch = str(batchNumber);
        return StringUtils.isNotEmpty(batch) ? batch : MsunSpdBillPushConstants.DEFAULT_OUTBOUND_BATCH_NO;
    }

    private static String firstNonEmpty(String a, String b)
    {
        return StringUtils.isNotEmpty(a) ? a : b;
    }

    private static String truncate(String s, int max)
    {
        if (s == null)
        {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** 推送失败回显：去掉 MyBatis 堆栈，优先取根因。 */
    private static String summarizePushError(Throwable ex)
    {
        if (ex == null)
        {
            return "未知错误";
        }
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root)
        {
            root = root.getCause();
        }
        String msg = root.getMessage();
        if (StringUtils.isEmpty(msg))
        {
            msg = ex.getMessage();
        }
        if (StringUtils.isEmpty(msg))
        {
            return root.getClass().getSimpleName();
        }
        if (msg.contains("### Error"))
        {
            int causeIdx = msg.indexOf("Cause:");
            if (causeIdx >= 0)
            {
                String cause = msg.substring(causeIdx + 6).trim();
                int next = cause.indexOf("###");
                if (next > 0)
                {
                    cause = cause.substring(0, next).trim();
                }
                if (StringUtils.isNotEmpty(cause))
                {
                    msg = cause;
                }
            }
        }
        return truncate(msg, 480);
    }

    private static String trim(Object o)
    {
        if (o == null)
        {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String str(Object o)
    {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer toInteger(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof Number)
        {
            return ((Number) o).intValue();
        }
        try
        {
            return Integer.parseInt(String.valueOf(o));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private static Long toLong(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof Number)
        {
            return ((Number) o).longValue();
        }
        try
        {
            return Long.parseLong(String.valueOf(o));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private static int toInt(Object o, int defaultVal)
    {
        Integer n = toInteger(o);
        return n != null ? n : defaultVal;
    }
}
