package com.scminterface.customer.hengsuiThird.his.support;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import org.springframework.util.DigestUtils;
import com.scminterface.customer.hengsuiThird.his.model.HisInpatientChargeMirrorRow;
import com.scminterface.customer.hengsuiThird.his.model.HisOutpatientChargeMirrorRow;
import com.scminterface.customer.hengsuiThird.his.model.HisPatientChargeMirrorUnifiedRow;

/**
 * 衡水三院 HIS 计费镜像同步：指纹、时间与统一表行构造（逻辑对齐 SPD HisPatientChargeServiceImpl / HisPatientChargeMirrorUnifiedSupport）。
 */
public final class HisChargeMirrorSyncSupport
{
    private static final DateTimeFormatter PARSE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DATE_DISPLAY_FMT = "yyyy-MM-dd HH:mm:ss";

    private HisChargeMirrorSyncSupport()
    {
    }

    public static String toHisIdString(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof BigDecimal)
        {
            return ((BigDecimal) o).stripTrailingZeros().toPlainString();
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    public static String trimToNull(String s)
    {
        if (s == null)
        {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static BigDecimal toBigDecimal(Object o)
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
        try
        {
            return new BigDecimal(String.valueOf(o).trim());
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static Date parseHisDateTime(Object raw)
    {
        if (raw == null)
        {
            return null;
        }
        if (raw instanceof Timestamp)
        {
            return new Date(((Timestamp) raw).getTime());
        }
        if (raw instanceof Date)
        {
            return new Date(((Date) raw).getTime());
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty())
        {
            return null;
        }
        String normalized = text.replace('/', '-');
        if (normalized.length() == 10)
        {
            normalized = normalized + " 00:00:00";
        }
        if (normalized.length() > 19)
        {
            normalized = normalized.substring(0, 19);
        }
        try
        {
            LocalDateTime ldt = LocalDateTime.parse(normalized, PARSE_FMT);
            return Timestamp.valueOf(ldt);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public static Date parseChargeAtFromDisplay(String chargeDateDisplay)
    {
        if (chargeDateDisplay == null)
        {
            return null;
        }
        return parseHisDateTime(chargeDateDisplay);
    }

    public static String formatChargeDateDisplay(Date chargeDate)
    {
        if (chargeDate == null)
        {
            return null;
        }
        return new SimpleDateFormat(DATE_DISPLAY_FMT).format(chargeDate);
    }

    public static String fingerprintInpatient(HisInpatientChargeMirrorRow e)
    {
        String raw = String.join("|",
            nz(e.getHisInpatientChargeId()),
            nz(e.getHisInpatientChargeIdTf()),
            nz(e.getPatientId()),
            nz(e.getChargeItemId()),
            nz(e.getQuantity()),
            nz(e.getUnitPrice()),
            nz(e.getTotalAmount()),
            nz(e.getChargeDate()),
            nz(e.getDeptCode()));
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String fingerprintOutpatient(HisOutpatientChargeMirrorRow e)
    {
        String raw = String.join("|",
            nz(e.getHisOutpatientChargeId()),
            nz(e.getHisOutpatientChargeIdTf()),
            nz(e.getPatientId()),
            nz(e.getChargeItemId()),
            nz(e.getQuantity()),
            nz(e.getUnitPrice()),
            nz(e.getTotalAmount()),
            nz(e.getChargeDate()),
            nz(e.getClinicCode()));
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String nz(Object o)
    {
        if (o == null)
        {
            return "";
        }
        if (o instanceof BigDecimal)
        {
            return ((BigDecimal) o).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(o);
    }

    public static HisPatientChargeMirrorUnifiedRow unifiedFromInpatient(HisInpatientChargeMirrorRow e)
    {
        if (e == null)
        {
            return null;
        }
        HisPatientChargeMirrorUnifiedRow m = new HisPatientChargeMirrorUnifiedRow();
        m.setId(e.getId());
        m.setTenantId(e.getTenantId());
        m.setVisitKind("INPATIENT");
        m.setFetchBatchId(e.getFetchBatchId());
        m.setHisInpatientChargeId(e.getHisInpatientChargeId());
        m.setHisOutpatientChargeId(null);
        m.setHisInpatientChargeIdTf(e.getHisInpatientChargeIdTf());
        m.setHisOutpatientChargeIdTf(null);
        m.setPatientId(e.getPatientId());
        m.setPatientName(e.getPatientName());
        m.setInpatientNo(e.getInpatientNo());
        m.setOutpatientNo(null);
        m.setDeptCode(e.getDeptCode());
        m.setDeptName(e.getDeptName());
        m.setClinicCode(null);
        m.setClinicName(null);
        m.setDoctorId(e.getDoctorId());
        m.setDoctorName(e.getDoctorName());
        m.setChargeItemId(e.getChargeItemId());
        m.setItemName(e.getItemName());
        m.setSpecModel(e.getSpecModel());
        m.setBatchNo(e.getBatchNo());
        m.setExpireDate(e.getExpireDate());
        m.setUseDate(e.getUseDate());
        m.setChargeDateDisplay(formatChargeDateDisplay(e.getChargeDate()));
        m.setChargeAt(e.getChargeDate());
        m.setQuantity(e.getQuantity());
        m.setUnitPrice(e.getUnitPrice());
        m.setTotalAmount(e.getTotalAmount());
        m.setChargeOperator(e.getChargeOperator());
        m.setPaymentType(null);
        m.setReceiptNo(null);
        m.setRemark(e.getRemark());
        m.setRowFingerprint(e.getRowFingerprint());
        m.setProcessStatus(defaultProcessStatus(e.getProcessStatus()));
        m.setProcessType(null);
        m.setProcessTime(null);
        m.setProcessBy(null);
        m.setCreateBy(e.getCreateBy());
        m.setCreateTime(e.getCreateTime());
        return m;
    }

    public static HisPatientChargeMirrorUnifiedRow unifiedFromOutpatient(HisOutpatientChargeMirrorRow e)
    {
        if (e == null)
        {
            return null;
        }
        HisPatientChargeMirrorUnifiedRow m = new HisPatientChargeMirrorUnifiedRow();
        m.setId(e.getId());
        m.setTenantId(e.getTenantId());
        m.setVisitKind("OUTPATIENT");
        m.setFetchBatchId(e.getFetchBatchId());
        m.setHisInpatientChargeId(null);
        m.setHisOutpatientChargeId(e.getHisOutpatientChargeId());
        m.setHisInpatientChargeIdTf(null);
        m.setHisOutpatientChargeIdTf(e.getHisOutpatientChargeIdTf());
        m.setPatientId(e.getPatientId());
        m.setPatientName(e.getPatientName());
        m.setInpatientNo(null);
        m.setOutpatientNo(e.getOutpatientNo());
        m.setDeptCode(null);
        m.setDeptName(null);
        m.setClinicCode(e.getClinicCode());
        m.setClinicName(e.getClinicName());
        m.setDoctorId(e.getDoctorId());
        m.setDoctorName(e.getDoctorName());
        m.setChargeItemId(e.getChargeItemId());
        m.setItemName(e.getItemName());
        m.setSpecModel(e.getSpecModel());
        m.setBatchNo(e.getBatchNo());
        m.setExpireDate(e.getExpireDate());
        m.setUseDate(null);
        String disp = e.getChargeDate();
        if (disp != null)
        {
            disp = disp.trim();
            if (disp.length() > 64)
            {
                disp = disp.substring(0, 64);
            }
        }
        m.setChargeDateDisplay(disp);
        m.setChargeAt(parseChargeAtFromDisplay(disp));
        m.setQuantity(e.getQuantity());
        m.setUnitPrice(e.getUnitPrice());
        m.setTotalAmount(e.getTotalAmount());
        m.setChargeOperator(e.getChargeOperator());
        m.setPaymentType(e.getPaymentType());
        m.setReceiptNo(e.getReceiptNo());
        m.setRemark(e.getRemark());
        m.setRowFingerprint(e.getRowFingerprint());
        m.setProcessStatus(defaultProcessStatus(e.getProcessStatus()));
        m.setProcessType(null);
        m.setProcessTime(null);
        m.setProcessBy(null);
        m.setCreateBy(e.getCreateBy());
        m.setCreateTime(e.getCreateTime());
        return m;
    }

    private static String defaultProcessStatus(String s)
    {
        if (s == null || s.trim().isEmpty())
        {
            return "PENDING_CONSUME";
        }
        return s;
    }

    public static boolean isBlank(String s)
    {
        return s == null || s.trim().isEmpty();
    }

    public static boolean fingerprintEquals(String a, String b)
    {
        return Objects.equals(a, b);
    }
}
