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
import com.scminterface.framework.domain.zs.ScmDeliveryXmlRow;
import com.scminterface.framework.domain.zs.ZsTpOrderDetailDsbRow;
import com.scminterface.framework.domain.zs.ZsTpOrderXmlRow;
import com.scminterface.framework.web.mapper.ScmZsDeliveryXmlMapper;
import com.scminterface.framework.web.service.support.ZsDeliveryDataXmlBuilder;

/**
 * 按配送单号生成第三方配送单 XML（供接口侧下载）。
 */
@Service
public class ZsDeliveryExportService
{
    @Autowired
    private ScmZsDeliveryXmlMapper scmZsDeliveryXmlMapper;

    @DataSource(DataSourceType.SCM)
    public String buildZsDeliveryDataXml(String deliveryNo, String hospitalCode)
    {
        if (StringUtils.isEmpty(deliveryNo))
        {
            throw new ServiceException("配送单号不能为空");
        }
        if (StringUtils.isEmpty(hospitalCode))
        {
            throw new ServiceException("平台医院编码不能为空");
        }
        String no = deliveryNo.trim();
        String hc = hospitalCode.trim();
        return buildZsDeliveryDataXmlByResolvedDeliveryNo(no, hc);
    }

    @DataSource(DataSourceType.SCM)
    public String buildZsDeliveryDataXmlByKeyword(String keyword, String hospitalCode)
    {
        if (StringUtils.isEmpty(keyword))
        {
            throw new ServiceException("查询关键字不能为空");
        }
        if (StringUtils.isEmpty(hospitalCode))
        {
            throw new ServiceException("平台医院编码不能为空");
        }
        String key = keyword.trim();
        String hc = hospitalCode.trim();
        String resolvedDeliveryNo = scmZsDeliveryXmlMapper.selectLatestDeliveryNoByKeyword(key, hc);
        if (StringUtils.isEmpty(resolvedDeliveryNo))
        {
            throw new ServiceException("未匹配到配送单（关键字: " + key + "，医院编码: " + hc + "）");
        }
        return buildZsDeliveryDataXmlByResolvedDeliveryNo(resolvedDeliveryNo, hc);
    }

    private String buildZsDeliveryDataXmlByResolvedDeliveryNo(String no, String hospitalCode)
    {
        ScmDeliveryXmlRow d = scmZsDeliveryXmlMapper.selectDeliveryByDeliveryNo(no, hospitalCode);
        if (d == null)
        {
            throw new ServiceException("配送单不存在或无权限访问：" + no);
        }
        if (StringUtils.isEmpty(d.getZsOrderId()))
        {
            throw new ServiceException("该配送单未关联第三方订单，无法按此格式导出：" + no);
        }
        ZsTpOrderXmlRow z = scmZsDeliveryXmlMapper.selectZsTpOrderById(d.getZsOrderId());
        if (z == null)
        {
            throw new ServiceException("第三方订单不存在，无法导出：" + d.getZsOrderId());
        }
        List<ScmDeliveryDetailXmlRow> details = scmZsDeliveryXmlMapper.selectDeliveryDetailsByDeliveryId(d.getDeliveryId());
        if (details == null)
        {
            details = new ArrayList<ScmDeliveryDetailXmlRow>();
        }
        attachBarcodes(details, scmZsDeliveryXmlMapper.selectBarcodesByDeliveryId(d.getDeliveryId()));

        Map<String, ZsTpOrderDetailDsbRow> zsDetailById = new HashMap<String, ZsTpOrderDetailDsbRow>();
        List<ZsTpOrderDetailDsbRow> dsbs = scmZsDeliveryXmlMapper.selectZsTpOrderDetailDsbs(d.getZsOrderId());
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
