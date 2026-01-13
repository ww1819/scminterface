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
import com.scminterface.framework.web.mapper.SpdSystemConfigMapper;

/**
 * SPD系统参数配置服务
 * 
 * @author scminterface
 */
@Service
public class SpdSystemConfigService
{
    private static final Logger log = LoggerFactory.getLogger(SpdSystemConfigService.class);

    @Autowired
    private SpdSystemConfigMapper spdSystemConfigMapper;

    /**
     * 查询所有配置
     * 
     * @return 配置列表
     */
    @DataSource(DataSourceType.SPD)
    public List<Map<String, Object>> getAllConfigs()
    {
        return spdSystemConfigMapper.selectAll();
    }

    /**
     * 根据配置键查询配置值
     * 
     * @param configKey 配置键
     * @return 配置值
     */
    @DataSource(DataSourceType.SPD)
    public String getConfigValue(String configKey)
    {
        return spdSystemConfigMapper.selectValueByKey(configKey);
    }

    /**
     * 保存配置
     * 
     * @param configKey 配置键
     * @param configValue 配置值
     * @param configDesc 配置描述
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int saveConfig(String configKey, String configValue, String configDesc)
    {
        Map<String, Object> config = new HashMap<>();
        config.put("configKey", configKey);
        config.put("configValue", configValue);
        config.put("configDesc", configDesc);
        return spdSystemConfigMapper.insertOrUpdate(config);
    }

    /**
     * 删除配置
     * 
     * @param configKey 配置键
     * @return 结果
     */
    @DataSource(DataSourceType.SPD)
    public int deleteConfig(String configKey)
    {
        return spdSystemConfigMapper.deleteByKey(configKey);
    }
}
