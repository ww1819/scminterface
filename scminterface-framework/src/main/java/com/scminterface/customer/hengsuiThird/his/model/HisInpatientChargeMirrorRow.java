package com.scminterface.customer.hengsuiThird.his.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 写入 his_inpatient_charge_mirror 的行（字段与 SPD HisInpatientChargeMirrorMapper.insertBatch 对齐）。衡水三院专用。
 */
public class HisInpatientChargeMirrorRow
{
    private String id;
    private String tenantId;
    private String fetchBatchId;
    private String hisInpatientChargeId;
    private String hisInpatientChargeIdTf;
    private String patientId;
    private String patientName;
    private String inpatientNo;
    private String deptCode;
    private String deptName;
    private String doctorId;
    private String doctorName;
    private String chargeItemId;
    private String itemName;
    private String specModel;
    private String batchNo;
    private String expireDate;
    private Date useDate;
    private Date chargeDate;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String chargeOperator;
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

    public String getHisInpatientChargeId()
    {
        return hisInpatientChargeId;
    }

    public void setHisInpatientChargeId(String hisInpatientChargeId)
    {
        this.hisInpatientChargeId = hisInpatientChargeId;
    }

    public String getHisInpatientChargeIdTf()
    {
        return hisInpatientChargeIdTf;
    }

    public void setHisInpatientChargeIdTf(String hisInpatientChargeIdTf)
    {
        this.hisInpatientChargeIdTf = hisInpatientChargeIdTf;
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

    public Date getChargeDate()
    {
        return chargeDate;
    }

    public void setChargeDate(Date chargeDate)
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
