package com.scminterface.customer.msun.spd.sync.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 众阳镜像 → SPD 主数据同步范围与提示文案。
 */
public final class MsunSpdMasterSyncSupport
{
    private static final Set<String> MASTER_DATA_APIS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "2.1.9", "2.1.12", "2.5.44", "2.5.58", "2.5.62", "2.5.63")));

    private static final Map<String, String> API_SPD_TABLE_HINTS;

    static
    {
        Map<String, String> hints = new LinkedHashMap<>(8);
        hints.put("2.1.9", "fd_department");
        hints.put("2.1.12", "sys_user（按 user_id 聚合；admin/super_01 登录名自动加 his_ 前缀）、sys_user_department");
        hints.put("2.5.44", "fd_material（仅 materialOrDrug=1 且分类在白名单内；invalidFlag→is_use；已逻辑删除档案跳过）");
        hints.put("2.5.58", "fd_warehouse_category（仅分类白名单；不逻辑删除既有分类）");
        hints.put("2.5.62", "fd_supplier");
        hints.put("2.5.63", "fd_factory");
        API_SPD_TABLE_HINTS = Collections.unmodifiableMap(hints);
    }

    private MsunSpdMasterSyncSupport()
    {
    }

    public static boolean isMasterDataApi(String apiCode)
    {
        return apiCode != null && MASTER_DATA_APIS.contains(apiCode);
    }

    public static String spdTableHint(String apiCode)
    {
        return API_SPD_TABLE_HINTS.getOrDefault(apiCode, "");
    }

    public static String zeroSpdRowsNote(String apiCode, int mirrorRows)
    {
        if (mirrorRows <= 0)
        {
            return null;
        }
        if ("2.5.44".equals(apiCode))
        {
            return "镜像 " + mirrorRows + " 行但 SPD=0：仅同步 material_or_drug=1 且含 drug_spec_packing_id 的耗材行；外键将按字典补全供应商/厂商/库房分类/最小包装单位";
        }
        String target = spdTableHint(apiCode);
        if (target.isEmpty())
        {
            return "镜像 " + mirrorRows + " 行但 SPD upsert=0，请检查目标表列与唯一键";
        }
        return "镜像 " + mirrorRows + " 行但 SPD upsert=0，请检查 " + target + " 是否含 his_id 等列及唯一键";
    }
}
