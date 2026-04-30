package com.scminterface.framework.web.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.util.ZsUuid7;
import com.scminterface.framework.web.mapper.ScmPartyLookupMapper;

/**
 * 供 SPD 经前置机拉取的平台供应商主数据（SCM 库）
 */
@Service
public class ScmSupplierInterfaceService
{
    @Autowired
    private ScmPartyLookupMapper scmPartyLookupMapper;

    @DataSource(DataSourceType.SCM)
    public Map<String, Object> buildSupplierProfile(String hospitalCode, String supplierCode,
        String spdTenantId, String requestIp, String createBy)
    {
        Map<String, Object> row = scmPartyLookupMapper.selectScmSupplierRowByCode(supplierCode);
        if (row == null || row.isEmpty())
        {
            return null;
        }
        int bound = scmPartyLookupMapper.countHospitalSupplierActive(hospitalCode, supplierCode);
        String scope = bound > 0 ? "FULL" : "LIMITED";
        scmPartyLookupMapper.insertScmSupplierExportLog(ZsUuid7.newString(), hospitalCode, supplierCode, scope,
            spdTenantId, requestIp, createBy != null ? createBy : "api");
        Map<String, Object> out = new HashMap<>();
        out.put("hospitalSupplierBound", bound > 0);
        out.put("exportScope", scope);
        out.put("supplier", row);
        return out;
    }

    @DataSource(DataSourceType.SCM)
    public List<Map<String, Object>> listSuppliersByHospital(String hospitalCode)
    {
        return scmPartyLookupMapper.selectSuppliersByHospitalCode(hospitalCode);
    }
}
