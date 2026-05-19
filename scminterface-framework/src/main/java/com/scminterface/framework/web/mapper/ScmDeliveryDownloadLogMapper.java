package com.scminterface.framework.web.mapper;

import java.util.Date;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 配送单接口下载记录（写入 SCM 库，供反审核等业务校验）
 */
@Mapper
public interface ScmDeliveryDownloadLogMapper
{
    int insertLog(@Param("id") String id, @Param("deliveryId") String deliveryId, @Param("downloadTime") Date downloadTime,
        @Param("downloadChannel") String downloadChannel);
}
