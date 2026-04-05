package com.scminterface.framework.web.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.scminterface.framework.domain.zs.ScmDeliveryDetailBarcodeRow;
import com.scminterface.framework.domain.zs.ScmDeliveryDetailXmlRow;
import com.scminterface.framework.domain.zs.ScmDeliveryListItemRow;
import com.scminterface.framework.domain.zs.ScmDeliveryXmlRow;
import com.scminterface.framework.domain.zs.ZsTpOrderDetailDsbRow;
import com.scminterface.framework.domain.zs.ZsTpOrderXmlRow;

/**
 * 中设配送单 XML 导出（读 SCM 库）
 */
@Mapper
public interface ScmZsDeliveryXmlMapper
{
    ScmDeliveryXmlRow selectDeliveryByDeliveryNo(@Param("deliveryNo") String deliveryNo);

    List<ScmDeliveryDetailXmlRow> selectDeliveryDetailsByDeliveryId(@Param("deliveryId") Long deliveryId);

    List<ScmDeliveryDetailBarcodeRow> selectBarcodesByDeliveryId(@Param("deliveryId") Long deliveryId);

    ZsTpOrderXmlRow selectZsTpOrderById(@Param("id") String id);

    List<ZsTpOrderDetailDsbRow> selectZsTpOrderDetailDsbs(@Param("orderId") String orderId);

    /**
     * @param deliveryNo 非空；exact 为 true 时按单号精确匹配，否则模糊匹配
     */
    List<ScmDeliveryListItemRow> selectDeliveriesForQuery(@Param("deliveryNo") String deliveryNo,
        @Param("exact") boolean exact);
}
