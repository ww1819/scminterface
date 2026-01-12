package com.scminterface.framework.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.core.domain.AjaxResult;
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

        // 这里简化处理，实际应该验证用户名密码
        // TODO: 实现真实的用户验证逻辑
        if (username == null || password == null)
        {
            return AjaxResult.error("用户名和密码不能为空");
        }

        // 生成token
        String token = tokenService.createToken(username);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);

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
}

