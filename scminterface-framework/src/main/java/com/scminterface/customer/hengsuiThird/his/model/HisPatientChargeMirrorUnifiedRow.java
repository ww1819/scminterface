package com.scminterface.customer.hengsuiThird.his.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 写入 his_patient_charge_mirror_unified（与 SPD HisPatientChargeMirrorUnifiedMapper.insertBatch 对齐）。衡水三院专用。
 */
public class HisPatientChargeMirrorUnifiedRow
{
    private String id;
    private String tenantId;
    private String visitKind;
    private String fetchBatchId;
    private String hisInpatientChargeId;
    private String hisOutpatientChargeId;
    private String hisInpatientChargeIdTf;
    private String hisOutpatientChargeIdTf;
    private String patientId;
    private String patientName;
    private String inpatientNo;
    private String outpatientNo;
    private String deptCode;
    private String deptName;
    private String clinicCode;
    private String clinicName;
    private String execDeptId;
    private String execDeptName;
    private String doctorId;
    private String doctorName;
    private String chargeItemId;
    private String itemName;
    private String specModel;
    private String batchNo;
    private String expireDate;
    private Date useDate;
    private String chargeDateDisplay;
    private Date chargeAt;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String chargeOperator;
    private String paymentType;
    private String receiptNo;
    private String remark;
    private String rowFingerprint;
    private String processStatus;
    private String processType;
    private Date processTime;
    private String processBy;
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

    public String getVisitKind()
    {
        return visitKind;
    }

    public void setVisitKind(String visitKind)
    {
        this.visitKind = visitKind;
    }

    public String getFetchBatchId()
    {
        return fetchBatchId;
    }

    public void setFetchBatchId(String fetchBatchId)
    {
        this.fetchBatchId = fetchBatchId;
    }

    public String getHisInpatientChargeId()
    {
        return hisInpatientChargeId;
    }

    public void setHisInpatientChargeId(String hisInpatientChargeId)
    {
        this.hisInpatientChargeId = hisInpatientChargeId;
    }

    public String getHisOutpatientChargeId()
    {
        return hisOutpatientChargeId;
    }

    public void setHisOutpatientChargeId(String hisOutpatientChargeId)
    {
        this.hisOutpatientChargeId = hisOutpatientChargeId;
    }

    public String getHisInpatientChargeIdTf()
    {
        return hisInpatientChargeIdTf;
    }

    public void setHisInpatientChargeIdTf(String hisInpatientChargeIdTf)
    {
        this.hisInpatientChargeIdTf = hisInpatientChargeIdTf;
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

    public String getInpatientNo()
    {
        return inpatientNo;
    }

    public void setInpatientNo(String inpatientNo)
    {
        this.inpatientNo = inpatientNo;
    }

    public String getOutpatientNo()
    {
        return outpatientNo;
    }

    public void setOutpatientNo(String outpatientNo)
    {
        this.outpatientNo = outpatientNo;
    }

    public String getDeptCode()
    {
        return deptCode;
    }

    public void setDeptCode(String deptCode)
    {
        this.deptCode = deptCode;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
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

    public String getExecDeptId()
    {
        return execDeptId;
    }

    public void setExecDeptId(String execDeptId)
    {
        this.execDeptId = execDeptId;
    }

    public String getExecDeptName()
    {
        return execDeptName;
    }

    public void setExecDeptName(String execDeptName)
    {
        this.execDeptName = execDeptName;
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

    public Date getUseDate()
    {
        return useDate;
    }

    public void setUseDate(Date useDate)
    {
        this.useDate = useDate;
    }

    public String getChargeDateDisplay()
    {
        return chargeDateDisplay;
    }

    public void setChargeDateDisplay(String chargeDateDisplay)
    {
        this.chargeDateDisplay = chargeDateDisplay;
    }

    public Date getChargeAt()
    {
        return chargeAt;
    }

    public void setChargeAt(Date chargeAt)
    {
        this.chargeAt = chargeAt;
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

    public String getProcessType()
    {
        return processType;
    }

    public void setProcessType(String processType)
    {
        this.processType = processType;
    }

    public Date getProcessTime()
    {
        return processTime;
    }

    public void setProcessTime(Date processTime)
    {
        this.processTime = processTime;
    }

    public String getProcessBy()
    {
        return processBy;
    }

    public void setProcessBy(String processBy)
    {
        this.processBy = processBy;
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
