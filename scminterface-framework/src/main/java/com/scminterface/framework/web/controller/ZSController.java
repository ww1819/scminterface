package com.scminterface.framework.web.controller;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.enums.DataSourceType;
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

    /**
     * 接收第三方推送的订单主表 + 明细 JSON，解析后写入 SCM 库（zs_tp_order / zs_tp_order_detail）。
     * <p>
     * 根节点须含 CUSTOMER（第三方服务标识）与 master；details 可为对象映射或数组。
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
}
