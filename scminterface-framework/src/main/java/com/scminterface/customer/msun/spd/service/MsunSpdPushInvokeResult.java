package com.scminterface.customer.msun.spd.service;

import com.alibaba.fastjson2.JSONObject;
import java.util.Map;

/** 众阳 SPD 推送一次 HTTP 调用的结果与排错信息。 */
public class MsunSpdPushInvokeResult
{
    private final JSONObject wrappedResponse;
    private final Map<String, Object> debug;

    public MsunSpdPushInvokeResult(JSONObject wrappedResponse, Map<String, Object> debug)
    {
        this.wrappedResponse = wrappedResponse;
        this.debug = debug;
    }

    public JSONObject getWrappedResponse()
    {
        return wrappedResponse;
    }

    public Map<String, Object> getDebug()
    {
        return debug;
    }
}
