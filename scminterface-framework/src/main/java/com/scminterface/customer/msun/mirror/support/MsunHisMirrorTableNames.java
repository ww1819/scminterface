package com.scminterface.customer.msun.mirror.support;

import com.scminterface.customer.msun.MsunVendorConstants;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 众阳（msun）HIS 镜像表名：{@code m_{厂商英文}_{对象名}}，与其他厂家镜像表隔离。
 */
public final class MsunHisMirrorTableNames
{
    /** 表名前缀，如 {@code m_msun_} */
    public static final String PREFIX = "m_" + MsunVendorConstants.VENDOR_CODE + "_";

    public static final String SYNC_BATCH = PREFIX + "sync_batch";
    public static final String DEPT = PREFIX + "dept";
    public static final String DEPT_CATEGORY_REL = PREFIX + "dept_category_rel";
    public static final String USER_IDENTITY = PREFIX + "user_identity";
    public static final String USER_IDENTITY_ACCOUNT = PREFIX + "user_identity_account";
    public static final String DRUG_DICT = PREFIX + "drug_dict";
    public static final String DICT_CATEGORY = PREFIX + "dict_category";
    public static final String SUPPLIER = PREFIX + "supplier";
    public static final String PRODUCER = PREFIX + "producer";
    public static final String MERGE_STOCK = PREFIX + "merge_stock";
    public static final String DRUG_BATCH_STOCK = PREFIX + "drug_batch_stock";
    public static final String YK_INSTOCK = PREFIX + "yk_instock";
    public static final String YK_INSTOCK_DETAIL = PREFIX + "yk_instock_detail";
    public static final String PUSH_LOG = PREFIX + "push_log";

    private static final Set<String> ALL = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            SYNC_BATCH, DEPT, DEPT_CATEGORY_REL, USER_IDENTITY, USER_IDENTITY_ACCOUNT,
            DRUG_DICT, DICT_CATEGORY, SUPPLIER, PRODUCER, MERGE_STOCK, DRUG_BATCH_STOCK,
            YK_INSTOCK, YK_INSTOCK_DETAIL, PUSH_LOG)));

    private MsunHisMirrorTableNames()
    {
    }

    public static Set<String> allTableNames()
    {
        return ALL;
    }

    public static boolean isMirrorTable(String table)
    {
        return table != null && ALL.contains(table);
    }
}
