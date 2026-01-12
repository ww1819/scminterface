package com.scminterface.framework.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.core.domain.MaterialArchiveDTO;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.web.service.ScmDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Map;

/**
 * SCM数据写入控制器
 * 
 * @author scminterface
 */
@Api(tags = "SCM数据写入")
@RestController
@RequestMapping("/api/scm")
public class ScmDataController
{
    private static final Logger log = LoggerFactory.getLogger(ScmDataController.class);

    @Autowired
    private ScmDataService scmDataService;

    /**
     * 保存数据到SCM数据库
     * 
     * @param data 数据
     * @return 结果
     */
    @ApiOperation("保存数据到SCM")
    @PostMapping("/save")
    @DataSource(DataSourceType.SCM)
    public AjaxResult saveScmData(@RequestBody Map<String, Object> data)
    {
        // 示例：保存数据
        // TODO: 实现具体的保存逻辑
        scmDataService.saveExampleData(data);
        return AjaxResult.success("保存成功");
    }

    /**
     * 推送档案数据到SCM
     * 接收档案数据，保存到SCM的产品证件登记功能
     * 
     * @param archiveDTO 档案数据
     * @return 结果
     */
    @ApiOperation("推送档案数据到SCM")
    @PostMapping("/pushMaterialArchive")
    @DataSource(DataSourceType.SCM)
    public AjaxResult pushMaterialArchive(@RequestBody MaterialArchiveDTO archiveDTO)
    {
        try
        {
            if (archiveDTO == null)
            {
                return AjaxResult.error("档案数据不能为空");
            }

            if (archiveDTO.getSupplierName() == null || archiveDTO.getSupplierName().isEmpty())
            {
                return AjaxResult.error("供应商名称不能为空");
            }

            log.info("开始保存档案数据，供应商: {}", archiveDTO.getSupplierName());

            // 保存档案数据
            Map<String, Object> result = scmDataService.saveMaterialArchive(archiveDTO);

            Integer successCount = (Integer) result.get("successCount");
            Integer skipCount = (Integer) result.get("skipCount");
            Integer errorCount = (Integer) result.get("errorCount");
            Integer totalCount = (Integer) result.get("totalCount");

            String message = String.format("保存完成，总计: %d, 成功: %d, 跳过: %d, 失败: %d", 
                totalCount, successCount, skipCount, errorCount);

            log.info(message);

            if (errorCount > 0)
            {
                return AjaxResult.warn(message, result);
            }
            else
            {
                return AjaxResult.success(message, result);
            }
        }
        catch (Exception e)
        {
            log.error("保存档案数据异常", e);
            return AjaxResult.error("保存档案数据失败: " + e.getMessage());
        }
    }
}

