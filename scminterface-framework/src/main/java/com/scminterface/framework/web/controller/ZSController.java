package com.scminterface.framework.web.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.web.service.ZsDeliveryExportService;
import com.scminterface.framework.web.service.ZsDeliveryQueryService;
import com.scminterface.framework.web.service.ZsOrderReceiveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * SCM 侧接收第三方（如招采、外系统）推送数据的示例入口。
 * <p>
 * 实际业务可在本类中扩展接口，或委托到 Service 落库。
 */
@Api(tags = "SCM第三方数据接收(ZS)")
@RestController
@RequestMapping("/api/scm/zs")
public class ZSController
{
    private static final Logger log = LoggerFactory.getLogger(ZSController.class);

    @Autowired
    private ZsOrderReceiveService zsOrderReceiveService;

    @Autowired
    private ZsDeliveryExportService zsDeliveryExportService;

    @Autowired
    private ZsDeliveryQueryService zsDeliveryQueryService;

    /**
     * 接收第三方推送的订单主表 + 明细 JSON，解析后写入 SCM 库（zs_tp_order / zs_tp_order_detail）。
     * <p>
     * 根节点须含 CUSTOMER（第三方服务标识）与 master；details 可为对象映射或数组。
     * 可选 SCMSUPCODE：与根或 master 同层传入，落库 zs_tp_order.scm_sup_code，为 SCM 平台供应商编码（与中设 SUPNO 区分），并据此匹配 scm_supplier.supplier_code 填充 scm_supplier_id。
     * 可选 NEWCUSTOMER：与根或 master 同层传入，落库 zs_tp_order.scm_hospital_code，并据此匹配 scm_hospital.hospital_code 填充 scm_hospital_id。
     * 推荐结构见接口说明与 {@code ZsOrderReceiveClientExample}。
     * <p>
     * POST http://ip:端口/api/scm/zs/receive
     */
    @ApiOperation("接收第三方推送订单并落库")
    @PostMapping("/receive")
    @DataSource(DataSourceType.SCM)
    public AjaxResult receiveThirdParty(@RequestBody(required = false) Map<String, Object> body)
    {
        log.info("ZS receive: keys={}", body == null ? null : body.keySet());
        return zsOrderReceiveService.receiveAndSave(body);
    }

    /**
     * 配送单查询：默认按配送单号模糊匹配；{@code exact=true} 时按完整单号精确查询。
     * <p>
     * GET http://ip:端口/api/scm/zs/delivery/query?deliveryNo=关键字或完整单号 与 exact=false（默认模糊）
     */
    @ApiOperation("配送单查询（模糊/精确）")
    @GetMapping("/delivery/query")
    public AjaxResult queryDeliveries(
        @RequestParam("deliveryNo") String deliveryNo,
        @RequestParam(value = "exact", required = false, defaultValue = "false") boolean exact)
    {
        return AjaxResult.success(zsDeliveryQueryService.listByDeliveryNo(deliveryNo, exact));
    }

    /**
     * 按配送单号下载中设配送单数据（XML，ROOT/LIST）。
     * <p>
     * GET http://ip:端口/api/scm/zs/deliveryData/download?deliveryNo=配送单号
     */
    @ApiOperation("按配送单号下载中设配送单XML")
    @GetMapping("/deliveryData/download")
    public ResponseEntity<byte[]> downloadZsDeliveryData(@RequestParam("deliveryNo") String deliveryNo)
    {
        String xml = zsDeliveryExportService.buildZsDeliveryDataXml(deliveryNo);
        byte[] body = xml.getBytes(StandardCharsets.UTF_8);
        String safe = StringUtils.isEmpty(deliveryNo) ? "export" : deliveryNo.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        ContentDisposition cd = ContentDisposition.attachment()
            .filename("zs-delivery-" + safe + ".xml", StandardCharsets.UTF_8)
            .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(new MediaType("application", "xml", StandardCharsets.UTF_8));
        return new ResponseEntity<byte[]>(body, headers, HttpStatus.OK);
    }
}
