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
import com.scminterface.framework.web.mapper.ScmSystemConfigMapper;

/**
 * SCM系统参数配置服务
 * 
 * @author scminterface
 */
@Service
public class ScmSystemConfigService
{
    private static final Logger log = LoggerFactory.getLogger(ScmSystemConfigService.class);

    @Autowired
    private ScmSystemConfigMapper scmSystemConfigMapper;

    /**
     * 查询所有配置
     * 
     * @return 配置列表
     */
    @DataSource(DataSourceType.SCM)
    public List<Map<String, Object>> getAllConfigs()
    {
        return scmSystemConfigMapper.selectAll();
    }

    /**
     * 根据配置键查询配置值
     * 
     * @param configKey 配置键
     * @return 配置值
     */
    @DataSource(DataSourceType.SCM)
    public String getConfigValue(String configKey)
    {
        return scmSystemConfigMapper.selectValueByKey(configKey);
    }

    /**
     * 保存配置
     * 
     * @param configKey 配置键
     * @param configValue 配置值
     * @param configDesc 配置描述
     * @return 结果
     */
    @DataSource(DataSourceType.SCM)
    public int saveConfig(String configKey, String configValue, String configDesc)
    {
        Map<String, Object> config = new HashMap<>();
        config.put("configKey", configKey);
        config.put("configValue", configValue);
        config.put("configDesc", configDesc);
        return scmSystemConfigMapper.insertOrUpdate(config);
    }

    /**
     * 删除配置
     * 
     * @param configKey 配置键
     * @return 结果
     */
    @DataSource(DataSourceType.SCM)
    public int deleteConfig(String configKey)
    {
        return scmSystemConfigMapper.deleteByKey(configKey);
    }
}
