package com.scminterface.framework.web.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScmBridgeInboxMapper
{
    List<Map<String, Object>> selectPending(@Param("hospitalCode") String hospitalCode,
        @Param("tenantId") String tenantId,
        @Param("limit") int limit);

    int ackByIds(@Param("hospitalCode") String hospitalCode,
        @Param("messageIds") List<String> messageIds,
        @Param("ackedStatus") String ackedStatus);

    int insert(@Param("id") String id,
        @Param("hospitalCode") String hospitalCode,
        @Param("tenantId") String tenantId,
        @Param("msgType") String msgType,
        @Param("payloadJson") String payloadJson,
        @Param("status") String status,
        @Param("createBy") String createBy);
}
