package com.scminterface.customer.msun.hospital.zaoqiangtcm.config;

import com.scminterface.customer.msun.MsunApiPaths;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 枣强县中医院连接众阳 HIS 的运行时配置。
 */
@ConfigurationProperties(prefix = ZaoqiangTcmHospitalConstants.CONFIG_PREFIX)
public class ZaoqiangTcmMsunProperties implements MsunHospitalRuntime
{
    private boolean enabled;

    private String activeEnv = ZaoqiangTcmMsunEnvProfile.PROD.getCode();

    private String deptsPath = MsunApiPaths.DEPTS;

    private String identitiesPath = MsunApiPaths.IDENTITIES;

    @Override
    public String getHospitalKey()
    {
        return ZaoqiangTcmHospitalConstants.HOSPITAL_KEY;
    }

    @Override
    public String getHospitalName()
    {
        return ZaoqiangTcmHospitalConstants.HOSPITAL_NAME;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
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

    @Override
    public String getActiveEnvLabel()
    {
        return activeProfile().getLabel();
    }

    @Override
    public String getBaseUrl()
    {
        return activeProfile().getBaseUrl();
    }

    @Override
    public String getAppId()
    {
        return activeProfile().getAppId();
    }

    @Override
    public String getAppSecret()
    {
        return activeProfile().getAppSecret();
    }

    @Override
    public String getSignType()
    {
        return activeProfile().getSignType();
    }

    @Override
    public String getHospitalId()
    {
        return activeProfile().getHospitalId();
    }

    @Override
    public String getOrgId()
    {
        return activeProfile().getOrgId();
    }

    @Override
    public String getLoginUser()
    {
        return activeProfile().getLoginUser();
    }

    @Override
    public String getDocumentUrl()
    {
        return activeProfile().getDocumentUrl();
    }

    @Override
    public String getDeptsPath()
    {
        return deptsPath;
    }

    public void setDeptsPath(String deptsPath)
    {
        this.deptsPath = deptsPath;
    }

    @Override
    public String getIdentitiesPath()
    {
        return identitiesPath;
    }

    public void setIdentitiesPath(String identitiesPath)
    {
        this.identitiesPath = identitiesPath;
    }

    @Override
    public String deptsUrl()
    {
        return joinUrl(getBaseUrl(), deptsPath);
    }

    @Override
    public String identitiesUrl()
    {
        return joinUrl(getBaseUrl(), identitiesPath);
    }

    @Override
    public String configPrefix()
    {
        return ZaoqiangTcmHospitalConstants.CONFIG_PREFIX;
    }

    private static String joinUrl(String base, String path)
    {
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }
}
