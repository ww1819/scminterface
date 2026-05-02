package com.scminterface.framework.web.service;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scminterface.framework.web.mapper.ScmPartyLookupMapper;

/**
 * 根据医院/供应商主键解析「当时」平台侧医院-供应商绑定状态（写入订单快照）。
 */
@Service
public class HospitalSupplierBindSnapshotService
{
    public static final String SNAP_HOSPITAL_UNRESOLVED = "医院未解析";
    public static final String SNAP_SUPPLIER_UNRESOLVED = "供应商未解析";
    public static final String SNAP_NOT_BOUND = "未绑定";
    public static final String SNAP_AUDIT_PENDING = "申请审核中";
    public static final String SNAP_BOUND = "已绑定";
    public static final String SNAP_UNBOUND = "已解绑";
    public static final String SNAP_EXPIRED = "绑定已过期";
    public static final String SNAP_DISABLED = "已停用";
    public static final String SNAP_AUDIT_REJECTED = "审核未通过";

    @Autowired
    private ScmPartyLookupMapper scmPartyLookupMapper;

    /**
     * @param hospitalId 平台医院主键，可为 null（编码未匹配到主数据）
     * @param supplierId 平台供应商主键，可为 null
     * @return 中文快照枚举，用于 hs_bind_snapshot 列
     */
    public String resolve(Long hospitalId, Long supplierId)
    {
        if (hospitalId == null)
        {
            return SNAP_HOSPITAL_UNRESOLVED;
        }
        if (supplierId == null)
        {
            return SNAP_SUPPLIER_UNRESOLVED;
        }
        Map<String, Object> row = scmPartyLookupMapper.selectHospitalSupplierRelationRow(hospitalId, supplierId);
        if (row == null || row.isEmpty())
        {
            int pending = scmPartyLookupMapper.countPendingHospitalSupplierApply(String.valueOf(hospitalId),
                String.valueOf(supplierId));
            return pending > 0 ? SNAP_AUDIT_PENDING : SNAP_NOT_BOUND;
        }
        String bindStatus = str(row.get("bindStatus"));
        String auditStatus = str(row.get("auditStatus"));
        String disableStatus = str(row.get("disableStatus"));
        LocalDate end = toLocalDate(row.get("supplyEndDate"));

        if ("2".equals(bindStatus))
        {
            return SNAP_UNBOUND;
        }
        if ("1".equals(disableStatus))
        {
            return SNAP_DISABLED;
        }
        if (end != null && end.isBefore(LocalDate.now()))
        {
            return SNAP_EXPIRED;
        }
        if ("2".equals(auditStatus))
        {
            return SNAP_AUDIT_REJECTED;
        }
        if ("0".equals(bindStatus) || "0".equals(auditStatus))
        {
            return SNAP_AUDIT_PENDING;
        }
        if ("1".equals(bindStatus) && ("1".equals(auditStatus) || auditStatus == null || auditStatus.isEmpty()))
        {
            return SNAP_BOUND;
        }
        return SNAP_NOT_BOUND;
    }

    private static String str(Object o)
    {
        if (o == null)
        {
            return "";
        }
        String s = String.valueOf(o).trim();
        return s;
    }

    private static LocalDate toLocalDate(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof java.sql.Date)
        {
            return ((java.sql.Date) o).toLocalDate();
        }
        if (o instanceof java.util.Date)
        {
            return new java.sql.Date(((java.util.Date) o).getTime()).toLocalDate();
        }
        if (o instanceof LocalDate)
        {
            return (LocalDate) o;
        }
        return null;
    }
}
