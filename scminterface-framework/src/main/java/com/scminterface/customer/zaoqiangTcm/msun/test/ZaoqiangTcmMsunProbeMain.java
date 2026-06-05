package com.scminterface.customer.zaoqiangTcm.msun.test;

import com.scminterface.customer.zaoqiangTcm.msun.config.ZaoqiangTcmMsunEnvProfile;

/**
 * 枣强中医院众阳接口联调入口（类似 zhongyang {@code GrayTest}）。
 * <p>默认使用灰度测试库 {@link ZaoqiangTcmMsunEnvProfile#TEST}，拉取科室与人员身份并打印回参。</p>
 * <p>运行方式：在 IDE 中直接运行 main；或命令行传参 {@code prod} 切换正式环境。</p>
 */
public class ZaoqiangTcmMsunProbeMain
{
    public static void main(String[] args) throws Exception
    {
        ZaoqiangTcmMsunEnvProfile env = resolveEnv(args);
        printEnvBanner(env);

        // 1. 科室 2.1.9
        String deptsRaw = ZaoqiangTcmMsunOpenApiRunner.fetchDepts(env, -1);
        ZaoqiangTcmMsunOpenApiRunner.printResponse("2.1.9 科室基本信息 /v1/depts", deptsRaw);

        // 2. 人员身份 2.1.12（科室成功时用首个 deptId；否则仅用 roleType 探测）
        Long firstDeptId = null;
        try
        {
            firstDeptId = ZaoqiangTcmMsunOpenApiRunner.extractFirstDeptId(deptsRaw);
            System.out.println();
            System.out.println("将使用首个科室 deptId=" + firstDeptId + " 查询人员身份");
        }
        catch (IllegalStateException ex)
        {
            System.out.println();
            System.out.println("科室未返回可用 data，人员身份改用 roleType=0 单独探测: " + ex.getMessage());
        }

        String identitiesRaw = ZaoqiangTcmMsunOpenApiRunner.fetchIdentities(
                env, "0", firstDeptId, null, null);
        ZaoqiangTcmMsunOpenApiRunner.printResponse("2.1.12 用户身份信息 /v1/identities", identitiesRaw);

        System.out.println();
        System.out.println("联调完成。切换正式: 运行配置「枣强众阳探针-正式库」或传参 prod");
        System.out.println("若 code=openapi@9984，需在众阳侧为 appId 开通 2.1.9/2.1.12 接口权限。");
    }

    private static ZaoqiangTcmMsunEnvProfile resolveEnv(String[] args)
    {
        if (args != null && args.length > 0 && ZaoqiangTcmMsunEnvProfile.PROD.getCode().equalsIgnoreCase(args[0].trim()))
        {
            return ZaoqiangTcmMsunEnvProfile.PROD;
        }
        return ZaoqiangTcmMsunEnvProfile.TEST;
    }

    private static void printEnvBanner(ZaoqiangTcmMsunEnvProfile env)
    {
        System.out.println("租户: zaoqiang-tcm-001 | 环境: " + env.getCode() + " (" + env.getLabel() + ")");
        System.out.println("baseUrl: " + env.getBaseUrl());
        System.out.println("appId: " + env.getAppId());
        System.out.println("hospitalId: " + env.getHospitalId() + " | orgId: " + env.getOrgId());
        System.out.println("signType: " + env.getSignType());
        if (env.getDocumentUrl() != null)
        {
            System.out.println("document: " + env.getDocumentUrl());
        }
        System.out.println();
    }
}
