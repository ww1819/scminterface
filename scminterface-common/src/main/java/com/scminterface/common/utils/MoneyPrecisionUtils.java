package com.scminterface.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额/单价精度：默认至少保留 3 位小数；超过 3 位按实际位数保留，最多 6 位。
 */
public final class MoneyPrecisionUtils
{
    public static final int MIN_SCALE = 3;
    public static final int MAX_SCALE = 6;

    private MoneyPrecisionUtils()
    {
    }

    /**
     * 规范化单价或金额，避免强制压成 2 位导致精度丢失。
     */
    public static BigDecimal preserve(BigDecimal value)
    {
        if (value == null)
        {
            return null;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        int scale = normalized.scale();
        if (scale < 0)
        {
            scale = 0;
        }
        if (scale < MIN_SCALE)
        {
            return value.setScale(MIN_SCALE, RoundingMode.HALF_UP);
        }
        if (scale > MAX_SCALE)
        {
            return value.setScale(MAX_SCALE, RoundingMode.HALF_UP);
        }
        return normalized.setScale(scale, RoundingMode.UNNECESSARY);
    }
}
