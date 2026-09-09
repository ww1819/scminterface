package com.scminterface.framework.bridge;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.bridge.BridgeConstants;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.util.ZsUuid7;
import com.scminterface.framework.web.mapper.ScmBridgeInboxMapper;

/**
 * 云端 → 院内收件箱（院内 pull/ack）
 */
@Service
public class BridgeInboxService
{
    @Autowired
    private ScmBridgeInboxMapper scmBridgeInboxMapper;

    @DataSource(DataSourceType.SCM)
    public List<Map<String, Object>> pullPending(String hospitalCode, String tenantId, int limit)
    {
        return scmBridgeInboxMapper.selectPending(hospitalCode, tenantId, limit);
    }

    @DataSource(DataSourceType.SCM)
    public int ack(String hospitalCode, List<String> messageIds)
    {
        if (messageIds == null || messageIds.isEmpty())
        {
            return 0;
        }
        return scmBridgeInboxMapper.ackByIds(hospitalCode, messageIds, BridgeConstants.INBOX_STATUS_ACKED);
    }

    /**
     * 业务侧投递消息到院内收件箱（云端写）
     */
    @DataSource(DataSourceType.SCM)
    public String enqueue(String hospitalCode, String tenantId, String msgType, String payloadJson, String createBy)
    {
        if (StringUtils.isEmpty(hospitalCode) || StringUtils.isEmpty(msgType))
        {
            throw new IllegalArgumentException("hospitalCode、msgType 不能为空");
        }
        String id = ZsUuid7.newString();
        scmBridgeInboxMapper.insert(id, hospitalCode, tenantId, msgType, payloadJson,
            BridgeConstants.INBOX_STATUS_PENDING, createBy != null ? createBy : "system");
        return id;
    }
}
