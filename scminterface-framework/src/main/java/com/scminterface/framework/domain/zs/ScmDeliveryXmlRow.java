package com.scminterface.framework.domain.zs;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 配送单主表字段快照（第三方配送单 XML 导出用）
 */
public class ScmDeliveryXmlRow
{
    private Long deliveryId;
    private String deliveryNo;
    /** 第一方订单 scm_order.order_id */
    private Long orderId;
    /** 第一方订单号 scm_delivery.order_no */
    private String orderNo;
    private String zsOrderId;
    /** scm_delivery.zs_customer_id，XML 节点 CUSTOMER */
    private String zsCustomerId;
    private String invoiceNo;
    /** 配送单快照：订单侧仓库ID（varchar） */
    private String srcOrderWarehouseId;
    private String srcOrderWarehouseName;
    /** 配送单表头：SPD 供应商主键（scm_delivery.spd_supplier_id） */
    private String spdSupplierId;
    /** 配送单快照：订单侧供应商ID、名称 */
    private String srcOrderSupplierId;
    private String srcOrderSupplierName;
    /** 配送单快照：订单侧科室ID、名称 */
    private String srcOrderDeptId;
    private String srcOrderDeptName;
    private String warehouse;
    private Date invoiceDate;
    private BigDecimal invoiceAmount;
    private String zsJsfs;
    private Date auditTime;

    public Long getDeliveryId()
    {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId)
    {
        this.deliveryId = deliveryId;
    }

    public String getDeliveryNo()
    {
        return deliveryNo;
    }

    public void setDeliveryNo(String deliveryNo)
    {
        this.deliveryNo = deliveryNo;
    }

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

    public String getZsOrderId()
    {
        return zsOrderId;
    }

    public void setZsOrderId(String zsOrderId)
    {
        this.zsOrderId = zsOrderId;
    }

    public String getZsCustomerId()
    {
        return zsCustomerId;
    }

    public void setZsCustomerId(String zsCustomerId)
    {
        this.zsCustomerId = zsCustomerId;
    }

    public String getInvoiceNo()
    {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo)
    {
        this.invoiceNo = invoiceNo;
    }

    public String getSrcOrderWarehouseName()
    {
        return srcOrderWarehouseName;
    }

    public void setSrcOrderWarehouseName(String srcOrderWarehouseName)
    {
        this.srcOrderWarehouseName = srcOrderWarehouseName;
    }

    public String getSrcOrderWarehouseId()
    {
        return srcOrderWarehouseId;
    }

    public void setSrcOrderWarehouseId(String srcOrderWarehouseId)
    {
        this.srcOrderWarehouseId = srcOrderWarehouseId;
    }

    public String getSrcOrderSupplierId()
    {
        return srcOrderSupplierId;
    }

    public String getSpdSupplierId()
    {
        return spdSupplierId;
    }

    public void setSpdSupplierId(String spdSupplierId)
    {
        this.spdSupplierId = spdSupplierId;
    }

    public void setSrcOrderSupplierId(String srcOrderSupplierId)
    {
        this.srcOrderSupplierId = srcOrderSupplierId;
    }

    public String getSrcOrderSupplierName()
    {
        return srcOrderSupplierName;
    }

    public void setSrcOrderSupplierName(String srcOrderSupplierName)
    {
        this.srcOrderSupplierName = srcOrderSupplierName;
    }

    public String getSrcOrderDeptId()
    {
        return srcOrderDeptId;
    }

    public void setSrcOrderDeptId(String srcOrderDeptId)
    {
        this.srcOrderDeptId = srcOrderDeptId;
    }

    public String getSrcOrderDeptName()
    {
        return srcOrderDeptName;
    }

    public void setSrcOrderDeptName(String srcOrderDeptName)
    {
        this.srcOrderDeptName = srcOrderDeptName;
    }

    public String getWarehouse()
    {
        return warehouse;
    }

    public void setWarehouse(String warehouse)
    {
        this.warehouse = warehouse;
    }

    public Date getInvoiceDate()
    {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate)
    {
        this.invoiceDate = invoiceDate;
    }

    public BigDecimal getInvoiceAmount()
    {
        return invoiceAmount;
    }

    public void setInvoiceAmount(BigDecimal invoiceAmount)
    {
        this.invoiceAmount = invoiceAmount;
    }

    public String getZsJsfs()
    {
        return zsJsfs;
    }

    public void setZsJsfs(String zsJsfs)
    {
        this.zsJsfs = zsJsfs;
    }

    public Date getAuditTime()
    {
        return auditTime;
    }

    public void setAuditTime(Date auditTime)
    {
        this.auditTime = auditTime;
    }
}
