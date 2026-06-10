package com.scminterface.customer.msun.spd.deptstock.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

/**
 * 科室 SPD 库存与众阳 HIS 镜像库存核对。
 */
@Mapper
public interface MsunDeptStockCompareMapper
{
    long countDeptStockCompareRows(Map<String, Object> query);

    List<Map<String, Object>> selectDeptStockCompareRows(Map<String, Object> query);
}
