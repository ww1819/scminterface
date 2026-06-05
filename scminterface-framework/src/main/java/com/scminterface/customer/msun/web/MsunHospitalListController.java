package com.scminterface.customer.msun.web;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.MsunVendorConstants;
import com.scminterface.customer.msun.hospital.MsunHospitalRegistry;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 众阳 HIS 已接入医院客户列表（供联调页选择医院）。
 */
@Api(tags = "众阳HIS-医院客户列表")
@RestController
@RequestMapping(MsunVendorConstants.API_PREFIX)
public class MsunHospitalListController
{
    private final Environment environment;

    public MsunHospitalListController(Environment environment)
    {
        this.environment = environment;
    }

    @ApiOperation("已登记的医院客户列表")
    @GetMapping("/hospitals")
    public AjaxResult listHospitals()
    {
        List<Map<String, Object>> hospitals = new ArrayList<>();
        for (MsunHospitalRegistry registry : MsunHospitalRegistry.values())
        {
            Map<String, Object> item = new LinkedHashMap<>(8);
            item.put("vendorCode", registry.getVendorCode());
            item.put("vendorName", registry.getVendorName());
            item.put("hospitalKey", registry.getHospitalKey());
            item.put("hospitalName", registry.getHospitalName());
            item.put("apiPrefix", registry.apiPrefix());
            item.put("spdQueryApiPrefix", MsunVendorConstants.spdQueryApiPrefix(registry.getHospitalKey()));
            item.put("configPrefix", registry.configPrefix());
            item.put("enabled", environment.getProperty(registry.configPrefix() + ".enabled", Boolean.class, false));
            hospitals.add(item);
        }
        Map<String, Object> payload = new LinkedHashMap<>(4);
        payload.put("vendorCode", MsunVendorConstants.VENDOR_CODE);
        payload.put("vendorName", MsunVendorConstants.VENDOR_NAME);
        payload.put("hospitals", hospitals);
        return AjaxResult.success(payload);
    }
}
