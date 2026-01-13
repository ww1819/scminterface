package com.scminterface.framework.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.framework.web.service.DataSourceCheckService;
import com.scminterface.framework.web.service.TokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录控制器
 * 
 * @author scminterface
 */
@Api(tags = "登录管理")
@RestController
public class LoginController
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private DataSourceCheckService dataSourceCheckService;

    // 固定账号密码
    private static final String FIXED_USERNAME = "admin";
    private static final String FIXED_PASSWORD = "admin123";

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @ApiOperation("登录")
    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> loginBody)
    {
        String username = loginBody.get("username");
        String password = loginBody.get("password");

        if (username == null || password == null)
        {
            return AjaxResult.error("用户名和密码不能为空");
        }

        // 验证固定账号密码
        if (!FIXED_USERNAME.equals(username) || !FIXED_PASSWORD.equals(password))
        {
            return AjaxResult.error("用户名或密码错误");
        }

        // 生成token
        String token = tokenService.createToken(username);

        // 获取可用数据源列表
        java.util.List<java.util.Map<String, Object>> availableDataSources = dataSourceCheckService.checkDataSourceStatus();

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);
        data.put("availableDataSources", availableDataSources);

        return AjaxResult.success("登录成功", data);
    }

    /**
     * 退出登录
     */
    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public AjaxResult logout()
    {
        return AjaxResult.success("退出成功");
    }

    /**
     * 获取数据源连接状态
     * 
     * @return 可用数据源列表
     */
    @ApiOperation("获取数据源连接状态")
    @GetMapping("/api/datasource/status")
    public AjaxResult getDataSourceStatus()
    {
        java.util.List<java.util.Map<String, Object>> availableDataSources = dataSourceCheckService.checkDataSourceStatus();
        return AjaxResult.success("获取成功", availableDataSources);
    }
}

