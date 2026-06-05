package com.scminterface.customer.zaoqiangTcm.msun.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 枣强中医院众阳 OpenAPI 运行时配置。
 * <p>连接凭证以 {@link ZaoqiangTcmMsunEnvProfile} 为准；{@code active-env} 切换 test/prod。</p>
 */
@ConfigurationProperties(prefix = "scminterface.customer.zaoqiang-tcm-001.msun")
public class ZaoqiangTcmMsunProperties
{
    /** 为 true 时注册探针 Controller/Service */
    private boolean enabled;

    /**
     * 当前使用环境：{@code test}（灰度，默认）或 {@code prod}（枣强正式）。
     */
    private String activeEnv = ZaoqiangTcmMsunEnvProfile.PROD.getCode();

    private String deptsPath = "/msun-middle-base-common/v1/depts";

    private String identitiesPath = "/msun-middle-base-common/v1/identities";

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getActiveEnv()
    {
        return activeEnv;
    }

    public void setActiveEnv(String activeEnv)
    {
        this.activeEnv = activeEnv;
    }

    public ZaoqiangTcmMsunEnvProfile activeProfile()
    {
        return ZaoqiangTcmMsunEnvProfile.resolve(activeEnv);
    }

    public String getBaseUrl()
    {
        return activeProfile().getBaseUrl();
    }

    public String getAppId()
    {
        return activeProfile().getAppId();
    }

    public String getAppSecret()
    {
        return activeProfile().getAppSecret();
    }

    public String getSignType()
    {
        return activeProfile().getSignType();
    }

    public String getHospitalId()
    {
        return activeProfile().getHospitalId();
    }

    public String getOrgId()
    {
        return activeProfile().getOrgId();
    }

    public String getLoginUser()
    {
        return activeProfile().getLoginUser();
    }

    public String getDocumentUrl()
    {
        return activeProfile().getDocumentUrl();
    }

    public String getDeptsPath()
    {
        return deptsPath;
    }

    public void setDeptsPath(String deptsPath)
    {
        this.deptsPath = deptsPath;
    }

    public String getIdentitiesPath()
    {
        return identitiesPath;
    }

    public void setIdentitiesPath(String identitiesPath)
    {
        this.identitiesPath = identitiesPath;
    }

    public String deptsUrl()
    {
        return joinUrl(getBaseUrl(), deptsPath);
    }

    public String identitiesUrl()
    {
        return joinUrl(getBaseUrl(), identitiesPath);
    }

    private static String joinUrl(String base, String path)
    {
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }
}
