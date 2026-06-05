package com.scminterface.customer.msun.spd.sync.support;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.utils.StringUtils;
import java.util.function.Function;

/**
 * 众阳 HIS 分页查询合并（游标翻页，供 SPD 一键同步使用）。
 */
public final class MsunHisPaginationSupport
{
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGES = 500;
    private static final long DELAY_MS = 300L;

    private MsunHisPaginationSupport()
    {
    }

    @FunctionalInterface
    public interface PageFetcher
    {
        JSONObject fetch(Long cursor) throws Exception;
    }

    public static JSONObject pullAllPages(PageFetcher fetcher, String cursorField) throws Exception
    {
        JSONArray allItems = new JSONArray();
        Long cursor = null;
        JSONObject lastPage = null;
        int pageNum = 0;

        while (pageNum < MAX_PAGES)
        {
            pageNum++;
            JSONObject page = fetcher.fetch(cursor);
            lastPage = page;
            if (!isHisOk(page))
            {
                throw new IllegalStateException("HIS 第 " + pageNum + " 页调用失败: " + hisMessage(page));
            }
            JSONArray items = extractData(page);
            if (items == null || items.isEmpty())
            {
                break;
            }
            allItems.addAll(items);
            if (items.size() < DEFAULT_PAGE_SIZE)
            {
                break;
            }
            Long next = maxCursor(items, cursorField);
            if (next == null || next.equals(cursor))
            {
                break;
            }
            cursor = next;
            if (pageNum < MAX_PAGES)
            {
                Thread.sleep(DELAY_MS);
            }
        }

        JSONObject merged = lastPage != null ? JSONObject.parseObject(lastPage.toJSONString()) : new JSONObject();
        JSONObject hisBody = merged.getJSONObject("hisBody");
        if (hisBody == null)
        {
            hisBody = new JSONObject();
            merged.put("hisBody", hisBody);
        }
        hisBody.put("data", allItems);
        hisBody.put("success", true);
        hisBody.put("code", "0000");
        hisBody.put("message", "合并完成");
        JSONObject probeMerged = new JSONObject();
        probeMerged.put("mode", "allPages");
        probeMerged.put("pages", pageNum);
        probeMerged.put("totalRows", allItems.size());
        probeMerged.put("pageSize", DEFAULT_PAGE_SIZE);
        probeMerged.put("cursorField", cursorField);
        hisBody.put("_probeMerged", probeMerged);
        return merged;
    }

    public static boolean isHisOk(JSONObject wrapped)
    {
        JSONObject hisBody = wrapped != null ? wrapped.getJSONObject("hisBody") : null;
        return hisBody != null && Boolean.TRUE.equals(hisBody.getBoolean("success"));
    }

    public static String hisMessage(JSONObject wrapped)
    {
        JSONObject hisBody = wrapped != null ? wrapped.getJSONObject("hisBody") : null;
        if (hisBody != null)
        {
            return hisBody.getString("message");
        }
        return wrapped != null ? wrapped.getString("parseError") : "无回参";
    }

    public static JSONArray extractData(JSONObject wrapped)
    {
        JSONObject hisBody = wrapped != null ? wrapped.getJSONObject("hisBody") : null;
        return hisBody != null ? hisBody.getJSONArray("data") : null;
    }

    public static Long maxCursor(JSONArray items, String field)
    {
        Long max = null;
        for (int i = 0; i < items.size(); i++)
        {
            JSONObject item = items.getJSONObject(i);
            if (item == null || !item.containsKey(field))
            {
                continue;
            }
            Long val = parseLong(item.get(field));
            if (val != null && (max == null || val > max))
            {
                max = val;
            }
        }
        return max;
    }

    private static Long parseLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (StringUtils.isEmpty(text))
        {
            return null;
        }
        try
        {
            return Long.valueOf(text);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}
