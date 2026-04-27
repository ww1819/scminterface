package com.scminterface.common.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购订单推送数据传输对象
 *
 * 用于在 SPD 与 SCM 之间传递采购订单主表及明细数据。
 */
public class PurchaseOrderDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** SPD 订单主键ID */
    private Long orderId;

    /** 订单单号 */
    private String orderNo;

    /** 计划单号（如有） */
    private String planNo;

    /** 供应商ID（SPD端） */
    private Long supplierId;

    /** 供应商名称（SPD fd_supplier.name，用于与 SCM scm_supplier.company_name 匹配） */
    private String supplierName;

    /** 医院/客户名称（SPD sb_customer.customer_name，用于与 SCM scm_hospital 名称匹配） */
    private String hospitalName;

    /** 仓库ID（SPD端） */
    private Long warehouseId;

    /** 要货仓库名称（SPD fd_warehouse.name，写入 SCM scm_order.warehouse_name） */
    private String warehouseName;

    /** 科室ID（SPD端，可能为空） */
    private Long departmentId;

    /** 订单日期 */
    private Date orderDate;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 单据状态 */
    private String orderStatus;

    /** 备注 */
    private String remark;

    /** 明细列表 */
    private List<PurchaseOrderItem> items;

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getPlanNo()
    {
        return planNo;
    }

    public void setPlanNo(String planNo)
    {
        this.planNo = planNo;
    }

    public Long getSupplierId()
    {
        return supplierId;
    }

    public void setSupplierId(Long supplierId)
    {
        this.supplierId = supplierId;
    }

    public String getSupplierName()
    {
        return supplierName;
    }

    public void setSupplierName(String supplierName)
    {
        this.supplierName = supplierName;
    }

    public String getHospitalName()
    {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName)
    {
        this.hospitalName = hospitalName;
    }

    public Long getWarehouseId()
    {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId)
    {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName()
    {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName)
    {
        this.warehouseName = warehouseName;
    }

    public Long getDepartmentId()
    {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId)
    {
        this.departmentId = departmentId;
    }

    public Date getOrderDate()
    {
        return orderDate;
    }

    public void setOrderDate(Date orderDate)
    {
        this.orderDate = orderDate;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public String getOrderStatus()
    {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus)
    {
        this.orderStatus = orderStatus;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public List<PurchaseOrderItem> getItems()
    {
        return items;
    }

    public void setItems(List<PurchaseOrderItem> items)
    {
        this.items = items;
    }

    /**
     * 采购订单明细
     */
    public static class PurchaseOrderItem implements Serializable
    {
        private static final long serialVersionUID = 1L;

        /** SPD 明细ID */
        private Long entryId;

        /** SPD 物资ID */
        private Long materialId;

        /** 物资编码 */
        private String materialCode;

        /** 物资名称 */
        private String materialName;

        /** 规格 */
        private String specification;

        /** 单位 */
        private String unit;

        /** 数量 */
        private BigDecimal quantity;

        /** 单价 */
        private BigDecimal unitPrice;

        /** 金额 */
        private BigDecimal amount;

        /** 生产厂家名称（如有） */
        private String manufacturerName;

        /** 注册证号（如有） */
        private String registerNo;

        /** 备注 */
        private String remark;

        public Long getEntryId()
        {
            return entryId;
        }

        public void setEntryId(Long entryId)
        {
            this.entryId = entryId;
        }

        public Long getMaterialId()
        {
            return materialId;
        }

        public void setMaterialId(Long materialId)
        {
            this.materialId = materialId;
        }

        public String getMaterialCode()
        {
            return materialCode;
        }

        public void setMaterialCode(String materialCode)
        {
            this.materialCode = materialCode;
        }

        public String getMaterialName()
        {
            return materialName;
        }

        public void setMaterialName(String materialName)
        {
            this.materialName = materialName;
        }

        public String getSpecification()
        {
            return specification;
        }

        public void setSpecification(String specification)
        {
            this.specification = specification;
        }

        public String getUnit()
        {
            return unit;
        }

        public void setUnit(String unit)
        {
            this.unit = unit;
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

        public BigDecimal getAmount()
        {
            return amount;
        }

        public void setAmount(BigDecimal amount)
        {
            this.amount = amount;
        }

        public String getManufacturerName()
        {
            return manufacturerName;
        }

        public void setManufacturerName(String manufacturerName)
        {
            this.manufacturerName = manufacturerName;
        }

        public String getRegisterNo()
        {
            return registerNo;
        }

        public void setRegisterNo(String registerNo)
        {
            this.registerNo = registerNo;
        }

        public String getRemark()
        {
            return remark;
        }

        public void setRemark(String remark)
        {
            this.remark = remark;
        }
    }
}

