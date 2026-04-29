package com.scminterface.framework.web.controller;

import java.nio.charset.StandardCharsets;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.utils.HttpClientUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第一方（SPD）内部使用的配送单查询与下载接口。
 * 与第三方客户调用接口分离。
 */
@Api(tags = "SPD内部配送单接口")
@RestController
@RequestMapping("/api/spd/delivery")
public class SpdDeliveryController
{
    @Autowired
    private HttpClientUtils httpClientUtils;

    @ApiOperation("SPD内部查询配送单（支持输入码/配送单号）")
    @GetMapping("/query")
    public AjaxResult query(@RequestParam("keyword") String keyword)
    {
        if (StringUtils.isEmpty(keyword))
        {
            return AjaxResult.error("查询关键字不能为空");
        }
        return httpClientUtils.querySpdDelivery(keyword);
    }

    @ApiOperation("SPD内部下载配送单XML")
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("deliveryNo") String deliveryNo)
    {
        if (StringUtils.isEmpty(deliveryNo))
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("配送单号不能为空".getBytes(StandardCharsets.UTF_8));
        }
        return httpClientUtils.downloadSpdDeliveryXml(deliveryNo);
    }
}
