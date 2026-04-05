package com.scminterface.framework.domain.zs;

import java.math.BigDecimal;

/**
 * 中设订单明细 DSB（按 id 与配送明细 zs_order_detail_id 关联）
 */
public class ZsTpOrderDetailDsbRow
{
    private String id;
    private BigDecimal dsb;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public BigDecimal getDsb()
    {
        return dsb;
    }

    public void setDsb(BigDecimal dsb)
    {
        this.dsb = dsb;
    }
}
