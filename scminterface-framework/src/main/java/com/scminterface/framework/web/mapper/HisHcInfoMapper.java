package com.scminterface.framework.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * HIS耗材信息Mapper接口
 * 
 * @author scminterface
 */
@Mapper
public interface HisHcInfoMapper
{
    /**
     * 批量插入或更新数据
     * 
     * @param dataList 数据列表
     * @return 结果
     */
    int batchInsertOrUpdate(List<Map<String, Object>> dataList);

    /**
     * 根据charge_item_id查询是否存在
     * 
     * @param chargeItemId 收费项目ID
     * @return 数量
     */
    int countByChargeItemId(@Param("chargeItemId") String chargeItemId);

    /**
     * 清空表数据
     * 
     * @return 结果
     */
    int truncateTable();

    /**
     * 查询所有已存在的charge_item_id列表
     * 
     * @return charge_item_id列表
     */
    List<String> selectAllChargeItemIds();
}
