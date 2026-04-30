package com.scminterface.framework.example;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * JDK 8 外部系统调用示例：按配送单号下载第三方配送单 XML。
 * <p>
 * 接口服务地址示例：{@code GET http://主机:端口/api/scm/zs/deliveryData/download?deliveryNo=配送单号}
 * <p>
 * 若接口需登录态，请传入与浏览器一致的 Cookie（或按你们网关改为 Token 头）。
 */
public final class ZsDeliveryXmlDownloadClientExample
{
    private static final String BASE_URL = "http://127.0.0.1:8080";
    /** 登录后的 Cookie，例如 {@code JSESSIONID=xxx}；无需鉴权时可留空 */
    private static final String SESSION_COOKIE = "";

    private ZsDeliveryXmlDownloadClientExample()
    {
    }

    public static void main(String[] args) throws Exception
    {
        String deliveryNo = "PS-27-260130-003915";
        byte[] xml = downloadXml(deliveryNo, SESSION_COOKIE);
        String out = "zs-delivery-" + deliveryNo + ".xml";
        Files.write(Paths.get(out), xml);
        System.out.println("已写入 " + out + "，字节数=" + xml.length);
    }

    /**
     * 下载 XML 正文（UTF-8）。
     *
     * @param deliveryNo 配送单号
     * @param cookie     Cookie 请求头，可为 null 或空
     */
    public static byte[] downloadXml(String deliveryNo, String cookie) throws Exception
    {
        String enc = URLEncoder.encode(deliveryNo, StandardCharsets.UTF_8.name());
        String spec = BASE_URL + "/api/scm/zs/deliveryData/download?deliveryNo=" + enc;
        HttpURLConnection conn = (HttpURLConnection) new URL(spec).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);
        if (cookie != null && !cookie.isEmpty())
        {
            conn.setRequestProperty("Cookie", cookie);
        }
        conn.setRequestProperty("Accept", "application/xml, text/xml, */*");

        int code = conn.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        if (in == null)
        {
            throw new IllegalStateException("HTTP " + code + "，无响应体");
        }
        try
        {
            byte[] body = readAll(in);
            if (code < 200 || code >= 300)
            {
                throw new IllegalStateException("HTTP " + code + "：" + new String(body, StandardCharsets.UTF_8));
            }
            return body;
        }
        finally
        {
            in.close();
            conn.disconnect();
        }
    }

    /**
     * 示例：POST 登录后从响应头解析 Cookie（路径、参数名请按你们登录接口调整）。
     */
    public static String loginAndGetCookieHeader(String loginUrl, String user, String pass) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(loginUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        String body = "username=" + URLEncoder.encode(user, "UTF-8") + "&password=" + URLEncoder.encode(pass, "UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        OutputStream out = conn.getOutputStream();
        try
        {
            out.write(bytes);
        }
        finally
        {
            out.close();
        }
        String setCookie = conn.getHeaderField("Set-Cookie");
        conn.disconnect();
        if (setCookie == null)
        {
            return "";
        }
        int semi = setCookie.indexOf(';');
        return semi > 0 ? setCookie.substring(0, semi) : setCookie;
    }

    private static byte[] readAll(InputStream in) throws Exception
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) != -1)
        {
            buf.write(b, 0, n);
        }
        return buf.toByteArray();
    }
}
