package com.scminterface.framework.util;

import com.github.f4b6a3.uuid.UuidCreator;

/**
 * 生成 UUID v7（时间有序；标准字符串 36 位含连字符；紧凑 32 位十六进制无连字符）
 */
public final class ZsUuid7
{
    private ZsUuid7()
    {
    }

    public static String newString()
    {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }

    /**
     * 与 {@link #newString()} 同一 UUID，去掉连字符，共 32 位十六进制。
     * 用于 {@code scm_barcode_seed.id} 等 {@code varchar(32)} 主键；带连字符的 36 位会触发 Data too long。
     */
    public static String newString32()
    {
        return newString().replace("-", "");
    }
}
