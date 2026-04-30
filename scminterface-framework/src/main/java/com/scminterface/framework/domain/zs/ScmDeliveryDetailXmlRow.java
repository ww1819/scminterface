package com.scminterface.framework.domain.zs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 配送明细行（第三方配送单 XML 导出用）
 */
public class ScmDeliveryDetailXmlRow
{
    private Long detailId;
    private String zsOrderDetailId;
    private String materialCode;
    private BigDecimal deliveryQuantity;
    private String batchNo;
    private String auxBarcode;
    private Date expireDate;
    private Date productionDate;
    private String mainBarcode;
    private String materialName;
    private BigDecimal price;
    private String registerNo;
    private String unit;
    private List<ScmDeliveryDetailBarcodeRow> detailBarcodes = new ArrayList<ScmDeliveryDetailBarcodeRow>();

    public Long getDetailId()
    {
        return detailId;
    }

    public void setDetailId(Long detailId)
    {
        this.detailId = detailId;
    }

    public String getZsOrderDetailId()
    {
        return zsOrderDetailId;
    }

    public void setZsOrderDetailId(String zsOrderDetailId)
    {
        this.zsOrderDetailId = zsOrderDetailId;
    }

    public String getMaterialCode()
    {
        return materialCode;
    }

    public void setMaterialCode(String materialCode)
    {
        this.materialCode = materialCode;
    }

    public BigDecimal getDeliveryQuantity()
    {
        return deliveryQuantity;
    }

    public void setDeliveryQuantity(BigDecimal deliveryQuantity)
    {
        this.deliveryQuantity = deliveryQuantity;
    }

    public String getBatchNo()
    {
        return batchNo;
    }

    public void setBatchNo(String batchNo)
    {
        this.batchNo = batchNo;
    }

    public String getAuxBarcode()
    {
        return auxBarcode;
    }

    public void setAuxBarcode(String auxBarcode)
    {
        this.auxBarcode = auxBarcode;
    }

    public Date getExpireDate()
    {
        return expireDate;
    }

    public void setExpireDate(Date expireDate)
    {
        this.expireDate = expireDate;
    }

    public Date getProductionDate()
    {
        return productionDate;
    }

    public void setProductionDate(Date productionDate)
    {
        this.productionDate = productionDate;
    }

    public String getMainBarcode()
    {
        return mainBarcode;
    }

    public void setMainBarcode(String mainBarcode)
    {
        this.mainBarcode = mainBarcode;
    }

    public String getMaterialName()
    {
        return materialName;
    }

    public void setMaterialName(String materialName)
    {
        this.materialName = materialName;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public String getRegisterNo()
    {
        return registerNo;
    }

    public void setRegisterNo(String registerNo)
    {
        this.registerNo = registerNo;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public List<ScmDeliveryDetailBarcodeRow> getDetailBarcodes()
    {
        return detailBarcodes;
    }

    public void setDetailBarcodes(List<ScmDeliveryDetailBarcodeRow> detailBarcodes)
    {
        this.detailBarcodes = detailBarcodes != null ? detailBarcodes : new ArrayList<ScmDeliveryDetailBarcodeRow>();
    }
}
