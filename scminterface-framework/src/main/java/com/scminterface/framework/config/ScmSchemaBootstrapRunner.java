package com.scminterface.framework.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.datasource.DataSourceAvailability;

/**
 * 启动时按顺序执行 classpath:sql/mysql/scm/ 下脚本，为 SCM 库补全表、字段、视图、触发器、菜单等结构。
 * <p>
 * 需同时满足：{@code spring.datasource.druid.scm.enabled=true} 且 {@code scminterface.scm.schema.bootstrap=true}。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@Conditional(ScmSchemaBootstrapEnabledCondition.class)
public class ScmSchemaBootstrapRunner implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(ScmSchemaBootstrapRunner.class);

    private static final Pattern SEGMENT_SPLIT = Pattern.compile("\\R/\\R");

    private static final String[] SCRIPT_FILES = new String[] {
        "sql/mysql/scm/table.sql",
        "sql/mysql/scm/column.sql",
        "sql/mysql/scm/view.sql",
        "sql/mysql/scm/trigger.sql",
        "sql/mysql/scm/procedure.sql",
        "sql/mysql/scm/function.sql",
        "sql/mysql/scm/menu.sql",
        "sql/mysql/scm/data_integrity.sql"
    };

    private final ObjectProvider<DataSource> scmDataSourceProvider;
    private final DataSourceAvailability dataSourceAvailability;

    @Value("${scminterface.scm.schema.fail-on-error:false}")
    private boolean failOnError;

    public ScmSchemaBootstrapRunner(
        @Qualifier("scmDataSource") ObjectProvider<DataSource> scmDataSourceProvider,
        DataSourceAvailability dataSourceAvailability)
    {
        this.scmDataSourceProvider = scmDataSourceProvider;
        this.dataSourceAvailability = dataSourceAvailability;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        if (!dataSourceAvailability.isAvailable(DataSourceType.SCM))
        {
            log.info("SCM 数据源未启用，跳过 SCM 库结构补全脚本");
            return;
        }
        DataSource scmDataSource = scmDataSourceProvider.getIfAvailable();
        if (scmDataSource == null)
        {
            log.warn("未找到 scmDataSource Bean，跳过 SCM 库结构补全脚本");
            return;
        }

        log.info("开始执行 SCM 库结构补全脚本（scm.enabled=true 且 scminterface.scm.schema.bootstrap=true）");
        for (String relativePath : SCRIPT_FILES)
        {
            ClassPathResource res = new ClassPathResource(relativePath);
            if (!res.exists())
            {
                log.warn("跳过不存在的脚本: {}", relativePath);
                continue;
            }
            String sqlText = readResource(res);
            List<String> segments = splitSegments(sqlText);
            int ok = 0;
            for (int i = 0; i < segments.size(); i++)
            {
                String sql = segments.get(i);
                if (isBlankSegment(sql))
                {
                    continue;
                }
                try (Connection conn = scmDataSource.getConnection(); Statement st = conn.createStatement())
                {
                    conn.setAutoCommit(true);
                    st.execute(sql);
                    ok++;
                }
                catch (Exception e)
                {
                    String msg = String.format("执行 [%s] 第 %d 段失败: %s", relativePath, i + 1, e.getMessage());
                    if (failOnError)
                    {
                        throw new IllegalStateException(msg, e);
                    }
                    log.warn(msg, e);
                }
            }
            log.info("脚本 [{}] 已处理，有效段数约 {} / {}", relativePath, ok, segments.size());
        }
        log.info("SCM 库结构补全脚本执行结束");
    }

    private static String readResource(ClassPathResource res) throws Exception
    {
        try (InputStream in = res.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private static List<String> splitSegments(String sqlText)
    {
        String[] parts = SEGMENT_SPLIT.split(sqlText);
        List<String> list = new ArrayList<>(parts.length);
        for (String p : parts)
        {
            list.add(p);
        }
        return list;
    }

    private static boolean isBlankSegment(String sql)
    {
        if (sql == null)
        {
            return true;
        }
        String t = sql.trim();
        return t.isEmpty();
    }
}
