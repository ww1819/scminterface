package com.scminterface.customer.msun.mirror.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorSqlProvider;

/**
 * 众阳 HIS 接口镜像库写入。
 */
@Mapper
public interface MsunHisMirrorMapper
{
    @InsertProvider(type = MsunHisMirrorSqlProvider.class, method = "upsertMirrorRow")
    int upsertMirrorRow(@Param("table") String table, @Param("row") Map<String, Object> row);

    @DeleteProvider(type = MsunHisMirrorSqlProvider.class, method = "deleteYkInstockDetails")
    int deleteYkInstockDetails(
            @Param("hospitalKey") String hospitalKey,
            @Param("activeEnv") String activeEnv,
            @Param("storageInstockId") String storageInstockId);
}
