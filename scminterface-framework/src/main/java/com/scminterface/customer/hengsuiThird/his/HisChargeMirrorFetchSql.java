package com.scminterface.customer.hengsuiThird.his;

/**
 * HIS 计费镜像抓取 SQL（与 SPD HisChargeMirrorFetchSql 对齐）。
 */
public final class HisChargeMirrorFetchSql
{
    private HisChargeMirrorFetchSql()
    {
    }

    public static final String SQLSERVER_INPATIENT_RECENT_3D =
        "SELECT inpatient_charge_id, inpatient_charge_id_tf, patient_id, patient_name, inpatient_no, dept_code, dept_name, "
            + "doctor_id, doctor_name, charge_item_id, item_name, spec_model, batch_no, expire_date, "
            + "use_date, charge_date, quantity, unit_price, total_amount, charge_operator, remark "
            + "FROM v_inpatient_consumable_charge "
            + "WHERE charge_date >= DATEADD(day, -3, GETDATE())";

    public static final String SQLSERVER_OUTPATIENT_RECENT_3D =
        "SELECT outpatient_charge_id, outpatient_charge_id_tf, patient_id, patient_name, outpatient_no, clinic_code, clinic_name, "
            + "doctor_id, doctor_name, charge_item_id, item_name, spec_model, batch_no, expire_date, "
            + "charge_date, quantity, unit_price, total_amount, charge_operator, payment_type, receipt_no, remark "
            + "FROM v_outpatient_consumable_charge "
            + "WHERE charge_date >= DATEADD(day, -3, GETDATE())";
}
