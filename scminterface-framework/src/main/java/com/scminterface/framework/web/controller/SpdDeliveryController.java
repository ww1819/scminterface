package com.scminterface.framework.web.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.domain.zs.ScmDeliveryListItemRow;
import com.scminterface.framework.web.service.SpdDeliveryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * 第一方（SPD）内部使用的配送单查询与下载接口。
 * 与第三方客户调用接口分离。
 */
@Api(tags = "SPD内部配送单接口")
@RestController
@RequestMapping("/api/spd/delivery")
public class SpdDeliveryController
{
    @Value("${scminterface.public.interface.url:}")
    private String publicInterfaceUrl;

    @Value("${scminterface.spd.delivery.forward-enabled:false}")
    private boolean forwardEnabled;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SpdDeliveryService spdDeliveryService;

    @ApiOperation("SPD内部查询配送单（支持输入码/配送单号）")
    @GetMapping("/query")
    public AjaxResult query(@RequestParam("keyword") String keyword, HttpServletRequest request)
    {
        if (StringUtils.isEmpty(keyword))
        {
            return AjaxResult.error("查询关键字不能为空");
        }
        if (shouldForward(request))
        {
            AjaxResult forwarded = forwardQuery(keyword);
            if (forwarded != null)
            {
                return forwarded;
            }
        }
        List<ScmDeliveryListItemRow> list = spdDeliveryService.listByKeyword(keyword);
        return AjaxResult.success(list);
    }

    @ApiOperation("SPD内部下载配送单XML")
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("deliveryNo") String deliveryNo, HttpServletRequest request)
    {
        if (StringUtils.isEmpty(deliveryNo))
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("配送单号不能为空".getBytes(StandardCharsets.UTF_8));
        }
        if (shouldForward(request))
        {
            ResponseEntity<byte[]> forwarded = forwardDownload(deliveryNo);
            if (forwarded != null)
            {
                return forwarded;
            }
        }
        String xml = spdDeliveryService.buildDeliveryXml(deliveryNo);
        byte[] body = xml.getBytes(StandardCharsets.UTF_8);
        String safe = deliveryNo.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("zs-delivery-" + safe + ".xml").build());
        headers.setContentType(MediaType.parseMediaType("application/xml;charset=UTF-8"));
        return new ResponseEntity<byte[]>(body, headers, HttpStatus.OK);
    }

    private boolean shouldForward(HttpServletRequest request)
    {
        if (!forwardEnabled || StringUtils.isEmpty(publicInterfaceUrl))
        {
            return false;
        }
        String forwarded = request.getHeader("X-Spd-Forwarded");
        return !"1".equals(forwarded);
    }

    private AjaxResult forwardQuery(String keyword)
    {
        try
        {
            String base = publicInterfaceUrl.endsWith("/") ? publicInterfaceUrl : publicInterfaceUrl + "/";
            String url = base + "api/spd/delivery/query?keyword=" + URLEncoder.encode(keyword, "UTF-8");
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Spd-Forwarded", "1");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(headers), String.class);
            if (response.getBody() == null)
            {
                return null;
            }
            JSONObject obj = JSONObject.parseObject(response.getBody());
            Integer code = obj.getInteger("code");
            if (code != null && code == 200)
            {
                return AjaxResult.success(obj.get("data"));
            }
            return AjaxResult.error(obj.getString("msg"));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private ResponseEntity<byte[]> forwardDownload(String deliveryNo)
    {
        try
        {
            String base = publicInterfaceUrl.endsWith("/") ? publicInterfaceUrl : publicInterfaceUrl + "/";
            String url = base + "api/spd/delivery/download?deliveryNo=" + URLEncoder.encode(deliveryNo, "UTF-8");
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Spd-Forwarded", "1");
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(headers), byte[].class);
            return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
