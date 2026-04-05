package com.scminterface.framework.domain.zs;

/**
 * 配送明细条码种子（scm_delivery_detail_barcode）
 */
public class ScmDeliveryDetailBarcodeRow
{
    private Long deliveryDetailId;
    private Long seedNum;
    /** 完整条码号，明细一行一条时写入 BZ */
    private String barcodeNo;

    public Long getDeliveryDetailId()
    {
        return deliveryDetailId;
    }

    public void setDeliveryDetailId(Long deliveryDetailId)
    {
        this.deliveryDetailId = deliveryDetailId;
    }

    public Long getSeedNum()
    {
        return seedNum;
    }

    public void setSeedNum(Long seedNum)
    {
        this.seedNum = seedNum;
    }

    public String getBarcodeNo()
    {
        return barcodeNo;
    }

    public void setBarcodeNo(String barcodeNo)
    {
        this.barcodeNo = barcodeNo;
    }
}
