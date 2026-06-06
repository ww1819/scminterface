package com.scminterface.customer.msun.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 众阳 OpenAPI 调用结果（含排错用的 URL、请求头、请求体）。
 */
public class MsunSignedHttpResult
{
    private final String method;
    private final String url;
    private final Map<String, String> requestHeaders;
    private final String requestBody;
    private final String responseBody;
    private final int httpStatus;

    public MsunSignedHttpResult(
            String method,
            String url,
            Map<String, String> requestHeaders,
            String requestBody,
            String responseBody,
            int httpStatus)
    {
        this.method = method;
        this.url = url;
        this.requestHeaders = requestHeaders == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestHeaders));
        this.requestBody = requestBody;
        this.responseBody = responseBody;
        this.httpStatus = httpStatus;
    }

    public String getMethod()
    {
        return method;
    }

    public String getUrl()
    {
        return url;
    }

    public Map<String, String> getRequestHeaders()
    {
        return requestHeaders;
    }

    public String getRequestBody()
    {
        return requestBody;
    }

    public String getResponseBody()
    {
        return responseBody;
    }

    public int getHttpStatus()
    {
        return httpStatus;
    }
}
