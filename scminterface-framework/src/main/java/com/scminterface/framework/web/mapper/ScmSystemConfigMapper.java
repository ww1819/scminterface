package com.scminterface.framework.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SCM系统参数配置Mapper接口
 * 
 * @author scminterface
 */
@Mapper
public interface ScmSystemConfigMapper
{
    /**
     * 查询所有配置
     * 
     * @return 配置列表
     */
    List<Map<String, Object>> selectAll();

    /**
     * 根据配置键查询配置值
     * 
     * @param configKey 配置键
     * @return 配置值
     */
    String selectValueByKey(@Param("configKey") String configKey);

    /**
     * 插入或更新配置
     * 
     * @param config 配置信息
     * @return 结果
     */
    int insertOrUpdate(Map<String, Object> config);

    /**
     * 删除配置
     * 
     * @param configKey 配置键
     * @return 结果
     */
    int deleteByKey(@Param("configKey") String configKey);
}
