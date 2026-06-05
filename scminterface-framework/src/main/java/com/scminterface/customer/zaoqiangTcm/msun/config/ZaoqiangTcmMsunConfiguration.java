package com.scminterface.customer.zaoqiangTcm.msun.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "scminterface.customer.zaoqiang-tcm-001.msun", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ZaoqiangTcmMsunProperties.class)
public class ZaoqiangTcmMsunConfiguration
{
}
