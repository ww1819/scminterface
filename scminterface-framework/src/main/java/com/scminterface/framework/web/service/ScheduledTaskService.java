package com.scminterface.framework.web.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.web.mapper.ScmScheduledTaskMapper;
import com.scminterface.framework.web.mapper.SpdScheduledTaskMapper;

/**
 * 定时任务服务
 * 
 * @author scminterface
 */
@Service
public class ScheduledTaskService
{
    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private SpdScheduledTaskMapper spdScheduledTaskMapper;

    @Autowired
    private ScmScheduledTaskMapper scmScheduledTaskMapper;

    /**
     * 获取SPD定时任务配置
     * 
     * @return 任务配置
     */
    @DataSource(DataSourceType.SPD)
    public Map<String, Object> getSpdTaskConfig()
    {
        return spdScheduledTaskMapper.selectByTaskName("SPD定时任务");
    }

    /**
     * 获取SCM定时任务配置
     * 
     * @return 任务配置
     */
    @DataSource(DataSourceType.SCM)
    public Map<String, Object> getScmTaskConfig()
    {
        return scmScheduledTaskMapper.selectByTaskName("SCM定时任务");
    }

    /**
     * 更新SPD定时任务配置
     * 
     * @param cronExpression Cron表达式
     * @param maxExecCount 最大执行次数
     * @param status 状态
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int updateSpdTask(String cronExpression, Integer maxExecCount, String status)
    {
        Map<String, Object> task = new HashMap<>();
        task.put("taskName", "SPD定时任务");
        task.put("cronExpression", cronExpression);
        task.put("maxExecCount", maxExecCount);
        task.put("status", status);
        return spdScheduledTaskMapper.updateTask(task);
    }

    /**
     * 更新SCM定时任务配置
     * 
     * @param cronExpression Cron表达式
     * @param maxExecCount 最大执行次数
     * @param status 状态
     * @return 结果
     */
    @DataSource(DataSourceType.SCM)
    public int updateScmTask(String cronExpression, Integer maxExecCount, String status)
    {
        Map<String, Object> task = new HashMap<>();
        task.put("taskName", "SCM定时任务");
        task.put("cronExpression", cronExpression);
        task.put("maxExecCount", maxExecCount);
        task.put("status", status);
        return scmScheduledTaskMapper.updateTask(task);
    }

    /**
     * SPD定时任务执行后增加执行次数
     * 
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int incrementSpdExecCount()
    {
        return spdScheduledTaskMapper.incrementExecCount("SPD定时任务");
    }

    /**
     * SCM定时任务执行后增加执行次数
     * 
     * @return 结果
     */
    @DataSource(DataSourceType.SCM)
    public int incrementScmExecCount()
    {
        return scmScheduledTaskMapper.incrementExecCount("SCM定时任务");
    }

    /**
     * 重置SPD执行次数
     * 
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int resetSpdExecCount()
    {
        return spdScheduledTaskMapper.resetExecCount("SPD定时任务");
    }

    /**
     * 重置SCM执行次数
     * 
     * @return 结果
     */
    @DataSource(DataSourceType.SCM)
    public int resetScmExecCount()
    {
        return scmScheduledTaskMapper.resetExecCount("SCM定时任务");
    }

    // ========== SPD多任务管理方法 ==========

    /**
     * 获取SPD所有定时任务配置
     * 
     * @return 任务列表
     */
    @DataSource(DataSourceType.SPD)
    public List<Map<String, Object>> getAllSpdTasks()
    {
        return spdScheduledTaskMapper.selectAll();
    }

    /**
     * 根据类和方法获取SPD定时任务配置
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 任务配置
     */
    @DataSource(DataSourceType.SPD)
    public Map<String, Object> getTaskConfigByClassAndMethod(String taskClass, String taskMethod)
    {
        // 先尝试直接查询
        Map<String, Object> config = spdScheduledTaskMapper.selectByClassAndMethod(taskClass, taskMethod);
        if (config != null)
        {
            return config;
        }
        
        // 如果查询不到，可能是代理类名，尝试获取实际类名再查询
        try
        {
            Class<?> clazz = Class.forName(taskClass);
            Class<?> actualClass = clazz;
            if (clazz.getName().contains("$$EnhancerBySpringCGLIB$$") || 
                clazz.getName().contains("$$"))
            {
                actualClass = clazz.getSuperclass();
                String actualClassName = actualClass.getName();
                config = spdScheduledTaskMapper.selectByClassAndMethod(actualClassName, taskMethod);
                if (config != null)
                {
                    return config;
                }
            }
        }
        catch (Exception e)
        {
            // 忽略异常，返回null
        }
        
        return null;
    }

    /**
     * 插入SPD定时任务
     * 
     * @param taskName 任务名称
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @param cronExpression Cron表达式
     * @param maxExecCount 最大执行次数
     * @param status 状态
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int insertSpdTask(String taskName, String taskClass, String taskMethod, 
                             String cronExpression, Integer maxExecCount, String status)
    {
        Map<String, Object> task = new HashMap<>();
        task.put("taskName", taskName);
        task.put("taskClass", taskClass);
        task.put("taskMethod", taskMethod);
        task.put("cronExpression", cronExpression);
        task.put("maxExecCount", maxExecCount);
        task.put("status", status);
        return spdScheduledTaskMapper.insertTask(task);
    }

    /**
     * 更新SPD定时任务配置（按类和方法）
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @param cronExpression Cron表达式
     * @param maxExecCount 最大执行次数
     * @param status 状态
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int updateSpdTaskByClassAndMethod(String taskClass, String taskMethod,
                                             String cronExpression, Integer maxExecCount, String status)
    {
        Map<String, Object> task = new HashMap<>();
        task.put("taskClass", taskClass);
        task.put("taskMethod", taskMethod);
        task.put("cronExpression", cronExpression);
        task.put("maxExecCount", maxExecCount);
        task.put("status", status);
        return spdScheduledTaskMapper.updateTask(task);
    }

    /**
     * 删除SPD定时任务
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int deleteSpdTask(String taskClass, String taskMethod)
    {
        return spdScheduledTaskMapper.deleteTask(taskClass, taskMethod);
    }

    /**
     * SPD定时任务执行后增加执行次数（按类和方法）
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int incrementTaskExecCount(String taskClass, String taskMethod)
    {
        return spdScheduledTaskMapper.incrementExecCountByClassAndMethod(taskClass, taskMethod);
    }

    /**
     * 重置SPD执行次数（按类和方法）
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int resetTaskExecCount(String taskClass, String taskMethod)
    {
        return spdScheduledTaskMapper.resetExecCountByClassAndMethod(taskClass, taskMethod);
    }
}
