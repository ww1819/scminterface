package com.scminterface.framework.web.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 耗材 SPD 主库 {@code sys_config}（参数设置），与若依 sys_config 表结构一致。
 */
@Mapper
public interface SpdSysConfigMapper
{
    String selectValueByKey(@Param("configKey") String configKey);
}
