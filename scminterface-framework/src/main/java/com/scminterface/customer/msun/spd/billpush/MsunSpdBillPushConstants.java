package com.scminterface.customer.msun.spd.billpush;

/**
 * 众阳 HIS 单据推送状态与报文字段常量（与 SPD {@code MsunHisConstants} 对齐）。
 */
public final class MsunSpdBillPushConstants
{
    private MsunSpdBillPushConstants()
    {
    }

    public static final String PUSH_NOT = "0";
    public static final String PUSHING = "1";
    public static final String PUSH_SUCCESS = "2";
    public static final String PUSH_FAILED = "3";

    public static final String IN_STOCK_STATUS_PHARMACY = "";
    public static final String SAVE_CORRELATION_FLAG = "1";
    public static final String RETURN_TO_SUPPLIER_YES = "1";

    public static final String SPD_DETAIL_ID_SEP = ":";

    public static String buildEntryMemo(String tenantId, Long entryId)
    {
        return "ZQ-" + tenantId + "-" + entryId;
    }

    public static String buildSpdDetailId(Long billMainId, Long entryDetailId)
    {
        if (billMainId == null || entryDetailId == null)
        {
            return null;
        }
        return billMainId + SPD_DETAIL_ID_SEP + entryDetailId;
    }
}
