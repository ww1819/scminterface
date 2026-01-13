package com.scminterface.framework.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import com.alibaba.fastjson2.JSON;
import com.scminterface.common.constant.HttpStatus;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.common.utils.ServletUtils;
import com.scminterface.framework.web.service.TokenService;

/**
 * token过滤器 验证token有效性
 * 
 * @author scminterface
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter
{
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private TokenService tokenService;

    /** 白名单路径 */
    private static final String[] WHITELIST = {
        "/login",
        "/logout",
        "/druid",
        "/swagger-ui",
        "/swagger-resources",
        "/v2/api-docs",
        "/v3/api-docs",
        "/webjars",
        "/favicon.ico",
        "/login.html",
        "/index.html",
        "/spd-config.html",
        "/scm-config.html",
        "/js/",
        "/api/datasource/status",
        "/api/spd/pushSupplier",        // SPD推送供应商档案接口（系统间调用）
        "/api/scm/pushMaterialArchive"   // SCM接收档案接口（系统间调用）
    };

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        String requestURI = request.getRequestURI();

        // 检查是否在白名单中
        if (isWhitelist(requestURI))
        {
            chain.doFilter(request, response);
            return;
        }

        // 获取token
        String token = tokenService.getToken(request);

        // token为空
        if (StringUtils.isEmpty(token))
        {
            log.warn("请求路径: {}, token为空", requestURI);
            ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(HttpStatus.UNAUTHORIZED, "未登录或登录已过期")));
            return;
        }

        // 验证token
        if (!tokenService.validateToken(token))
        {
            log.warn("请求路径: {}, token无效或已过期", requestURI);
            ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(HttpStatus.UNAUTHORIZED, "未登录或登录已过期")));
            return;
        }

        // token有效，继续执行
        chain.doFilter(request, response);
    }

    /**
     * 判断请求路径是否在白名单中
     * 
     * @param requestURI 请求路径
     * @return 是否在白名单中
     */
    private boolean isWhitelist(String requestURI)
    {
        for (String pattern : WHITELIST)
        {
            if (pathMatcher.match(pattern + "/**", requestURI) || pathMatcher.match(pattern, requestURI))
            {
                return true;
            }
        }
        return false;
    }
}

