package com.scminterface.framework.util;

import com.github.f4b6a3.uuid.UuidCreator;

/**
 * 生成 UUID v7（时间有序，字符串形式，36 位含连字符）
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
}
