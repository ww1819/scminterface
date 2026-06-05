package com.scminterface.customer.zaoqiangTcm.msun.spd;

/**
 * 枣强中医院 SPD 相关众阳接口路径（来源：接口文档2）。
 */
public final class ZaoqiangTcmMsunSpdApiPaths
{
    /** 2.5.44 药品、材料字典查询 GET */
    public static final String DRUG_DICT_INFOS = "/msun-middle-base-resource/v1/drug-dict-infos";

    /** 2.5.58 SPD 药品材料分类字典查询 GET */
    public static final String DICT_CATEGORY = "/msun-middle-base-dict/v1/dict-category";

    /** 2.5.62 SPD 供应商查询 GET */
    public static final String DRUG_SUPPLIERES = "/msun-middle-base-dict/v1/drug-supplieres";

    /** 2.5.63 SPD 生产厂商查询 GET */
    public static final String DRUG_PRODUCERES = "/msun-middle-base-dict/v1/drug-produceres";

    /** 2.5.43 药房批次库存查询 GET */
    public static final String DRUG_BATCH_STOCKS = "/msun-middle-base-resource/v1/drug-batch-stocks";

    /** 2.5.102 一级库入库和退库记录查询 POST */
    public static final String QUERY_YK_INSTOCK = "/msun-middle-base-resource/v1/query-yk-instock";

    /** 2.5.41 药品材料入库 POST（推送，本阶段未实现） */
    public static final String DRUG_STOCKS_NEW = "/msun-middle-base-resource/v1/drug-stocks-new";

    /** 2.5.42 药品材料退库 POST（推送，本阶段未实现） */
    public static final String DRUG_STOCKS_NEW_RETURN = "/msun-middle-base-resource/v1/drug-stocks-new/d";

    private ZaoqiangTcmMsunSpdApiPaths()
    {
    }
}
