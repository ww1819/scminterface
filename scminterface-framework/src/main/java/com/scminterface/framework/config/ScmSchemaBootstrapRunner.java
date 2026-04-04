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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 启动时按顺序执行 classpath:sql/mysql/scm/ 下脚本，为 SCM 库补全表、字段、视图、触发器、菜单等结构。
 * <p>
 * 脚本分段符与 scm-admin 约定一致：单独一行的 {@code /}。需启用 SCM 数据源且 {@code scminterface.scm.schema.bootstrap=true}。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(prefix = "scminterface.scm.schema", name = "bootstrap", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(name = "scmDataSource")
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

    private final DataSource scmDataSource;

    @Value("${scminterface.scm.schema.fail-on-error:false}")
    private boolean failOnError;

    public ScmSchemaBootstrapRunner(@Qualifier("scmDataSource") DataSource scmDataSource)
    {
        this.scmDataSource = scmDataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        log.info("开始执行 SCM 库结构补全脚本（scminterface.scm.schema.bootstrap=true）");
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
