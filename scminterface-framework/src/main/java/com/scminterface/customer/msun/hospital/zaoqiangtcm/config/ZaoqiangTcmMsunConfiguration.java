package com.scminterface.customer.msun.hospital.zaoqiangtcm.config;

import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = ZaoqiangTcmHospitalConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ZaoqiangTcmMsunProperties.class)
public class ZaoqiangTcmMsunConfiguration
{
}
