package com.scminterface.customer.msun.support;

import com.alibaba.fastjson2.JSONObject;

/**
 * 解析众阳 HIS 推送/查询响应中的 {@code hisBody}。
 * <p>
 * 兼容三种外层结构：
 * <ul>
 *   <li>直连 HIS：{@code wrapRawResponse} → {@code { requestParams, hisBody }}</li>
 *   <li>前置机 AjaxResult：{@code { code, data: { data: { hisBody }, hisInvoke } }}</li>
 *   <li>仅内层包装：{@code { data: { hisBody } }}</li>
 * </ul>
 */
public final class MsunHisResponseSupport
{
    private MsunHisResponseSupport()
    {
    }

    public static JSONObject resolveHisBody(JSONObject response)
    {
        if (response == null)
        {
            return null;
        }
        JSONObject hisBody = response.getJSONObject("hisBody");
        if (hisBody != null)
        {
            return hisBody;
        }
        JSONObject data = response.getJSONObject("data");
        if (data == null)
        {
            return null;
        }
        hisBody = data.getJSONObject("hisBody");
        if (hisBody != null)
        {
            return hisBody;
        }
        JSONObject inner = data.getJSONObject("data");
        if (inner != null)
        {
            return inner.getJSONObject("hisBody");
        }
        return null;
    }

    public static void assertHisSuccess(JSONObject response)
    {
        JSONObject hisBody = resolveHisBody(response);
        if (hisBody == null)
        {
            return;
        }
        if (!Boolean.TRUE.equals(hisBody.getBoolean("success")))
        {
            String msg = hisBody.getString("message");
            throw new IllegalStateException(msg != null ? msg : "HIS推送失败");
        }
    }

    public static String extractTraceId(JSONObject response)
    {
        JSONObject hisBody = resolveHisBody(response);
        return hisBody != null ? hisBody.getString("traceId") : null;
    }
}
