package com.scminterface.common.bridge;

import java.util.Map;

/**
 * 同步调用桥请求体
 */
public class BridgeInvokeRequest
{
    /** 业务动作，见 {@link BridgeActions} */
    private String action;

    /** 契约版本，默认 1 */
    private String apiVersion = "1";

    /** 平台医院编码 */
    private String hospitalCode;

    /** SPD 租户 ID */
    private String tenantId;

    /** 幂等键 */
    private String requestId;

    /** 业务载荷（不透明透传，云端按 action 解析） */
    private Map<String, Object> payload;

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getApiVersion()
    {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion)
    {
        this.apiVersion = apiVersion;
    }

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

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public Map<String, Object> getPayload()
    {
        return payload;
    }

    public void setPayload(Map<String, Object> payload)
    {
        this.payload = payload;
    }
}
