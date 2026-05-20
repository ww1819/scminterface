package com.scminterface.framework.datasource;

import java.lang.reflect.Method;
import org.springframework.core.annotation.AnnotationUtils;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;

/**
 * 判断定时任务等方法声明的数据源依赖是否可用。
 */
public final class TaskDataSourceSupport
{
    private TaskDataSourceSupport()
    {
    }

    /**
     * @return 无 {@link DataSource} 注解视为不依赖特定库；有注解则须对应数据源已启用
     */
    public static boolean isDeclaredDataSourceAvailable(Class<?> taskClass, Method method,
        DataSourceAvailability availability)
    {
        if (taskClass == null || method == null || availability == null)
        {
            return true;
        }
        DataSource ann = AnnotationUtils.findAnnotation(method, DataSource.class);
        if (ann == null)
        {
            ann = AnnotationUtils.findAnnotation(taskClass, DataSource.class);
        }
        if (ann == null)
        {
            return true;
        }
        return availability.isAvailable(ann.value());
    }

    public static DataSourceType requiredType(Class<?> taskClass, Method method)
    {
        if (taskClass == null || method == null)
        {
            return null;
        }
        DataSource ann = AnnotationUtils.findAnnotation(method, DataSource.class);
        if (ann == null)
        {
            ann = AnnotationUtils.findAnnotation(taskClass, DataSource.class);
        }
        return ann != null ? ann.value() : null;
    }
}
