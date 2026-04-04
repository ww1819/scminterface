package com.scminterface.framework.web.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.util.ZsUuid7;
import com.scminterface.framework.web.mapper.ScmBarcodeSeedInitMapper;
import com.scminterface.framework.web.mapper.ZsTpOrderMapper;

/**
 * 解析第三方推送的订单主表/明细 JSON，落库 zs_tp_order / zs_tp_order_detail。
 * <p>
 * CUSTOMER：第三方服务标识，用于区分不同上游系统。
 */
@Service
public class ZsOrderReceiveService
{
    private static final Logger log = LoggerFactory.getLogger(ZsOrderReceiveService.class);

    private static final String DEL_BY_PUSH = "zs-receive";

    @Autowired
    private ZsTpOrderMapper zsTpOrderMapper;

    @Autowired
    private ScmBarcodeSeedInitMapper scmBarcodeSeedInitMapper;

    @DataSource(DataSourceType.SCM)
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult receiveAndSave(Map<String, Object> body)
    {
        if (body == null || body.isEmpty())
        {
            return AjaxResult.error("请求体不能为空");
        }

        Map<String, Object> masterRow;
        try
        {
            masterRow = extractSingleMaster(body.get("master"));
        }
        catch (IllegalArgumentException e)
        {
            return AjaxResult.error(e.getMessage());
        }

        String customer = firstNonBlank(str(body.get("CUSTOMER")), str(masterRow.get("CUSTOMER")));
        if (isBlank(customer))
        {
            return AjaxResult.error("缺少第三方服务标识 CUSTOMER（可放在根节点或 master 内）");
        }

        List<Map<String, Object>> detailRows = extractDetails(body.get("details"));
        if (masterRow.get("thirdPartyPk") == null)
        {
            masterRow.put("thirdPartyPk", inferThirdPartyPk(masterRow, "1"));
        }

        Map<String, Object> normalized = new LinkedHashMap<>(4);
        normalized.put("CUSTOMER", customer);
        normalized.put("master", masterRow);
        normalized.put("details", detailRows);

        String dh = str(masterRow.get("DH"));
        if (isBlank(dh))
        {
            return AjaxResult.error("主表缺少单号 DH");
        }

        String existingId = zsTpOrderMapper.selectActiveOrderIdByCustomerAndDh(customer, dh);
        if (existingId != null)
        {
            zsTpOrderMapper.softDeleteDetailsByOrderId(existingId, DEL_BY_PUSH);
            zsTpOrderMapper.softDeleteOrderById(existingId, DEL_BY_PUSH);
        }

        String receiveChannel = firstNonBlank(str(body.get("RECEIVE_CHANNEL")), str(masterRow.get("RECEIVE_CHANNEL")));
        if (isBlank(receiveChannel))
        {
            receiveChannel = ZsBarcodeSeedConstants.CHANNEL_ZS;
        }

        String orderId = ZsUuid7.newString();
        Map<String, Object> orderInsert = buildOrderInsert(orderId, customer, masterRow, receiveChannel);
        zsTpOrderMapper.insertOrder(orderInsert);

        String tenantId = str(body.get("TENANT_ID"));
        if (tenantId == null)
        {
            tenantId = "";
        }
        String ckno = str(masterRow.get("CKNO"));
        if (ckno == null)
        {
            ckno = "";
        }
        if (ZsBarcodeSeedConstants.CHANNEL_TENANT.equalsIgnoreCase(receiveChannel))
        {
            scmBarcodeSeedInitMapper.ensureTenantSeed(ZsUuid7.newString(), tenantId, ckno, "L");
        }
        else
        {
            scmBarcodeSeedInitMapper.ensureZsCustomerSeed(ZsUuid7.newString(), tenantId, customer, ckno, "L");
        }

        int line = 0;
        for (Map<String, Object> d : detailRows)
        {
            line++;
            Map<String, Object> lineMap = new LinkedHashMap<>(d);
            if (lineMap.get("thirdPartyPk") == null)
            {
                lineMap.put("thirdPartyPk", inferThirdPartyPk(lineMap, String.valueOf(line)));
            }
            Map<String, Object> detailInsert = buildDetailInsert(ZsUuid7.newString(), orderId, lineMap);
            zsTpOrderMapper.insertDetail(detailInsert);
        }

        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("orderId", orderId);
        result.put("detailCount", detailRows.size());
        result.put("normalizedPayload", normalized);

        log.info("ZS 订单已落库 customer={} dh={} orderId={} lines={}", customer, dh, orderId, detailRows.size());
        return AjaxResult.success("接收并保存成功", result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractSingleMaster(Object masterRaw)
    {
        if (masterRaw == null)
        {
            throw new IllegalArgumentException("缺少 master 节点");
        }
        if (!(masterRaw instanceof Map))
        {
            throw new IllegalArgumentException("master 须为 JSON 对象");
        }
        Map<String, Object> raw = (Map<String, Object>) masterRaw;

        if (raw.containsKey("thirdPartyPk") || looksLikeFlatMasterRow(raw))
        {
            return new LinkedHashMap<>(raw);
        }

        Map<String, Object> onlyChild = null;
        String outerKey = null;
        for (Map.Entry<String, Object> e : raw.entrySet())
        {
            if (e.getValue() instanceof Map)
            {
                if (onlyChild != null)
                {
                    throw new IllegalArgumentException("master 暂只支持单行：请改为 { thirdPartyPk, ... } 或 { \"1\": { ... } } 单键");
                }
                outerKey = e.getKey();
                onlyChild = new LinkedHashMap<>((Map<String, Object>) e.getValue());
            }
        }
        if (onlyChild == null)
        {
            throw new IllegalArgumentException("master 格式无法识别");
        }
        onlyChild.putIfAbsent("thirdPartyPk", outerKey);
        return onlyChild;
    }

    private static boolean looksLikeFlatMasterRow(Map<String, Object> raw)
    {
        return raw.containsKey("DH") || raw.containsKey("SHEET_JE") || raw.containsKey("SUPNO");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractDetails(Object detailsRaw)
    {
        if (detailsRaw == null)
        {
            return new ArrayList<>();
        }
        if (detailsRaw instanceof List)
        {
            List<?> list = (List<?>) detailsRaw;
            List<Map<String, Object>> out = new ArrayList<>();
            int i = 0;
            for (Object o : list)
            {
                i++;
                if (o instanceof Map)
                {
                    Map<String, Object> row = new LinkedHashMap<>((Map<String, Object>) o);
                    row.putIfAbsent("thirdPartyPk", String.valueOf(i));
                    out.add(row);
                }
            }
            return out;
        }
        if (detailsRaw instanceof Map)
        {
            Map<String, Object> dm = (Map<String, Object>) detailsRaw;
            List<String> keys = new ArrayList<>(dm.keySet());
            keys.sort(Comparator.comparingInt(ZsOrderReceiveService::parseIntKey));
            List<Map<String, Object>> out = new ArrayList<>();
            for (String k : keys)
            {
                Object v = dm.get(k);
                if (v instanceof Map)
                {
                    Map<String, Object> row = new LinkedHashMap<>((Map<String, Object>) v);
                    row.putIfAbsent("thirdPartyPk", k);
                    out.add(row);
                }
            }
            return out;
        }
        return new ArrayList<>();
    }

    private static int parseIntKey(String k)
    {
        try
        {
            return Integer.parseInt(k);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private static String inferThirdPartyPk(Map<String, Object> row, String fallback)
    {
        Object v = row.get("thirdPartyPk");
        if (v != null)
        {
            return String.valueOf(v);
        }
        return fallback;
    }

    private static Map<String, Object> buildOrderInsert(String id, String customer, Map<String, Object> row, String receiveChannel)
    {
        Map<String, Object> m = new HashMap<>(32);
        m.put("id", id);
        m.put("thirdPartyPk", Objects.toString(row.get("thirdPartyPk"), ""));
        m.put("customer", customer);
        m.put("receiveChannel", receiveChannel);
        m.put("sheetJe", toDecimal(row.get("SHEET_JE")));
        m.put("dh", str(row.get("DH")));
        m.put("supno", str(row.get("SUPNO")));
        m.put("sup", str(row.get("SUP")));
        m.put("supno2", str(row.get("SUPNO2")));
        m.put("sup2", str(row.get("SUP2")));
        m.put("ckno", str(row.get("CKNO")));
        m.put("ck", str(row.get("CK")));
        m.put("pc", str(row.get("PC")));
        m.put("oper", str(row.get("OPER")));
        m.put("bz", str(row.get("BZ")));
        m.put("jsfs", str(row.get("JSFS")));
        m.put("ksbh", str(row.get("KSBH")));
        m.put("ksmc", str(row.get("KSMC")));
        m.put("zjly", str(row.get("ZJLY")));
        m.put("createBy", DEL_BY_PUSH);
        return m;
    }

    private static Map<String, Object> buildDetailInsert(String id, String orderId, Map<String, Object> row)
    {
        Map<String, Object> m = new HashMap<>(32);
        m.put("id", id);
        m.put("orderId", orderId);
        m.put("thirdPartyPk", Objects.toString(row.get("thirdPartyPk"), ""));
        m.put("dh", str(row.get("DH")));
        m.put("code", str(row.get("CODE")));
        m.put("name", str(row.get("NAME")));
        m.put("gg", str(row.get("GG")));
        m.put("dw", str(row.get("DW")));
        m.put("bzl", str(row.get("BZL")));
        m.put("sccj", str(row.get("SCCJ")));
        m.put("zcz", str(row.get("ZCZ")));
        m.put("phflag", str(row.get("PHFLAG")));
        m.put("sl", toDecimal(row.get("SL")));
        m.put("dj", toDecimal(row.get("DJ")));
        m.put("je", toDecimal(row.get("JE")));
        m.put("jm", str(row.get("JM")));
        m.put("cgj", str(row.get("CGJ")));
        m.put("bz", str(row.get("BZ")));
        m.put("bz1", str(row.get("BZ1")));
        m.put("bz2", str(row.get("BZ2")));
        m.put("dsb", toDecimal(row.get("DSB")));
        return m;
    }

    private static BigDecimal toDecimal(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof BigDecimal)
        {
            return (BigDecimal) o;
        }
        if (o instanceof Number)
        {
            return BigDecimal.valueOf(((Number) o).doubleValue());
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty())
        {
            return null;
        }
        return new BigDecimal(s);
    }

    private static String str(Object o)
    {
        if (o == null)
        {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String firstNonBlank(String a, String b)
    {
        if (!isBlank(a))
        {
            return a;
        }
        return isBlank(b) ? null : b;
    }

    private static boolean isBlank(String s)
    {
        return s == null || s.trim().isEmpty();
    }
}
