package com.scminterface.framework.web.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.domain.zs.ScmDeliveryListItemRow;
import com.scminterface.framework.web.service.SpdDeliveryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第一方配送单接收接口（被 /api/spd/delivery 转发调用）
 */
@Api(tags = "SCM接收SPD配送单接口")
@RestController
@RequestMapping("/api/scm/spd/delivery")
public class ScmSpdDeliveryController
{
    @Autowired
    private SpdDeliveryService spdDeliveryService;

    @ApiOperation("接收查询配送单（支持输入码/配送单号）")
    @GetMapping("/query")
    public AjaxResult query(@RequestParam("keyword") String keyword)
    {
        if (StringUtils.isEmpty(keyword))
        {
            return AjaxResult.error("查询关键字不能为空");
        }
        List<ScmDeliveryListItemRow> list = spdDeliveryService.listByKeyword(keyword);
        return AjaxResult.success(list);
    }

    @ApiOperation("接收下载配送单XML")
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("deliveryNo") String deliveryNo)
    {
        if (StringUtils.isEmpty(deliveryNo))
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("配送单号不能为空".getBytes(StandardCharsets.UTF_8));
        }
        String xml = spdDeliveryService.buildDeliveryXml(deliveryNo);
        byte[] body = xml.getBytes(StandardCharsets.UTF_8);
        String safe = deliveryNo.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("zs-delivery-" + safe + ".xml").build());
        headers.setContentType(MediaType.parseMediaType("application/xml;charset=UTF-8"));
        return new ResponseEntity<byte[]>(body, headers, HttpStatus.OK);
    }
}
