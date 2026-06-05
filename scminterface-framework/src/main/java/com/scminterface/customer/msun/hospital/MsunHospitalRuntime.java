package com.scminterface.customer.msun.hospital;

/**
 * 某医院客户连接众阳 HIS 的运行时上下文（连接信息 + 路径配置）。
 */
public interface MsunHospitalRuntime
{
    String getHospitalKey();

    /** SPD 租户 ID（枣强县中医院为 zaoqiang-tcm-001） */
    String getTenantId();

    String getHospitalName();

    boolean isEnabled();

    String getActiveEnv();

    String getActiveEnvLabel();

    String getBaseUrl();

    String getAppId();

    String getAppSecret();

    String getSignType();

    String getHospitalId();

    String getOrgId();

    String getLoginUser();

    String getDocumentUrl();

    String getDeptsPath();

    String getIdentitiesPath();

    String deptsUrl();

    String identitiesUrl();

    String configPrefix();
}
