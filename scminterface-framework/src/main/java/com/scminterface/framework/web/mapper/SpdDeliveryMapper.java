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
 * 第一方（SPD）内部配送单查询/下载 Mapper（与第三方接口分离）
 */
@Mapper
public interface SpdDeliveryMapper
{
    /**
     * 支持按配送单号或输入码（dsb）查询
     */
    List<ScmDeliveryListItemRow> selectDeliveriesForSpdQuery(@Param("keyword") String keyword);

    ScmDeliveryXmlRow selectDeliveryByDeliveryNo(@Param("deliveryNo") String deliveryNo);

    /**
     * 第一方采购订单头信息，映射为与 {@link ZsTpOrderXmlRow} 相同结构以便复用 XML 拼装
     */
    ZsTpOrderXmlRow selectScmOrderAsOrderXml(@Param("orderId") Long orderId);

    List<ScmDeliveryDetailXmlRow> selectDeliveryDetailsByDeliveryId(@Param("deliveryId") Long deliveryId);

    List<ScmDeliveryDetailBarcodeRow> selectBarcodesByDeliveryId(@Param("deliveryId") Long deliveryId);

    ZsTpOrderXmlRow selectZsTpOrderById(@Param("id") String id);

    List<ZsTpOrderDetailDsbRow> selectZsTpOrderDetailDsbs(@Param("orderId") String orderId);
}
