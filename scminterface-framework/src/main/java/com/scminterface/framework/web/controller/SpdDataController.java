package com.scminterface.framework.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.core.domain.MaterialArchiveDTO;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.utils.HttpClientUtils;
import com.scminterface.framework.web.service.SpdDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Map;

/**
 * SPD数据查询控制器
 * 
 * @author scminterface
 */
@Api(tags = "SPD数据查询")
@RestController
@RequestMapping("/api/spd")
public class SpdDataController
{
    private static final Logger log = LoggerFactory.getLogger(SpdDataController.class);

    @Autowired
    private SpdDataService spdDataService;

    @Autowired
    private HttpClientUtils httpClientUtils;

    /**
     * 查询示例数据
     * 
     * @return 结果
     */
    @ApiOperation("查询SPD数据示例")
    @GetMapping("/data")
    @DataSource(DataSourceType.SPD)
    public AjaxResult getSpdData()
    {
        // 示例：查询数据
        // TODO: 实现具体的查询逻辑
        return AjaxResult.success("查询成功", spdDataService.getExampleData());
    }

    /**
     * 推送供应商档案
     * 接收供应商ID，查询该供应商的所有档案，然后调用公网interface接口
     * 
     * @param params 请求参数，包含supplierId
     * @return 结果
     */
    @ApiOperation("推送供应商档案")
    @PostMapping("/pushSupplier")
    @DataSource(DataSourceType.SPD)
    public AjaxResult pushSupplier(@RequestBody Map<String, Object> params)
    {
        try
        {
            Long supplierId = null;
            if (params.get("supplierId") instanceof Number)
            {
                supplierId = ((Number) params.get("supplierId")).longValue();
            }
            else if (params.get("supplierId") != null)
            {
                supplierId = Long.parseLong(params.get("supplierId").toString());
            }

            if (supplierId == null)
            {
                return AjaxResult.error("供应商ID不能为空");
            }

            log.info("开始推送供应商档案，供应商ID: {}", supplierId);

            // 查询供应商档案数据
            MaterialArchiveDTO archiveDTO = spdDataService.pushMaterialArchive(supplierId);

            // 调用公网interface接口
            AjaxResult result = httpClientUtils.pushMaterialArchive(archiveDTO);

            if (result.isSuccess())
            {
                log.info("推送供应商档案成功，供应商ID: {}", supplierId);
                return AjaxResult.success("推送档案成功", result.get(AjaxResult.DATA_TAG));
            }
            else
            {
                log.error("推送供应商档案失败，供应商ID: {}, 错误: {}", supplierId, result.get(AjaxResult.MSG_TAG));
                return result;
            }
        }
        catch (Exception e)
        {
            log.error("推送供应商档案异常", e);
            return AjaxResult.error("推送档案失败: " + e.getMessage());
        }
    }
}

