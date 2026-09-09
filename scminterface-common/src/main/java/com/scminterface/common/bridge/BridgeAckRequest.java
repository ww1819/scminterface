package com.scminterface.common.bridge;

import java.util.List;

/**
 * 收件箱确认消费
 */
public class BridgeAckRequest
{
    private String hospitalCode;

    private String tenantId;

    /** 单条确认 */
    private String messageId;

    /** 批量确认 */
    private List<String> messageIds;

    public String getHospitalCode()
    {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode)
    {
        this.hospitalCode = hospitalCode;
    }

    public String getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(String tenantId)
    {
        this.tenantId = tenantId;
    }

    public String getMessageId()
    {
        return messageId;
    }

    public void setMessageId(String messageId)
    {
        this.messageId = messageId;
    }

    public List<String> getMessageIds()
    {
        return messageIds;
    }

    public void setMessageIds(List<String> messageIds)
    {
        this.messageIds = messageIds;
    }
}
