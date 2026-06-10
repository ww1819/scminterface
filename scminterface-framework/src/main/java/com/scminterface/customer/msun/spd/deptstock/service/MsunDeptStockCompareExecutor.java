package com.scminterface.customer.msun.spd.deptstock.service;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.msun.spd.deptstock.mapper.MsunDeptStockCompareMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MsunDeptStockCompareExecutor
{
    private final MsunDeptStockCompareMapper compareMapper;

    public MsunDeptStockCompareExecutor(MsunDeptStockCompareMapper compareMapper)
    {
        this.compareMapper = compareMapper;
    }

    @DataSource(DataSourceType.SPD)
    public long countRows(Map<String, Object> query)
    {
        return compareMapper.countDeptStockCompareRows(query);
    }

    @DataSource(DataSourceType.SPD)
    public List<Map<String, Object>> listRows(Map<String, Object> query)
    {
        return compareMapper.selectDeptStockCompareRows(query);
    }
}
