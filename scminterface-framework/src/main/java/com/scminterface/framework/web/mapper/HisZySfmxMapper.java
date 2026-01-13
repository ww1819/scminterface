package com.scminterface.framework.web.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * HIS住院收费明细Mapper接口
 * 
 * @author scminterface
 */
@Mapper
public interface HisZySfmxMapper
{
    /**
     * 批量插入或更新数据
     * 
     * @param dataList 数据列表
     * @return 结果
     */
    int batchInsertOrUpdate(List<java.util.Map<String, Object>> dataList);

    /**
     * 查询所有已存在的inpatient_charge_id列表
     * 
     * @return inpatient_charge_id列表
     */
    List<Long> selectAllInpatientChargeIds();
}
