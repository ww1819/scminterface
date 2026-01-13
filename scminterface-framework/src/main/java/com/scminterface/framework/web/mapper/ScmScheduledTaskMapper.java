package com.scminterface.framework.web.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SCM定时任务Mapper接口
 * 
 * @author scminterface
 */
@Mapper
public interface ScmScheduledTaskMapper
{
    /**
     * 查询定时任务配置
     * 
     * @param taskName 任务名称
     * @return 任务配置
     */
    Map<String, Object> selectByTaskName(@Param("taskName") String taskName);

    /**
     * 更新定时任务配置
     * 
     * @param task 任务配置
     * @return 结果
     */
    int updateTask(Map<String, Object> task);

    /**
     * 增加执行次数
     * 
     * @param taskName 任务名称
     * @return 结果
     */
    int incrementExecCount(@Param("taskName") String taskName);

    /**
     * 重置执行次数
     * 
     * @param taskName 任务名称
     * @return 结果
     */
    int resetExecCount(@Param("taskName") String taskName);
}
