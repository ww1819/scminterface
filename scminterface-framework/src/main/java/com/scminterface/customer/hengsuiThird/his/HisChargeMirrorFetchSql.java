package com.scminterface.customer.hengsuiThird.his;

/**
 * HIS 计费镜像抓取 SQL（与 SPD HisChargeMirrorFetchSql 区间语义对齐：昨天 0 点起至明天 0 点前，即昨天+今天）。
 */
public final class HisChargeMirrorFetchSql
{
    private HisChargeMirrorFetchSql()
    {
    }

    /**
     * 昨天 00:00:00（含）至明天 00:00:00（不含），覆盖「昨天 + 今天」两个自然日。
     */
    private static final String CHARGE_DATE_YESTERDAY_TODAY =
        "charge_date >= DATEADD(day, -1, CAST(CAST(GETDATE() AS date) AS datetime)) "
            + "AND charge_date < DATEADD(day, 1, CAST(CAST(GETDATE() AS date) AS datetime))";

    public static final String SQLSERVER_INPATIENT_YESTERDAY_TODAY =
        "SELECT inpatient_charge_id, inpatient_charge_id_tf, patient_id, patient_name, inpatient_no, dept_code, dept_name, "
            + "exec_dept_id, exec_dept_name, "
            + "doctor_id, doctor_name, charge_item_id, item_name, spec_model, batch_no, expire_date, "
            + "use_date, charge_date, quantity, unit_price, total_amount, charge_operator, remark "
            + "FROM v_inpatient_consumable_charge "
            + "WHERE charge_date IS NOT NULL AND charge_date <> '' AND " + CHARGE_DATE_YESTERDAY_TODAY;

    public static final String SQLSERVER_OUTPATIENT_YESTERDAY_TODAY =
        "SELECT outpatient_charge_id, outpatient_charge_id_tf, patient_id, patient_name, outpatient_no, clinic_code, clinic_name, "
            + "exec_dept_id, exec_dept_name, "
            + "doctor_id, doctor_name, charge_item_id, item_name, spec_model, batch_no, expire_date, "
            + "charge_date, quantity, unit_price, total_amount, charge_operator, payment_type, receipt_no, remark "
            + "FROM v_outpatient_consumable_charge "
            + "WHERE charge_date IS NOT NULL AND charge_date <> '' AND " + CHARGE_DATE_YESTERDAY_TODAY;

    /** 按计费时间区间抓取（下界含、上界不含），用于历史执行科室补全 */
    public static final String SQLSERVER_INPATIENT_RANGE =
        "SELECT inpatient_charge_id, inpatient_charge_id_tf, patient_id, patient_name, inpatient_no, dept_code, dept_name, "
            + "exec_dept_id, exec_dept_name, "
            + "doctor_id, doctor_name, charge_item_id, item_name, spec_model, batch_no, expire_date, "
            + "use_date, charge_date, quantity, unit_price, total_amount, charge_operator, remark "
            + "FROM v_inpatient_consumable_charge "
            + "WHERE charge_date IS NOT NULL AND charge_date <> '' "
            + "AND charge_date >= ? AND charge_date < ?";

    public static final String SQLSERVER_OUTPATIENT_RANGE =
        "SELECT outpatient_charge_id, outpatient_charge_id_tf, patient_id, patient_name, outpatient_no, clinic_code, clinic_name, "
            + "exec_dept_id, exec_dept_name, "
            + "doctor_id, doctor_name, charge_item_id, item_name, spec_model, batch_no, expire_date, "
            + "charge_date, quantity, unit_price, total_amount, charge_operator, payment_type, receipt_no, remark "
            + "FROM v_outpatient_consumable_charge "
            + "WHERE charge_date IS NOT NULL AND charge_date <> '' "
            + "AND charge_date >= ? AND charge_date < ?";

    /** @deprecated 使用 {@link #SQLSERVER_INPATIENT_YESTERDAY_TODAY} */
    @Deprecated
    public static final String SQLSERVER_INPATIENT_RECENT_3D = SQLSERVER_INPATIENT_YESTERDAY_TODAY;

    /** @deprecated 使用 {@link #SQLSERVER_OUTPATIENT_YESTERDAY_TODAY} */
    @Deprecated
    public static final String SQLSERVER_OUTPATIENT_RECENT_3D = SQLSERVER_OUTPATIENT_YESTERDAY_TODAY;
}
