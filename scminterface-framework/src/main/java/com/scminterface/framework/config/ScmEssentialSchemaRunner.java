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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.datasource.DataSourceAvailability;

/**
 * SCM 必要结构轻量补全：scm.enabled 时默认执行 {@code essential.sql}（幂等 CREATE IF NOT EXISTS）。
 * <p>与全量 {@link ScmSchemaBootstrapRunner}（bootstrap=true）分离，减少院内人工跑脚本。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
@ConditionalOnProperty(prefix = "spring.datasource.druid.scm", name = "enabled", havingValue = "true")
public class ScmEssentialSchemaRunner implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(ScmEssentialSchemaRunner.class);

    private static final Pattern SEGMENT_SPLIT = Pattern.compile("\\R/\\R");

    private static final String ESSENTIAL_SCRIPT = "sql/mysql/scm/essential.sql";

    private final ObjectProvider<DataSource> scmDataSourceProvider;
    private final DataSourceAvailability dataSourceAvailability;

    @Value("${scminterface.scm.schema.essential:true}")
    private boolean essentialEnabled;

    @Value("${scminterface.scm.schema.fail-on-error:false}")
    private boolean failOnError;

    public ScmEssentialSchemaRunner(
        @Qualifier("scmDataSource") ObjectProvider<DataSource> scmDataSourceProvider,
        DataSourceAvailability dataSourceAvailability)
    {
        this.scmDataSourceProvider = scmDataSourceProvider;
        this.dataSourceAvailability = dataSourceAvailability;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        if (!essentialEnabled)
        {
            log.info("scminterface.scm.schema.essential=false，跳过必要结构补全");
            return;
        }
        if (!dataSourceAvailability.isAvailable(DataSourceType.SCM))
        {
            log.info("SCM 数据源未启用，跳过必要结构补全");
            return;
        }
        DataSource scmDataSource = scmDataSourceProvider.getIfAvailable();
        if (scmDataSource == null)
        {
            log.warn("未找到 scmDataSource Bean，跳过必要结构补全");
            return;
        }
        ClassPathResource res = new ClassPathResource(ESSENTIAL_SCRIPT);
        if (!res.exists())
        {
            log.warn("必要结构脚本不存在: {}", ESSENTIAL_SCRIPT);
            return;
        }
        log.info("开始执行 SCM 必要结构补全: {}", ESSENTIAL_SCRIPT);
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
                String msg = String.format("执行 [%s] 第 %d 段失败: %s", ESSENTIAL_SCRIPT, i + 1, e.getMessage());
                if (failOnError)
                {
                    throw new IllegalStateException(msg, e);
                }
                log.warn(msg, e);
            }
        }
        log.info("SCM 必要结构补全结束，有效段约 {} / {}", ok, segments.size());
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
        List<String> list = new ArrayList<>();
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
        if (t.isEmpty())
        {
            return true;
        }
        String[] lines = t.split("\\R");
        for (String line : lines)
        {
            String s = line.trim();
            if (!s.isEmpty() && !s.startsWith("--"))
            {
                return false;
            }
        }
        return true;
    }
}
