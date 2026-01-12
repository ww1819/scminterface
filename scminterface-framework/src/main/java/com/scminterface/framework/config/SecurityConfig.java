package com.scminterface.framework.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.scminterface.framework.security.JwtAuthenticationFilter;

/**
 * 安全配置
 * 
 * @author scminterface
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer
{
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        // Token认证通过Filter实现，这里不需要添加拦截器
    }
}

