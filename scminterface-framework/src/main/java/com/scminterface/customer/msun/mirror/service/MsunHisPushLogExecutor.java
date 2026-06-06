package com.scminterface.customer.msun.mirror.service;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.msun.mirror.mapper.MsunHisMirrorMapper;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorPushLogRowSupport;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorTableNames;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MsunHisPushLogExecutor
{
    private final MsunHisMirrorMapper mirrorMapper;

    public MsunHisPushLogExecutor(MsunHisMirrorMapper mirrorMapper)
    {
        this.mirrorMapper = mirrorMapper;
    }

    @DataSource(DataSourceType.SPD)
    public int insert(Map<String, Object> row)
    {
        return mirrorMapper.insertMirrorRow(
                MsunHisMirrorTableNames.PUSH_LOG,
                MsunHisMirrorPushLogRowSupport.sanitize(row));
    }
}
