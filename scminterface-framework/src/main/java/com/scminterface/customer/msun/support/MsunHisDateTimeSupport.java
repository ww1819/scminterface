package com.scminterface.customer.msun.support;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import org.apache.commons.lang3.time.DateUtils;

/**
 * 众阳 HIS 日期时间格式：{@code yyyy-MM-dd HH:mm:ss}（出库/退库明细生产日期、有效期等）。
 */
public final class MsunHisDateTimeSupport
{
    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    private static final String[] PARSE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy/MM/dd HH:mm:ss"
    };

    private MsunHisDateTimeSupport()
    {
    }

    /** 格式化为 HIS 要求的 {@link #PATTERN}；无法解析时返回 null。 */
    public static String format(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof LocalDateTime)
        {
            return ((LocalDateTime) value).format(FORMATTER);
        }
        if (value instanceof LocalDate)
        {
            return ((LocalDate) value).atStartOfDay().format(FORMATTER);
        }
        if (value instanceof Date)
        {
            return new SimpleDateFormat(PATTERN).format((Date) value);
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty())
        {
            return null;
        }
        if (text.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))
        {
            return text;
        }
        if (text.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"))
        {
            return text + ":00";
        }
        if (text.matches("\\d{4}-\\d{2}-\\d{2}"))
        {
            return text + " 00:00:00";
        }
        if (text.contains("T"))
        {
            try
            {
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(FORMATTER);
            }
            catch (DateTimeParseException ignored)
            {
                try
                {
                    return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay().format(FORMATTER);
                }
                catch (DateTimeParseException ignored2)
                {
                    // fall through
                }
            }
        }
        try
        {
            Date parsed = DateUtils.parseDate(text, PARSE_PATTERNS);
            return new SimpleDateFormat(PATTERN).format(parsed);
        }
        catch (ParseException ex)
        {
            return null;
        }
    }

    /** 格式化为 HIS 日期时间；值为空时返回当前时间。 */
    public static String formatOrNow(Object value)
    {
        String formatted = format(value);
        if (formatted != null)
        {
            return formatted;
        }
        return new SimpleDateFormat(PATTERN).format(new Date());
    }
}
