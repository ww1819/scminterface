package com.scminterface.customer.hengsuiThird.his.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 写入 his_outpatient_charge_mirror 的行（字段与 SPD HisOutpatientChargeMirrorMapper.insertBatch 对齐）。衡水三院专用。
 */
public class HisOutpatientChargeMirrorRow
{
    private String id;
    private String tenantId;
    private String fetchBatchId;
    private String hisOutpatientChargeId;
    private String hisOutpatientChargeIdTf;
    private String patientId;
    private String patientName;
    private String outpatientNo;
    private String clinicCode;
    private String clinicName;
    private String doctorId;
    private String doctorName;
    private String chargeItemId;
    private String itemName;
    private String specModel;
    private String batchNo;
    private String expireDate;
    private String chargeDate;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String chargeOperator;
    private String paymentType;
    private String receiptNo;
    private String remark;
    private String rowFingerprint;
    private String processStatus;
    private String createBy;
    private Date createTime;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(String tenantId)
    {
        this.tenantId = tenantId;
    }

    public String getFetchBatchId()
    {
        return fetchBatchId;
    }

    public void setFetchBatchId(String fetchBatchId)
    {
        this.fetchBatchId = fetchBatchId;
    }

    public String getHisOutpatientChargeId()
    {
        return hisOutpatientChargeId;
    }

    public void setHisOutpatientChargeId(String hisOutpatientChargeId)
    {
        this.hisOutpatientChargeId = hisOutpatientChargeId;
    }

    public String getHisOutpatientChargeIdTf()
    {
        return hisOutpatientChargeIdTf;
    }

    public void setHisOutpatientChargeIdTf(String hisOutpatientChargeIdTf)
    {
        this.hisOutpatientChargeIdTf = hisOutpatientChargeIdTf;
    }

    public String getPatientId()
    {
        return patientId;
    }

    public void setPatientId(String patientId)
    {
        this.patientId = patientId;
    }

    public String getPatientName()
    {
        return patientName;
    }

    public void setPatientName(String patientName)
    {
        this.patientName = patientName;
    }

    public String getOutpatientNo()
    {
        return outpatientNo;
    }

    public void setOutpatientNo(String outpatientNo)
    {
        this.outpatientNo = outpatientNo;
    }

    public String getClinicCode()
    {
        return clinicCode;
    }

    public void setClinicCode(String clinicCode)
    {
        this.clinicCode = clinicCode;
    }

    public String getClinicName()
    {
        return clinicName;
    }

    public void setClinicName(String clinicName)
    {
        this.clinicName = clinicName;
    }

    public String getDoctorId()
    {
        return doctorId;
    }

    public void setDoctorId(String doctorId)
    {
        this.doctorId = doctorId;
    }

    public String getDoctorName()
    {
        return doctorName;
    }

    public void setDoctorName(String doctorName)
    {
        this.doctorName = doctorName;
    }

    public String getChargeItemId()
    {
        return chargeItemId;
    }

    public void setChargeItemId(String chargeItemId)
    {
        this.chargeItemId = chargeItemId;
    }

    public String getItemName()
    {
        return itemName;
    }

    public void setItemName(String itemName)
    {
        this.itemName = itemName;
    }

    public String getSpecModel()
    {
        return specModel;
    }

    public void setSpecModel(String specModel)
    {
        this.specModel = specModel;
    }

    public String getBatchNo()
    {
        return batchNo;
    }

    public void setBatchNo(String batchNo)
    {
        this.batchNo = batchNo;
    }

    public String getExpireDate()
    {
        return expireDate;
    }

    public void setExpireDate(String expireDate)
    {
        this.expireDate = expireDate;
    }

    public String getChargeDate()
    {
        return chargeDate;
    }

    public void setChargeDate(String chargeDate)
    {
        this.chargeDate = chargeDate;
    }

    public BigDecimal getQuantity()
    {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice()
    {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice)
    {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public String getChargeOperator()
    {
        return chargeOperator;
    }

    public void setChargeOperator(String chargeOperator)
    {
        this.chargeOperator = chargeOperator;
    }

    public String getPaymentType()
    {
        return paymentType;
    }

    public void setPaymentType(String paymentType)
    {
        this.paymentType = paymentType;
    }

    public String getReceiptNo()
    {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo)
    {
        this.receiptNo = receiptNo;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getRowFingerprint()
    {
        return rowFingerprint;
    }

    public void setRowFingerprint(String rowFingerprint)
    {
        this.rowFingerprint = rowFingerprint;
    }

    public String getProcessStatus()
    {
        return processStatus;
    }

    public void setProcessStatus(String processStatus)
    {
        this.processStatus = processStatus;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
