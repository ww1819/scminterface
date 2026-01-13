package com.scminterface.framework.config;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import com.scminterface.framework.web.service.ScheduledTaskService;

/**
 * 动态定时任务配置
 * 
 * @author scminterface
 */
@Configuration
public class ScheduledTaskConfig implements SchedulingConfigurer
{
    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskConfig.class);

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @Autowired
    private ApplicationContext applicationContext;

    private ScheduledTaskRegistrar taskRegistrar;
    
    // 存储所有任务的Future，key为 taskClass#taskMethod
    private Map<String, ScheduledFuture<?>> taskFutures = new ConcurrentHashMap<>();

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar)
    {
        this.taskRegistrar = taskRegistrar;
    }

    @PostConstruct
    public void initTasks()
    {
        // 延迟初始化，确保数据源已准备好
        new Thread(() -> {
            try
            {
                Thread.sleep(3000); // 等待3秒
                refreshTasks();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 刷新定时任务配置
     */
    public void refreshTasks()
    {
        try
        {
            // 取消所有现有任务
            for (Map.Entry<String, ScheduledFuture<?>> entry : taskFutures.entrySet())
            {
                ScheduledFuture<?> future = entry.getValue();
                if (future != null && !future.isCancelled())
                {
                    future.cancel(false);
                }
            }
            taskFutures.clear();

            // 从数据库读取所有SPD任务配置并注册
            try
            {
                List<Map<String, Object>> tasks = scheduledTaskService.getAllSpdTasks();
                if (tasks != null && !tasks.isEmpty())
                {
                    for (Map<String, Object> taskConfig : tasks)
                    {
                        try
                        {
                            registerTask(taskConfig);
                        }
                        catch (Exception e)
                        {
                            log.warn("注册定时任务失败: {}.{}", 
                                taskConfig.get("taskClass"), taskConfig.get("taskMethod"), e);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                log.warn("获取SPD任务配置失败: {}", e.getMessage());
            }

            // 兼容旧版本：注册SPD和SCM任务（如果存在）
            try
            {
                registerLegacyTasks();
            }
            catch (Exception e)
            {
                log.warn("注册旧版本任务失败: {}", e.getMessage());
            }
        }
        catch (Exception e)
        {
            log.error("刷新定时任务配置异常", e);
        }
    }

    /**
     * 注册单个定时任务
     * 
     * @param taskConfig 任务配置
     */
    private void registerTask(Map<String, Object> taskConfig)
    {
        try
        {
            String taskClass = (String) taskConfig.get("taskClass");
            String taskMethod = (String) taskConfig.get("taskMethod");
            String status = (String) taskConfig.get("status");

            if (taskClass == null || taskMethod == null)
            {
                log.debug("任务配置缺少类名或方法名，跳过注册");
                return;
            }

            if (!"0".equals(status))
            {
                log.debug("任务 {}.{} 已停用，跳过注册", taskClass, taskMethod);
                return;
            }

            String cronExpression = (String) taskConfig.get("cronExpression");
            if (cronExpression == null || cronExpression.isEmpty())
            {
                log.warn("任务 {}.{} Cron表达式为空，使用默认值: 0 0/5 * * * ?", taskClass, taskMethod);
                cronExpression = "0 0/5 * * * ?";
            }

            // 处理代理类名，获取实际类名
            Class<?> clazz = Class.forName(taskClass);
            Class<?> actualClass = clazz;
            if (clazz.getName().contains("$$EnhancerBySpringCGLIB$$") || 
                clazz.getName().contains("$$"))
            {
                actualClass = clazz.getSuperclass();
                log.debug("检测到代理类 {}，使用实际类 {}", clazz.getName(), actualClass.getName());
            }

            // 通过实际类获取bean（Spring会根据类型查找）
            Object taskBean = applicationContext.getBean(actualClass);
            Method method = actualClass.getMethod(taskMethod);

            // 创建触发器
            final String finalCronExpression = cronExpression;
            Trigger trigger = new Trigger()
            {
                @Override
                public java.util.Date nextExecutionTime(TriggerContext triggerContext)
                {
                    CronTrigger cronTrigger = new CronTrigger(finalCronExpression);
                    return cronTrigger.nextExecutionTime(triggerContext);
                }
            };

            // 注册任务
            final Object finalTaskBean = taskBean;
            final Method finalMethod = method;
            ScheduledFuture<?> future = taskRegistrar.getScheduler().schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        finalMethod.invoke(finalTaskBean);
                    }
                    catch (Exception e)
                    {
                        log.error("执行定时任务异常: {}.{}", taskClass, taskMethod, e);
                    }
                }
            }, trigger);

            String taskKey = taskClass + "#" + taskMethod;
            taskFutures.put(taskKey, future);

            log.info("定时任务注册成功: {}.{}, Cron表达式: {}", taskClass, taskMethod, cronExpression);
        }
        catch (ClassNotFoundException e)
        {
            log.warn("任务类不存在: {}", taskConfig.get("taskClass"));
        }
        catch (NoSuchMethodException e)
        {
            log.warn("任务方法不存在: {}.{}", taskConfig.get("taskClass"), taskConfig.get("taskMethod"));
        }
        catch (Exception e)
        {
            log.error("注册定时任务异常: {}.{}", taskConfig.get("taskClass"), taskConfig.get("taskMethod"), e);
        }
    }

    /**
     * 注册旧版本任务（兼容性支持）
     */
    private void registerLegacyTasks()
    {
        // 注册SPD定时任务（如果存在）
        try
        {
            Map<String, Object> taskConfig = scheduledTaskService.getSpdTaskConfig();
            if (taskConfig != null && taskConfig.get("taskClass") == null)
            {
                // 旧版本任务，使用固定类和方法
                taskConfig.put("taskClass", "com.scminterface.framework.web.task.SpdScheduledTask");
                taskConfig.put("taskMethod", "execute");
                registerTask(taskConfig);
            }
        }
        catch (Exception e)
        {
            log.debug("注册旧版本SPD任务失败: {}", e.getMessage());
        }

        // 注册SCM定时任务（如果存在）
        try
        {
            Map<String, Object> taskConfig = scheduledTaskService.getScmTaskConfig();
            if (taskConfig != null && taskConfig.get("taskClass") == null)
            {
                // 旧版本任务，使用固定类和方法
                taskConfig.put("taskClass", "com.scminterface.framework.web.task.ScmScheduledTask");
                taskConfig.put("taskMethod", "execute");
                registerTask(taskConfig);
            }
        }
        catch (Exception e)
        {
            log.debug("注册旧版本SCM任务失败: {}", e.getMessage());
        }
    }
}
