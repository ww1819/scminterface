package com.scminterface.framework.web.controller;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.enums.DataSourceType;
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

    /**
     * 示例：接收第三方 POST 的 JSON 体，原样回显关键信息，便于联调。
     * 请求体可为任意 JSON 对象，例如：{"bizType":"demo","payload":{...}}
     */
    @ApiOperation("示例-接收第三方推送数据")
    @PostMapping("/receive")
    @DataSource(DataSourceType.SCM)
    public AjaxResult receiveThirdParty(@RequestBody(required = false) Map<String, Object> body)
    {
        if (body == null || body.isEmpty())
        {
            log.warn("ZS receive: 请求体为空");
            return AjaxResult.error("请求体不能为空");
        }

        log.info("ZS receive: 收到第三方数据, keys={}", body.keySet());

        Map<String, Object> echo = new HashMap<>(4);
        echo.put("received", true);
        echo.put("fieldCount", body.size());
        echo.put("sampleKeys", body.keySet());

        return AjaxResult.success("SCM已接收示例数据（未落库，仅联调用）", echo);
    }
}
