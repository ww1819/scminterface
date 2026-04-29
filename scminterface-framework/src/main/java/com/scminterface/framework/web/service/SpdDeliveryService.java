package com.scminterface.framework.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.exception.ServiceException;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.domain.zs.ScmDeliveryDetailBarcodeRow;
import com.scminterface.framework.domain.zs.ScmDeliveryDetailXmlRow;
import com.scminterface.framework.domain.zs.ScmDeliveryListItemRow;
import com.scminterface.framework.domain.zs.ScmDeliveryXmlRow;
import com.scminterface.framework.domain.zs.ZsTpOrderDetailDsbRow;
import com.scminterface.framework.domain.zs.ZsTpOrderXmlRow;
import com.scminterface.framework.web.mapper.SpdDeliveryMapper;
import com.scminterface.framework.web.service.support.ZsDeliveryDataXmlBuilder;

/**
 * 第一方（SPD）内部配送单服务（与第三方接口实现分离）
 */
@Service
public class SpdDeliveryService
{
    @Autowired
    private SpdDeliveryMapper spdDeliveryMapper;

    @DataSource(DataSourceType.SCM)
    public List<ScmDeliveryListItemRow> listByKeyword(String keyword)
    {
        if (StringUtils.isEmpty(keyword))
        {
            throw new ServiceException("查询关键字不能为空");
        }
        return spdDeliveryMapper.selectDeliveriesForSpdQuery(keyword.trim());
    }

    @DataSource(DataSourceType.SCM)
    public String buildDeliveryXml(String deliveryNo)
    {
        if (StringUtils.isEmpty(deliveryNo))
        {
            throw new ServiceException("配送单号不能为空");
        }
        String no = deliveryNo.trim();
        ScmDeliveryXmlRow d = spdDeliveryMapper.selectDeliveryByDeliveryNo(no);
        if (d == null)
        {
            throw new ServiceException("配送单不存在：" + no);
        }
        if (StringUtils.isEmpty(d.getZsOrderId()))
        {
            throw new ServiceException("该配送单未关联中设订单，无法按此格式导出：" + no);
        }
        ZsTpOrderXmlRow z = spdDeliveryMapper.selectZsTpOrderById(d.getZsOrderId());
        if (z == null)
        {
            throw new ServiceException("中设订单不存在，无法导出：" + d.getZsOrderId());
        }
        List<ScmDeliveryDetailXmlRow> details = spdDeliveryMapper.selectDeliveryDetailsByDeliveryId(d.getDeliveryId());
        if (details == null)
        {
            details = new ArrayList<ScmDeliveryDetailXmlRow>();
        }
        attachBarcodes(details, spdDeliveryMapper.selectBarcodesByDeliveryId(d.getDeliveryId()));

        Map<String, ZsTpOrderDetailDsbRow> zsDetailById = new HashMap<String, ZsTpOrderDetailDsbRow>();
        List<ZsTpOrderDetailDsbRow> dsbs = spdDeliveryMapper.selectZsTpOrderDetailDsbs(d.getZsOrderId());
        if (dsbs != null)
        {
            for (ZsTpOrderDetailDsbRow row : dsbs)
            {
                zsDetailById.put(row.getId(), row);
            }
        }
        return ZsDeliveryDataXmlBuilder.build(d, details, z, zsDetailById);
    }

    private static void attachBarcodes(List<ScmDeliveryDetailXmlRow> details, List<ScmDeliveryDetailBarcodeRow> all)
    {
        if (details == null || details.isEmpty())
        {
            return;
        }
        Map<Long, List<ScmDeliveryDetailBarcodeRow>> byDetail = new HashMap<Long, List<ScmDeliveryDetailBarcodeRow>>();
        if (all != null)
        {
            for (ScmDeliveryDetailBarcodeRow b : all)
            {
                if (b.getDeliveryDetailId() == null)
                {
                    continue;
                }
                if (!byDetail.containsKey(b.getDeliveryDetailId()))
                {
                    byDetail.put(b.getDeliveryDetailId(), new ArrayList<ScmDeliveryDetailBarcodeRow>());
                }
                byDetail.get(b.getDeliveryDetailId()).add(b);
            }
        }
        for (ScmDeliveryDetailXmlRow row : details)
        {
            List<ScmDeliveryDetailBarcodeRow> list =
                byDetail.containsKey(row.getDetailId()) ? byDetail.get(row.getDetailId()) : new ArrayList<ScmDeliveryDetailBarcodeRow>();
            row.setDetailBarcodes(list);
        }
    }
}
