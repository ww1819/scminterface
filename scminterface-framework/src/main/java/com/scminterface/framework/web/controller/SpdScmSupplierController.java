package com.scminterface.framework.web.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.framework.web.service.ScmSupplierInterfaceService;

/**
 * SPD 调用的平台供应商主数据查询（内网前置机；与配送下载接口同类无鉴权，请网络层隔离）
 */
@RestController
@RequestMapping("/api/spd/scmSupplier")
public class SpdScmSupplierController
{
    @Autowired
    private ScmSupplierInterfaceService scmSupplierInterfaceService;

    @GetMapping("/profile")
    public AjaxResult profile(@RequestParam("hospitalCode") String hospitalCode,
        @RequestParam("supplierCode") String supplierCode,
        @RequestParam(value = "spdTenantId", required = false) String spdTenantId,
        HttpServletRequest request)
    {
        String ip = request.getRemoteAddr();
        Map<String, Object> data = scmSupplierInterfaceService.buildSupplierProfile(
            hospitalCode, supplierCode, spdTenantId, ip, "spd-api");
        if (data == null)
        {
            return AjaxResult.error("未找到平台供应商：" + supplierCode);
        }
        return AjaxResult.success(data);
    }

    @GetMapping("/listByHospital")
    public AjaxResult listByHospital(@RequestParam("hospitalCode") String hospitalCode)
    {
        List<Map<String, Object>> list = scmSupplierInterfaceService.listSuppliersByHospital(hospitalCode);
        return AjaxResult.success(list);
    }
}
