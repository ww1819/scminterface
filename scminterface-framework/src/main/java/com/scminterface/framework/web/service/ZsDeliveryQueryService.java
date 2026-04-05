package com.scminterface.framework.web.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.exception.ServiceException;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.domain.zs.ScmDeliveryListItemRow;
import com.scminterface.framework.web.mapper.ScmZsDeliveryXmlMapper;

/**
 * 配送单查询（接口侧，读 SCM 库）
 */
@Service
public class ZsDeliveryQueryService
{
    @Autowired
    private ScmZsDeliveryXmlMapper scmZsDeliveryXmlMapper;

    /**
     * @param deliveryNo 配送单号关键字或完整单号，必填
     * @param exact        true：按单号精确匹配；false：模糊匹配（包含即中）
     */
    @DataSource(DataSourceType.SCM)
    public List<ScmDeliveryListItemRow> listByDeliveryNo(String deliveryNo, boolean exact)
    {
        if (StringUtils.isEmpty(deliveryNo))
        {
            throw new ServiceException("配送单号不能为空");
        }
        return scmZsDeliveryXmlMapper.selectDeliveriesForQuery(deliveryNo.trim(), exact);
    }
}
