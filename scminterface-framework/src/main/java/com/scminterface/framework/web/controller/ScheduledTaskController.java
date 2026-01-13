package com.scminterface.framework.web.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.framework.config.ScheduledTaskConfig;
import com.scminterface.framework.web.service.ScheduledTaskService;
import com.scminterface.framework.web.task.ScmScheduledTask;
import com.scminterface.framework.web.task.SpdScheduledTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 定时任务控制器
 * 
 * @author scminterface
 */
@Api(tags = "定时任务管理")
@RestController
@RequestMapping("/api/task")
public class ScheduledTaskController
{
    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @Autowired
    private ScheduledTaskConfig scheduledTaskConfig;

    @Autowired
    private SpdScheduledTask spdScheduledTask;

    @Autowired
    private ScmScheduledTask scmScheduledTask;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 获取SPD所有定时任务列表
     * 
     * @return 结果
     */
    @ApiOperation("获取SPD所有定时任务列表")
    @GetMapping("/spd/list")
    public AjaxResult getSpdTaskList()
    {
        try
        {
            List<Map<String, Object>> tasks = scheduledTaskService.getAllSpdTasks();
            return AjaxResult.success("查询成功", tasks);
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有任务类列表
     * 
     * @return 结果
     */
    @ApiOperation("获取所有任务类列表")
    @GetMapping("/spd/classes")
    public AjaxResult getTaskClasses()
    {
        try
        {
            List<Map<String, Object>> classes = new ArrayList<>();
            
            String basePackage = "com.scminterface.framework.web.task";
            
            // 获取task包下的所有bean
            for (String beanName : applicationContext.getBeanNamesForType(Object.class))
            {
                try
                {
                    Object bean = applicationContext.getBean(beanName);
                    Class<?> clazz = bean.getClass();
                    
                    // 获取实际类（如果是CGLIB代理，获取父类）
                    Class<?> actualClass = clazz;
                    if (clazz.getName().contains("$$EnhancerBySpringCGLIB$$") || 
                        clazz.getName().contains("$$"))
                    {
                        actualClass = clazz.getSuperclass();
                    }
                    
                    // 检查是否在task包下
                    if (actualClass.getPackage() != null && 
                        actualClass.getPackage().getName().equals(basePackage))
                    {
                        // 检查是否已经添加（避免重复）
                        boolean exists = false;
                        String fullClassName = actualClass.getName();
                        for (Map<String, Object> existing : classes)
                        {
                            if (fullClassName.equals(existing.get("fullClassName")))
                            {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (!exists)
                        {
                            Map<String, Object> classInfo = new HashMap<>();
                            classInfo.put("className", actualClass.getSimpleName());
                            classInfo.put("fullClassName", fullClassName);
                            classes.add(classInfo);
                        }
                    }
                }
                catch (Exception e)
                {
                    // 忽略无法获取的bean
                }
            }
            
            return AjaxResult.success("查询成功", classes);
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定任务类的方法列表
     * 
     * @param taskClass 任务类全限定名
     * @return 结果
     */
    @ApiOperation("获取指定任务类的方法列表")
    @GetMapping("/spd/methods")
    public AjaxResult getTaskMethods(@RequestParam String taskClass)
    {
        try
        {
            List<Map<String, Object>> methods = new ArrayList<>();
            
            Class<?> clazz = Class.forName(taskClass);
            Method[] allMethods = clazz.getDeclaredMethods();
            
            for (Method method : allMethods)
            {
                // 只返回public方法，且参数为0的方法，排除Object类的方法
                if (java.lang.reflect.Modifier.isPublic(method.getModifiers()) &&
                    method.getParameterCount() == 0 &&
                    !method.getName().equals("equals") &&
                    !method.getName().equals("hashCode") &&
                    !method.getName().equals("toString") &&
                    !method.getName().equals("getClass") &&
                    !method.getName().equals("notify") &&
                    !method.getName().equals("notifyAll") &&
                    !method.getName().equals("wait"))
                {
                    Map<String, Object> methodInfo = new HashMap<>();
                    methodInfo.put("methodName", method.getName());
                    methodInfo.put("returnType", method.getReturnType().getSimpleName());
                    methods.add(methodInfo);
                }
            }
            
            return AjaxResult.success("查询成功", methods);
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取SPD定时任务配置（按类和方法）
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 结果
     */
    @ApiOperation("获取SPD定时任务配置")
    @GetMapping("/spd/config")
    public AjaxResult getSpdTaskConfig(@RequestParam(required = false) String taskClass,
                                       @RequestParam(required = false) String taskMethod)
    {
        try
        {
            Map<String, Object> config;
            if (taskClass != null && taskMethod != null)
            {
                config = scheduledTaskService.getTaskConfigByClassAndMethod(taskClass, taskMethod);
            }
            else
            {
                // 兼容旧版本
                config = scheduledTaskService.getSpdTaskConfig();
            }
            return AjaxResult.success("查询成功", config);
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取SCM定时任务配置
     * 
     * @return 结果
     */
    @ApiOperation("获取SCM定时任务配置")
    @GetMapping("/scm/config")
    public AjaxResult getScmTaskConfig()
    {
        try
        {
            Map<String, Object> config = scheduledTaskService.getScmTaskConfig();
            return AjaxResult.success("查询成功", config);
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 添加SPD定时任务
     * 
     * @param params 配置参数
     * @return 结果
     */
    @ApiOperation("添加SPD定时任务")
    @PostMapping("/spd/add")
    public AjaxResult addSpdTask(@RequestBody Map<String, Object> params)
    {
        try
        {
            String taskName = (String) params.get("taskName");
            String taskClass = (String) params.get("taskClass");
            String taskMethod = (String) params.get("taskMethod");
            String cronExpression = (String) params.get("cronExpression");
            Integer maxExecCount = params.get("maxExecCount") != null ? 
                Integer.parseInt(params.get("maxExecCount").toString()) : -1;
            String status = (String) params.get("status");

            if (taskClass == null || taskMethod == null)
            {
                return AjaxResult.error("任务类和方法不能为空");
            }

            scheduledTaskService.insertSpdTask(taskName, taskClass, taskMethod, 
                cronExpression, maxExecCount, status != null ? status : "0");
            
            // 刷新任务配置
            scheduledTaskConfig.refreshTasks();
            
            return AjaxResult.success("添加成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("添加失败: " + e.getMessage());
        }
    }

    /**
     * 更新SPD定时任务配置
     * 
     * @param params 配置参数
     * @return 结果
     */
    @ApiOperation("更新SPD定时任务配置")
    @PostMapping("/spd/update")
    public AjaxResult updateSpdTask(@RequestBody Map<String, Object> params)
    {
        try
        {
            String taskClass = (String) params.get("taskClass");
            String taskMethod = (String) params.get("taskMethod");
            String cronExpression = (String) params.get("cronExpression");
            Integer maxExecCount = params.get("maxExecCount") != null ? 
                Integer.parseInt(params.get("maxExecCount").toString()) : null;
            String status = (String) params.get("status");

            if (taskClass != null && taskMethod != null)
            {
                // 新版本：按类和方法更新
                scheduledTaskService.updateSpdTaskByClassAndMethod(taskClass, taskMethod, 
                    cronExpression, maxExecCount, status);
            }
            else
            {
                // 兼容旧版本：按任务名称更新
                scheduledTaskService.updateSpdTask(cronExpression, maxExecCount, status);
            }
            
            // 刷新任务配置
            scheduledTaskConfig.refreshTasks();
            
            return AjaxResult.success("更新成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除SPD定时任务
     * 
     * @param taskClass 任务类全限定名
     * @param taskMethod 任务方法名
     * @return 结果
     */
    @ApiOperation("删除SPD定时任务")
    @DeleteMapping("/spd/delete")
    public AjaxResult deleteSpdTask(@RequestParam String taskClass, 
                                    @RequestParam String taskMethod)
    {
        try
        {
            scheduledTaskService.deleteSpdTask(taskClass, taskMethod);
            
            // 刷新任务配置
            scheduledTaskConfig.refreshTasks();
            
            return AjaxResult.success("删除成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新SCM定时任务配置
     * 
     * @param params 配置参数
     * @return 结果
     */
    @ApiOperation("更新SCM定时任务配置")
    @PostMapping("/scm/update")
    public AjaxResult updateScmTask(@RequestBody Map<String, Object> params)
    {
        try
        {
            String cronExpression = (String) params.get("cronExpression");
            Integer maxExecCount = params.get("maxExecCount") != null ? 
                Integer.parseInt(params.get("maxExecCount").toString()) : null;
            String status = (String) params.get("status");

            scheduledTaskService.updateScmTask(cronExpression, maxExecCount, status);
            
            // 刷新任务配置
            scheduledTaskConfig.refreshTasks();
            
            return AjaxResult.success("更新成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发SPD定时任务
     * 
     * @param params 任务参数（taskClass和taskMethod）
     * @return 结果
     */
    @ApiOperation("手动触发SPD定时任务")
    @PostMapping("/spd/trigger")
    public AjaxResult triggerSpdTask(@RequestBody(required = false) Map<String, Object> params)
    {
        try
        {
            if (params != null && params.get("taskClass") != null && params.get("taskMethod") != null)
            {
                // 新版本：按类和方法触发
                String taskClass = (String) params.get("taskClass");
                String taskMethod = (String) params.get("taskMethod");
                
                Class<?> clazz = Class.forName(taskClass);
                Object taskBean = applicationContext.getBean(clazz);
                Method method = clazz.getMethod(taskMethod);
                method.invoke(taskBean);
            }
            else
            {
                // 兼容旧版本：触发默认任务
                spdScheduledTask.execute();
            }
            return AjaxResult.success("任务已触发");
        }
        catch (Exception e)
        {
            return AjaxResult.error("触发失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发SCM定时任务
     * 
     * @return 结果
     */
    @ApiOperation("手动触发SCM定时任务")
    @PostMapping("/scm/trigger")
    public AjaxResult triggerScmTask()
    {
        try
        {
            scmScheduledTask.execute();
            return AjaxResult.success("任务已触发");
        }
        catch (Exception e)
        {
            return AjaxResult.error("触发失败: " + e.getMessage());
        }
    }

    /**
     * 刷新定时任务配置
     * 
     * @return 结果
     */
    @ApiOperation("刷新定时任务配置")
    @PostMapping("/refresh")
    public AjaxResult refreshTasks()
    {
        try
        {
            scheduledTaskConfig.refreshTasks();
            return AjaxResult.success("刷新成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 重置SPD执行次数
     * 
     * @param params 任务参数（taskClass和taskMethod）
     * @return 结果
     */
    @ApiOperation("重置SPD执行次数")
    @PostMapping("/spd/reset")
    public AjaxResult resetSpdExecCount(@RequestBody(required = false) Map<String, Object> params)
    {
        try
        {
            if (params != null && params.get("taskClass") != null && params.get("taskMethod") != null)
            {
                // 新版本：按类和方法重置
                String taskClass = (String) params.get("taskClass");
                String taskMethod = (String) params.get("taskMethod");
                scheduledTaskService.resetTaskExecCount(taskClass, taskMethod);
            }
            else
            {
                // 兼容旧版本：重置默认任务
                scheduledTaskService.resetSpdExecCount();
            }
            return AjaxResult.success("重置成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("重置失败: " + e.getMessage());
        }
    }

    /**
     * 重置SCM执行次数
     * 
     * @return 结果
     */
    @ApiOperation("重置SCM执行次数")
    @PostMapping("/scm/reset")
    public AjaxResult resetScmExecCount()
    {
        try
        {
            scheduledTaskService.resetScmExecCount();
            return AjaxResult.success("重置成功");
        }
        catch (Exception e)
        {
            return AjaxResult.error("重置失败: " + e.getMessage());
        }
    }
}
