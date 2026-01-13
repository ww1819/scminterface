package com.scminterface.framework.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.framework.web.service.ScmSystemConfigService;
import com.scminterface.framework.web.service.SpdSystemConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Map;

/**
 * 系统参数配置控制器
 * 
 * @author scminterface
 */
@Api(tags = "系统参数配置")
@RestController
@RequestMapping("/api/config")
public class SystemConfigController
{
    @Autowired
    private SpdSystemConfigService spdSystemConfigService;

    @Autowired
    private ScmSystemConfigService scmSystemConfigService;

    /**
     * 获取SPD所有配置
     * 
     * @return 结果
     */
    @ApiOperation("获取SPD所有配置")
    @GetMapping("/spd/all")
    public AjaxResult getSpdAllConfigs()
    {
        try
        {
            return AjaxResult.success("查询成功", spdSystemConfigService.getAllConfigs());
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取SCM所有配置
     * 
     * @return 结果
     */
    @ApiOperation("获取SCM所有配置")
    @GetMapping("/scm/all")
    public AjaxResult getScmAllConfigs()
    {
        try
        {
            return AjaxResult.success("查询成功", scmSystemConfigService.getAllConfigs());
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 保存SPD配置
     * 
     * @param params 配置参数
     * @return 结果
     */
    @ApiOperation("保存SPD配置")
    @PostMapping("/spd/save")
    public AjaxResult saveSpdConfig(@RequestBody Map<String, String> params)
    {
        try
        {
            String configKey = params.get("configKey");
            String configValue = params.get("configValue");
            String configDesc = params.get("configDesc");

            if (configKey == null || configKey.isEmpty())
            {
                return AjaxResult.error("配置键不能为空");
            }

            spdSystemConfigService.saveConfig(configKey, configValue, configDesc);
            return AjaxResult.success("保存成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 保存SCM配置
     * 
     * @param params 配置参数
     * @return 结果
     */
    @ApiOperation("保存SCM配置")
    @PostMapping("/scm/save")
    public AjaxResult saveScmConfig(@RequestBody Map<String, String> params)
    {
        try
        {
            String configKey = params.get("configKey");
            String configValue = params.get("configValue");
            String configDesc = params.get("configDesc");

            if (configKey == null || configKey.isEmpty())
            {
                return AjaxResult.error("配置键不能为空");
            }

            scmSystemConfigService.saveConfig(configKey, configValue, configDesc);
            return AjaxResult.success("保存成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 删除SPD配置
     * 
     * @param configKey 配置键
     * @return 结果
     */
    @ApiOperation("删除SPD配置")
    @DeleteMapping("/spd/delete")
    public AjaxResult deleteSpdConfig(@RequestParam String configKey)
    {
        try
        {
            if (configKey == null || configKey.isEmpty())
            {
                return AjaxResult.error("配置键不能为空");
            }

            spdSystemConfigService.deleteConfig(configKey);
            return AjaxResult.success("删除成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除SCM配置
     * 
     * @param configKey 配置键
     * @return 结果
     */
    @ApiOperation("删除SCM配置")
    @DeleteMapping("/scm/delete")
    public AjaxResult deleteScmConfig(@RequestParam String configKey)
    {
        try
        {
            if (configKey == null || configKey.isEmpty())
            {
                return AjaxResult.error("配置键不能为空");
            }

            scmSystemConfigService.deleteConfig(configKey);
            return AjaxResult.success("删除成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }
}
