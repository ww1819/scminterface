package com.scminterface.framework.web.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.util.ZsUuid7;
import com.scminterface.framework.web.mapper.ScmPartyLookupMapper;

/**
 * 供 SPD 经前置机拉取的平台供应商主数据（SCM 库）
 */
@Service
public class ScmSupplierInterfaceService
{
    @Autowired
    private ScmPartyLookupMapper scmPartyLookupMapper;

    @DataSource(DataSourceType.SCM)
    public Map<String, Object> buildSupplierProfile(String hospitalCode, String supplierCode,
        String spdTenantId, String requestIp, String createBy)
    {
        Map<String, Object> row = scmPartyLookupMapper.selectScmSupplierRowByCode(supplierCode);
        if (row == null || row.isEmpty())
        {
            return null;
        }
        Map<String, Object> relation = scmPartyLookupMapper.selectHospitalSupplierRelationByCodes(
            hospitalCode, supplierCode);
        String relationStatus = resolveRelationStatus(relation);
        boolean downloadable = StringUtils.isNotEmpty(relationStatus);
        boolean activeBound = "ACTIVE".equals(relationStatus);
        String scope = downloadable ? "FULL" : "LIMITED";
        scmPartyLookupMapper.insertScmSupplierExportLog(ZsUuid7.newString(), hospitalCode, supplierCode, scope,
            spdTenantId, requestIp, createBy != null ? createBy : "api");
        Map<String, Object> out = new HashMap<>();
        out.put("hospitalSupplierBound", activeBound);
        out.put("downloadable", downloadable);
        out.put("relationStatus", relationStatus);
        out.put("relationStatusLabel", relationStatusLabel(relationStatus));
        out.put("exportScope", scope);
        out.put("hospitalRelation", enrichRelationSnapshot(relation, relationStatus));
        out.put("supplier", row);
        return out;
    }

    @DataSource(DataSourceType.SCM)
    public List<Map<String, Object>> listSuppliersByHospital(String hospitalCode)
    {
        List<Map<String, Object>> rows = scmPartyLookupMapper.selectSuppliersByHospitalCode(hospitalCode);
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null)
        {
            return out;
        }
        for (Map<String, Object> row : rows)
        {
            if (row == null)
            {
                continue;
            }
            String relationStatus = resolveRelationStatus(row);
            if (StringUtils.isEmpty(relationStatus))
            {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>(row);
            m.put("relationStatus", relationStatus);
            m.put("relationStatusLabel", relationStatusLabel(relationStatus));
            m.put("downloadable", Boolean.TRUE);
            m.put("hospitalSupplierBound", Boolean.valueOf("ACTIVE".equals(relationStatus)));
            out.add(m);
        }
        return out;
    }

    /**
     * 优先级：UNBOUND &gt; DISABLED &gt; EXPIRED &gt; ACTIVE；不可下载返回 null。
     */
    static String resolveRelationStatus(Map<String, Object> relation)
    {
        if (relation == null || relation.isEmpty())
        {
            return null;
        }
        String bind = trim(relation.get("bindStatus"));
        String audit = trim(relation.get("auditStatus"));
        String disable = trim(relation.get("disableStatus"));
        String rowStatus = trim(relation.get("relationRowStatus"));
        if (rowStatus == null)
        {
            rowStatus = trim(relation.get("status"));
        }

        if ("0".equals(bind) || "0".equals(audit))
        {
            return null;
        }
        boolean everApproved = "1".equals(audit)
            || ((audit == null || audit.isEmpty()) && ("1".equals(bind) || "2".equals(bind)));
        if (!everApproved)
        {
            return null;
        }

        if ("2".equals(bind))
        {
            return "UNBOUND";
        }
        if ("1".equals(disable) || "1".equals(rowStatus))
        {
            return "DISABLED";
        }
        if (isSupplyEnded(relation.get("supplyEndDate")))
        {
            return "EXPIRED";
        }
        if ("1".equals(bind) || bind == null || bind.isEmpty())
        {
            return "ACTIVE";
        }
        return null;
    }

    static String relationStatusLabel(String status)
    {
        if (status == null)
        {
            return null;
        }
        switch (status)
        {
            case "ACTIVE":
                return "有效";
            case "EXPIRED":
                return "已过期";
            case "DISABLED":
                return "已停用";
            case "UNBOUND":
                return "已解绑";
            default:
                return status;
        }
    }

    private static Map<String, Object> enrichRelationSnapshot(Map<String, Object> relation, String relationStatus)
    {
        Map<String, Object> snap = new LinkedHashMap<>();
        if (relation != null)
        {
            snap.putAll(relation);
        }
        snap.put("relationStatus", relationStatus);
        snap.put("relationStatusLabel", relationStatusLabel(relationStatus));
        snap.put("downloadable", Boolean.valueOf(StringUtils.isNotEmpty(relationStatus)));
        return snap;
    }

    private static boolean isSupplyEnded(Object endDate)
    {
        LocalDate end = toLocalDate(endDate);
        if (end == null)
        {
            return false;
        }
        return end.isBefore(LocalDate.now());
    }

    private static LocalDate toLocalDate(Object v)
    {
        if (v == null)
        {
            return null;
        }
        if (v instanceof LocalDate)
        {
            return (LocalDate) v;
        }
        if (v instanceof java.sql.Date)
        {
            return ((java.sql.Date) v).toLocalDate();
        }
        if (v instanceof Date)
        {
            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String s = String.valueOf(v).trim();
        if (s.length() >= 10)
        {
            try
            {
                return LocalDate.parse(s.substring(0, 10));
            }
            catch (Exception ignored)
            {
                return null;
            }
        }
        return null;
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
}
