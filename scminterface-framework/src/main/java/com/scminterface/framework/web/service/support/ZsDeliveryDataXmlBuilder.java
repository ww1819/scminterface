package com.scminterface.framework.web.service.support;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.domain.zs.ScmDeliveryDetailBarcodeRow;
import com.scminterface.framework.domain.zs.ScmDeliveryDetailXmlRow;
import com.scminterface.framework.domain.zs.ScmDeliveryXmlRow;
import com.scminterface.framework.domain.zs.ZsTpOrderDetailDsbRow;
import com.scminterface.framework.domain.zs.ZsTpOrderXmlRow;

/**
 * 中设配送单数据 XML（ROOT/LIST）与业务字段映射。
 * <p>
 * 每条 LIST 含 {@code CUSTOMER}（scm_delivery.zs_customer_id）。<br>
 * 有条码时：每个条码一条 LIST，ZZS 为单枚种子，SL 为 1，BZ 优先取条码号。<br>
 * 无条码时：按配送明细每条一行，SL 为配送数量，ZZS 为空。
 */
public final class ZsDeliveryDataXmlBuilder
{
    private static final String DATE_FMT = "yyyy-MM-dd";

    private static final String SL_ONE_PER_BARCODE = "1";

    private ZsDeliveryDataXmlBuilder()
    {
    }

    public static String build(ScmDeliveryXmlRow delivery, List<ScmDeliveryDetailXmlRow> details, ZsTpOrderXmlRow z,
        Map<String, ZsTpOrderDetailDsbRow> zsDetailById)
    {
        StringBuilder sb = new StringBuilder(Math.max(4096, details.size() * 512));
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        sb.append("<ROOT>\r\n");
        int num = 0;
        for (ScmDeliveryDetailXmlRow dd : details)
        {
            ZsTpOrderDetailDsbRow zd =
                dd.getZsOrderDetailId() != null ? zsDetailById.get(dd.getZsOrderDetailId()) : null;
            List<ScmDeliveryDetailBarcodeRow> bars = dd.getDetailBarcodes();
            if (bars != null && !bars.isEmpty())
            {
                List<ScmDeliveryDetailBarcodeRow> sorted = new ArrayList<ScmDeliveryDetailBarcodeRow>(bars);
                sorted.sort(Comparator.comparing(b -> b.getSeedNum() != null ? b.getSeedNum() : 0L));
                for (ScmDeliveryDetailBarcodeRow b : sorted)
                {
                    String bz = firstNonBlank(b.getBarcodeNo(), dd.getMainBarcode(), dd.getMaterialName());
                    String zzs = b.getSeedNum() != null ? String.valueOf(b.getSeedNum()) : "";
                    num = appendListRow(sb, num, delivery, dd, z, zd, SL_ONE_PER_BARCODE, zzs, bz);
                }
            }
            else
            {
                String bz = firstNonBlank(dd.getMainBarcode(), dd.getMaterialName());
                num = appendListRow(sb, num, delivery, dd, z, zd, decPlain(dd.getDeliveryQuantity()), "", bz);
            }
        }
        sb.append("</ROOT>\r\n");
        return sb.toString();
    }

    private static int appendListRow(StringBuilder sb, int num, ScmDeliveryXmlRow delivery, ScmDeliveryDetailXmlRow dd,
        ZsTpOrderXmlRow z, ZsTpOrderDetailDsbRow zd, String sl, String zzs, String bz)
    {
        sb.append("  <LIST>\r\n");
        appendEl(sb, "NUM", String.valueOf(num++));
        appendEl(sb, "CUSTOMER", delivery.getZsCustomerId() != null ? delivery.getZsCustomerId() : "");
        appendEl(sb, "DH", z != null ? z.getDh() : "");
        appendEl(sb, "SUP", z != null ? z.getSupno() : "");
        appendEl(sb, "CODE", dd.getMaterialCode());
        appendEl(sb, "SL", sl);
        appendEl(sb, "PH", dd.getBatchNo());
        appendEl(sb, "MJPH", dd.getAuxBarcode());
        appendEl(sb, "YXQ", dateStr(dd.getExpireDate()));
        appendEl(sb, "SCRQ", dateStr(dd.getProductionDate()));
        appendEl(sb, "FP", delivery.getInvoiceNo());
        appendEl(sb, "CK", firstNonBlank(delivery.getSrcOrderWarehouseName(), z != null ? z.getCk() : null, delivery.getWarehouse()));
        appendEl(sb, "BZ", bz);
        appendEl(sb, "FPDATE", dateStr(delivery.getInvoiceDate()));
        appendEl(sb, "SRM", "");
        appendEl(sb, "TX", "0");
        appendEl(sb, "ZFP", "");
        appendEl(sb, "KSBH", z != null ? z.getKsbh() : "");
        appendEl(sb, "GZFLAG", firstNonBlank(delivery.getZsJsfs(), z != null ? z.getJsfs() : null));
        appendEl(sb, "DJ", decPlain(dd.getPrice()));
        appendEl(sb, "PRINT_PS", "1");
        appendEl(sb, "PRINT_ZL", "1");
        appendEl(sb, "ZCZ", dd.getRegisterNo());
        appendEl(sb, "ZJLY", z != null ? z.getZjly() : "");
        appendEl(sb, "ZZS", zzs);
        appendEl(sb, "SHRQ", dateStr(delivery.getAuditTime()));
        appendEl(sb, "ZTM", "");
        appendEl(sb, "FTM", "");
        appendEl(sb, "DSB", zd != null ? decPlain(zd.getDsb()) : "");
        appendEl(sb, "CKBH", z != null ? z.getCkno() : "");
        appendEl(sb, "PSDH", delivery.getDeliveryNo());
        appendEl(sb, "WD", "");
        appendEl(sb, "DW", dd.getUnit());
        appendEl(sb, "MJRQ", "");
        appendEl(sb, "FPJE", decPlain(delivery.getInvoiceAmount()));
        sb.append("  </LIST>\r\n");
        return num;
    }

    private static void appendEl(StringBuilder sb, String tag, String value)
    {
        sb.append("    <").append(tag).append('>');
        sb.append(xmlEscape(value));
        sb.append("</").append(tag).append(">\r\n");
    }

    private static String xmlEscape(String s)
    {
        if (s == null || s.isEmpty())
        {
            return "";
        }
        String t = s;
        t = t.replace("&", "&amp;");
        t = t.replace("<", "&lt;");
        t = t.replace(">", "&gt;");
        t = t.replace("\"", "&quot;");
        return t;
    }

    private static String firstNonBlank(String a, String b, String c)
    {
        if (StringUtils.isNotEmpty(a))
        {
            return a;
        }
        if (StringUtils.isNotEmpty(b))
        {
            return b;
        }
        return StringUtils.trimToEmpty(c);
    }

    private static String firstNonBlank(String a, String b)
    {
        if (StringUtils.isNotEmpty(a))
        {
            return a;
        }
        return StringUtils.trimToEmpty(b);
    }

    private static String dateStr(Date d)
    {
        if (d == null)
        {
            return "";
        }
        return new SimpleDateFormat(DATE_FMT).format(d);
    }

    private static String decPlain(BigDecimal v)
    {
        if (v == null)
        {
            return "";
        }
        return v.stripTrailingZeros().toPlainString();
    }
}
