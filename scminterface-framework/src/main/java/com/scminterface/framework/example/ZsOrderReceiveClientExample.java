package com.scminterface.framework.example;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson2.JSON;

/**
 * 第三方系统 HTTP 调用示例：向 SCM ZS 接口 POST 订单数据。
 * <p>
 * CUSTOMER 为第三方服务标识（如院区/上游系统编码），用于区分不同调用方。
 * 可选 SCMSUPCODE（根或 master）：SCM 平台供应商编码，落库 zs_tp_order.scm_sup_code 并解析 scm_supplier_id。
 * 可选 NEWCUSTOMER（根或 master）：SCM 医院编码，落库 zs_tp_order.scm_hospital_code 并解析 scm_hospital_id。
 * 本类仅作联调参考，勿在生产环境硬编码密码；生产请使用配置中心、HTTPS、鉴权等。
 */
public final class ZsOrderReceiveClientExample
{
    private ZsOrderReceiveClientExample()
    {
    }

    /**
     * 推荐入参结构（主表单行 + 明细数组，字段名与源系统一致，大写键）。
     */
    public static Map<String, Object> buildRecommendedPayload(String customerCode)
    {
        Map<String, Object> master = new LinkedHashMap<>();
        master.put("thirdPartyPk", "1");
        master.put("CUSTOMER", customerCode);
        master.put("SHEET_JE", "675445.00000000");
        master.put("DH", "DH-260128-011295");
        master.put("SUPNO", "H0157");
        master.put("SUP", "江西欧贝医疗器械有限公司");
        master.put("SUPNO2", "H0157");
        master.put("SUP2", "江西欧贝医疗器械有限公司");
        master.put("CKNO", "CK01");
        master.put("CK", "医疗器械库");
        master.put("PC", "DH-260128-011295");
        master.put("OPER", "邢娇");
        master.put("BZ", "");
        master.put("JSFS", "3");
        master.put("KSBH", "");
        master.put("KSMC", "");
        master.put("ZJLY", "");

        Map<String, Object> line1 = new LinkedHashMap<>();
        line1.put("thirdPartyPk", "1");
        line1.put("DH", "DH-260128-011295");
        line1.put("CODE", "H0157-00155");
        line1.put("NAME", "造影导管");
        line1.put("GG", "11610005#");
        line1.put("DW", "条");
        line1.put("SL", "50.0000");
        line1.put("DJ", "132.0000");
        line1.put("JE", "6600.0000");
        line1.put("BZ", "C02070100400000004460000001");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("CUSTOMER", customerCode);
        root.put("SCMSUPCODE", "示例-SCM平台供应商编码");
        root.put("master", master);
        root.put("details", new Object[] { line1 });
        return root;
    }

    /**
     * 与联调 JSON 一致：单条明细（导管鞘组），用于模拟调用 {@code POST /api/scm/zs/receive}。
     */
    public static Map<String, Object> buildSamplePayloadSingleDetailLine()
    {
        Map<String, Object> master = new LinkedHashMap<>();
        master.put("thirdPartyPk", "1");
        master.put("CUSTOMER", "K20001");
        master.put("SHEET_JE", "675445.00000000");
        master.put("DH", "DH-260128-011295");
        master.put("SUPNO", "H0157");
        master.put("SUP", "江西欧贝医疗器械有限公司");
        master.put("SUPNO2", "H0157");
        master.put("SUP2", "江西欧贝医疗器械有限公司");
        master.put("CKNO", "CK01");
        master.put("CK", "医疗器械库");
        master.put("PC", "DH-260128-011295");
        master.put("OPER", "邢娇");
        master.put("BZ", "");
        master.put("JSFS", "3");
        master.put("KSBH", "");
        master.put("KSMC", "");
        master.put("ZJLY", "");
        master.put("NEWCUSTOMER", "H21312312");
        master.put("SCMSUPCODE", "H0050");

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("DH", "DH-260128-011295");
        line.put("CODE", "H0157-00203");
        line.put("NAME", "导管鞘组");
        line.put("GG", "92200250");
        line.put("DW", "套");
        line.put("BZL", "");
        line.put("SCCJ", "湖南埃普特");
        line.put("ZCZ", "国械注准20193030894");
        line.put("PHFLAG", "");
        line.put("SL", "4.0000");
        line.put("DJ", "2400.0000");
        line.put("JE", "9600.0000");
        line.put("JM", "");
        line.put("CGJ", "0");
        line.put("BZ", "C02062701500002004460000004");
        line.put("BZ1", "");
        line.put("BZ2", "");
        line.put("DSB", "1.00");

        List<Map<String, Object>> details = new ArrayList<>(1);
        details.add(line);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("CUSTOMER", "K20001");
        body.put("NEWCUSTOMER", "H21312312");
        body.put("SCMSUPCODE", "H0050");
        body.put("master", master);
        body.put("details", details);
        return body;
    }

    /**
     * 模拟第三方服务调用 {@code ZSController#receiveThirdParty}：组装入参并 POST JSON。
     *
     * @param baseUrl 服务根地址，如 http://localhost:8088
     * @return 接口返回的 JSON 字符串（与 {@link com.scminterface.common.core.domain.AjaxResult} 序列化一致）
     */
    public static String simulateThirdPartyCallReceive(String baseUrl) throws Exception
    {
        Map<String, Object> body = buildSamplePayloadSingleDetailLine();
        return postReceive(baseUrl, body);
    }

    /**
     * 兼容旧版：主表、明细为「行号 → 行对象」映射（仍支持解析）。
     */
    public static Map<String, Object> buildLegacyMapPayload(String customerCode)
    {
        Map<String, Object> master = new LinkedHashMap<>();
        Map<String, Object> m1 = new LinkedHashMap<>();
        m1.put("CUSTOMER", customerCode);
        m1.put("SHEET_JE", "675445.00000000");
        m1.put("DH", "DH-260128-011295");
        m1.put("SUPNO", "H0157");
        m1.put("SUP", "江西欧贝医疗器械有限公司");
        master.put("1", m1);

        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, Object> d1 = new LinkedHashMap<>();
        d1.put("DH", "DH-260128-011295");
        d1.put("CODE", "H0157-00155");
        d1.put("NAME", "造影导管");
        d1.put("SL", "50.0000");
        d1.put("DJ", "132.0000");
        d1.put("JE", "6600.0000");
        details.put("1", d1);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("CUSTOMER", customerCode);
        root.put("master", master);
        root.put("details", details);
        return root;
    }

    /**
     * 使用 JDK HttpURLConnection 发送 POST（UTF-8 JSON）。
     *
     * @param baseUrl 如 http://localhost:8088
     */
    public static String postReceive(String baseUrl, Map<String, Object> payload) throws Exception
    {
        String json = JSON.toJSONString(payload);
        URL url = new URL(trimSlash(baseUrl) + "/api/scm/zs/receive");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        c.setFixedLengthStreamingMode(body.length);
        try (OutputStream os = c.getOutputStream())
        {
            os.write(body);
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        if (in == null)
        {
            return "HTTP " + code;
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b)) != -1)
        {
            buf.write(b, 0, n);
        }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String trimSlash(String baseUrl)
    {
        if (baseUrl == null || baseUrl.isEmpty())
        {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 演示：多个第三方服务标识分别推送（仅控制台输出响应摘要）。
     */
    public static void main(String[] args) throws Exception
    {
        String base = args.length > 0 ? args[0] : "http://localhost:8088";
        // 模拟用户给定 JSON 的第三方调用
        System.out.println(simulateThirdPartyCallReceive(base));
    }
}
