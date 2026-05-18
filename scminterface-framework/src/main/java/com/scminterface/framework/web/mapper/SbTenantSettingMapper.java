package com.scminterface.framework.web.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SbTenantSettingMapper
{
    String selectValueByTenantAndKey(@Param("tenantId") String tenantId, @Param("settingKey") String settingKey);

    int insertSetting(@Param("tenantId") String tenantId, @Param("settingKey") String settingKey,
        @Param("settingValue") String settingValue, @Param("remark") String remark);

    int updateSettingValue(@Param("tenantId") String tenantId, @Param("settingKey") String settingKey,
        @Param("settingValue") String settingValue, @Param("remark") String remark);
}
