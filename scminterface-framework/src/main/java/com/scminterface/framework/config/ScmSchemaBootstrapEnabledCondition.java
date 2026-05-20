package com.scminterface.framework.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * SCM 结构脚本仅在「SCM 数据源启用」且「显式开启 bootstrap」时执行。
 */
public class ScmSchemaBootstrapEnabledCondition implements Condition
{
    private static final String SCM_ENABLED = "spring.datasource.druid.scm.enabled";
    private static final String BOOTSTRAP = "scminterface.scm.schema.bootstrap";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
    {
        Boolean scmEnabled = context.getEnvironment().getProperty(SCM_ENABLED, Boolean.class, false);
        Boolean bootstrap = context.getEnvironment().getProperty(BOOTSTRAP, Boolean.class, false);
        return Boolean.TRUE.equals(scmEnabled) && Boolean.TRUE.equals(bootstrap);
    }
}
