package com.scminterface.framework.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SPD定时任务Mapper接口
 * 
 * @author scminterface
 */
@Mapper
public interface SpdScheduledTaskMapper
{
    /**
     * 查询定时任务配置（按任务名称，兼容旧版本）
     * 
     * @param taskName 任务名称
     * @return 任务配置
     */
    Map<String, Object> selectByTaskName(@Param("taskName") String taskName);

    /**
     * 查询定时任务配置（按类和方法）
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 任务配置
     */
    Map<String, Object> selectByClassAndMethod(@Param("taskClass") String taskClass, 
                                               @Param("taskMethod") String taskMethod);

    /**
     * 查询所有定时任务
     * 
     * @return 任务列表
     */
    List<Map<String, Object>> selectAll();

    /**
     * 插入定时任务
     * 
     * @param task 任务配置
     * @return 结果
     */
    int insertTask(Map<String, Object> task);

    /**
     * 更新定时任务配置
     * 
     * @param task 任务配置
     * @return 结果
     */
    int updateTask(Map<String, Object> task);

    /**
     * 删除定时任务
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 结果
     */
    int deleteTask(@Param("taskClass") String taskClass, @Param("taskMethod") String taskMethod);

    /**
     * 增加执行次数（按任务名称，兼容旧版本）
     * 
     * @param taskName 任务名称
     * @return 结果
     */
    int incrementExecCount(@Param("taskName") String taskName);

    /**
     * 增加执行次数（按类和方法）
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 结果
     */
    int incrementExecCountByClassAndMethod(@Param("taskClass") String taskClass, 
                                            @Param("taskMethod") String taskMethod);

    /**
     * 重置执行次数（按任务名称，兼容旧版本）
     * 
     * @param taskName 任务名称
     * @return 结果
     */
    int resetExecCount(@Param("taskName") String taskName);

    /**
     * 重置执行次数（按类和方法）
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 结果
     */
    int resetExecCountByClassAndMethod(@Param("taskClass") String taskClass, 
                                        @Param("taskMethod") String taskMethod);
}
