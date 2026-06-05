package com.scminterface.customer.msun.support;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.msun.util.OpenapiUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

/**
 * 众阳标准 OpenAPI 签名 HTTP 客户端（GET/POST），逻辑与 zhongyang {@code ToInterfaceTool} 对齐。
 */
public class MsunSignedHttpClient
{
    private static final String RSA2 = "RSA2";
    private static final String SM2 = "SM2";

    private final String appId;
    private final String appSecret;
    private final String hospitalId;
    private final String orgId;
    private final String loginUser;

    public MsunSignedHttpClient(String appId, String appSecret, String hospitalId, String orgId, String loginUser)
    {
        this.appId = appId;
        this.appSecret = appSecret;
        this.hospitalId = hospitalId;
        this.orgId = orgId;
        this.loginUser = loginUser;
    }

    public String get(String url, Map<String, Object> params) throws Exception
    {
        Map<String, Object> sorted = new TreeMap<>(Comparator.naturalOrder());
        if (CollectionUtil.isNotEmpty(params))
        {
            params.forEach(sorted::put);
        }

        String sortedParamsStr = buildQueryString(sorted);
        long timestamp = System.currentTimeMillis();
        String signatureStr = sortedParamsStr + timestamp;
        String keyType = resolveKeyType(appSecret);
        String sign = signPayload(signatureStr, keyType);

        Map<String, String> headers = buildHeaders(sign, timestamp, keyType);
        if (StringUtils.isNotBlank(sortedParamsStr))
        {
            url = url + "?" + sortedParamsStr;
        }

        HttpRequest request = HttpUtil.createGet(url);
        request.addHeaders(headers);
        HttpResponse response = request.execute();
        return response.body();
    }

    public String post(String url, Map<String, Object> body) throws Exception
    {
        String paramsJsonStr = CollectionUtil.isNotEmpty(body) ? JSON.toJSONString(body) : "";
        long timestamp = System.currentTimeMillis();
        String signatureStr = paramsJsonStr + timestamp;
        MessageDigest md = MessageDigest.getInstance("MD5");
        String md5Str = Hex.encodeHexString(md.digest(signatureStr.getBytes(StandardCharsets.UTF_8)));
        String keyType = resolveKeyType(appSecret);
        String sign = signPayload(md5Str, keyType);

        HttpRequest request = HttpUtil.createPost(url);
        request.addHeaders(buildHeaders(sign, timestamp, keyType));
        request.body(paramsJsonStr);
        HttpResponse response = request.execute();
        return response.body();
    }

    private Map<String, String> buildHeaders(String sign, long timestamp, String keyType)
    {
        Map<String, String> headers = new HashMap<>(8);
        String license = OpenapiUtil.getLicense(appId, appSecret, keyType, String.valueOf(timestamp));
        headers.put("appId", appId);
        headers.put("signType", keyType);
        headers.put("orgId", orgId);
        headers.put("hospitalId", hospitalId);
        headers.put("sign", sign);
        headers.put("timestamp", String.valueOf(timestamp));
        headers.put("license", license);
        if (StringUtils.isNotEmpty(loginUser))
        {
            headers.put("loginUser", loginUser);
        }
        return headers;
    }

    private String signPayload(String payload, String keyType) throws Exception
    {
        if (RSA2.equals(keyType))
        {
            return rsaSign(payload);
        }
        if (SM2.equals(keyType))
        {
            return MsunSm2Util.sign(payload, appSecret);
        }
        throw new IllegalStateException("不支持的 signType: " + keyType);
    }

    private String rsaSign(String preStr) throws Exception
    {
        String normalizedSecret = appSecret
                .replace("-----    BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\r", "")
                .replace("\n", "")
                .trim();
        Signature signature = Signature.getInstance("SHA256WithRSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(normalizedSecret));
        PrivateKey privateKey = java.security.KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        signature.initSign(privateKey);
        signature.update(preStr.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(signature.sign());
    }

    private static String resolveKeyType(String secret)
    {
        return secret.matches("[0-9A-Fa-f]+") ? SM2 : RSA2;
    }

    private static String buildQueryString(Map<String, Object> params)
    {
        if (CollectionUtil.isEmpty(params))
        {
            return "";
        }
        Set<String> keySet = params.keySet();
        Iterator<String> iter = keySet.iterator();
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext())
        {
            String key = iter.next();
            Object value = params.get(key);
            if (value != null)
            {
                sb.append(key).append("=").append(value).append("&");
            }
        }
        if (sb.length() == 0)
        {
            return "";
        }
        return sb.deleteCharAt(sb.length() - 1).toString();
    }
}
