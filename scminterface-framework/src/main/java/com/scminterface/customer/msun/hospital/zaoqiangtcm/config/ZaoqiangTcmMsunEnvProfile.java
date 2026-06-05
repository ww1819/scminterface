package com.scminterface.customer.msun.hospital.zaoqiangtcm.config;

/**
 * 枣强县中医院众阳 OpenAPI 环境凭证。
 * <p>联调默认使用 {@link #TEST}（与 zhongyang {@code GrayTest} 一致），
 * 上线前将 {@code scminterface.vendor.msun.hospitals.zaoqiang-tcm-001.active-env} 改为 {@code prod}。</p>
 */
public enum ZaoqiangTcmMsunEnvProfile
{
    /**
     * 灰度测试库（zhongyang GrayTest.java）。
     */
    TEST(
            "test",
            "灰度测试",
            "https://thirdpart-graytest.msunhis.com:9443",
            "app1730369704514752213",
            "0086fddcefca624fb7649216722fd3133855492b6f1f7013d444234aec29f7cbda",
            "SM2",
            "407545331508854784",
            "10001",
            null,
            null),

    /**
     * 枣强县中医院正式环境（SPD对接）。
     */
    PROD(
            "prod",
            "枣强正式-SPD对接",
            "https://zqxzyyy.msuncloud.cn",
            "app1779776749809786837",
            "23d27dd105384eea1d2d6c9ffae881a098b123edb5aa9889d66673c0054e1f31",
            "SM2",
            "11273002",
            "11273",
            "https://openapi.msuncloud.com/document/app1779776749809786837",
            null);

    private final String code;
    private final String label;
    private final String baseUrl;
    private final String appId;
    private final String appSecret;
    private final String signType;
    private final String hospitalId;
    private final String orgId;
    private final String documentUrl;
    private final String loginUser;

    ZaoqiangTcmMsunEnvProfile(
            String code,
            String label,
            String baseUrl,
            String appId,
            String appSecret,
            String signType,
            String hospitalId,
            String orgId,
            String documentUrl,
            String loginUser)
    {
        this.code = code;
        this.label = label;
        this.baseUrl = baseUrl;
        this.appId = appId;
        this.appSecret = appSecret;
        this.signType = signType;
        this.hospitalId = hospitalId;
        this.orgId = orgId;
        this.documentUrl = documentUrl;
        this.loginUser = loginUser;
    }

    public static ZaoqiangTcmMsunEnvProfile resolve(String activeEnv)
    {
        if (activeEnv != null && PROD.code.equalsIgnoreCase(activeEnv.trim()))
        {
            return PROD;
        }
        return TEST;
    }

    public String getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public String getAppId()
    {
        return appId;
    }

    public String getAppSecret()
    {
        return appSecret;
    }

    public String getSignType()
    {
        return signType;
    }

    public String getHospitalId()
    {
        return hospitalId;
    }

    public String getOrgId()
    {
        return orgId;
    }

    public String getDocumentUrl()
    {
        return documentUrl;
    }

    public String getLoginUser()
    {
        return loginUser;
    }
}
