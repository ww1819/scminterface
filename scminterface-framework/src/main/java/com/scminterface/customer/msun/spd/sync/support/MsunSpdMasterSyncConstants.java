package com.scminterface.customer.msun.spd.sync.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 众阳 HIS → SPD 主数据同步约束（枣强耗材分类白名单）。
 */
public final class MsunSpdMasterSyncConstants
{
    /** 允许同步至 SPD 的 HIS 耗材分类 ID（2.5.58 / 2.5.44 drug_catagory_id） */
    public static final Set<String> ALLOWED_MATERIAL_CATEGORY_HIS_IDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "18", "20", "21", "23", "1", "15", "14", "19", "16", "2", "11", "12")));

    private MsunSpdMasterSyncConstants()
    {
    }

    public static boolean isAllowedMaterialCategory(String hisCategoryId)
    {
        if (hisCategoryId == null)
        {
            return false;
        }
        return ALLOWED_MATERIAL_CATEGORY_HIS_IDS.contains(hisCategoryId.trim());
    }
}
