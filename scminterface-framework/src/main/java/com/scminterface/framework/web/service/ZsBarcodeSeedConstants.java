package com.scminterface.framework.web.service;

/**
 * 与 SCM 侧 {@code ScmBarcodeSeedService} 渠道常量保持一致
 */
public final class ZsBarcodeSeedConstants
{
    public static final String CHANNEL_TENANT = "TENANT";
    public static final String CHANNEL_ZS = "ZS";

    /** 与 {@code ZsJsfsHighLow}（scm-common）语义一致：3=高值，0=低值 */
    public static final String JSFS_HIGH_VALUE = "3";
    public static final String JSFS_LOW_VALUE = "0";

    /** 中设种子暂不按仓划分，与 {@code ZsJsfsHighLow#ZS_SEED_WAREHOUSE_ID} 一致 */
    public static final String ZS_SEED_WAREHOUSE_ID = "";

    private ZsBarcodeSeedConstants()
    {
    }

    /**
     * 中设主表 JSFS → 种子高低值：3→H，0→L，其它或空→L（与 scm 侧 {@code ZsJsfsHighLow#highLowFlagFromJsfs} 一致）
     */
    public static String highLowFlagFromJsfs(String jsfs)
    {
        String t = jsfs == null ? "" : jsfs.trim();
        if (JSFS_HIGH_VALUE.equals(t))
        {
            return "H";
        }
        if (JSFS_LOW_VALUE.equals(t))
        {
            return "L";
        }
        return "L";
    }
}
