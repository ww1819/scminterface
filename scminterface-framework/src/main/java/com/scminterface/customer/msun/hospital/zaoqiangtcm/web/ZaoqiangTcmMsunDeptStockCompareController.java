package com.scminterface.customer.msun.hospital.zaoqiangtcm.web;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.customer.msun.MsunVendorConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.ZaoqiangTcmHospitalConstants;
import com.scminterface.customer.msun.hospital.zaoqiangtcm.config.ZaoqiangTcmMsunProperties;
import com.scminterface.customer.msun.spd.deptstock.service.MsunDeptStockCompareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 枣强：科室 SPD 库存 vs 众阳 HIS 镜像库存核对报表。
 */
@Api(tags = "众阳HIS-枣强-科室库存核对")
@RestController
@ConditionalOnProperty(prefix = ZaoqiangTcmHospitalConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@RequestMapping({
        ZaoqiangTcmHospitalConstants.API_PREFIX + "/dept-stock",
        ZaoqiangTcmHospitalConstants.SPD_API_PREFIX + "/dept-stock"
})
public class ZaoqiangTcmMsunDeptStockCompareController
{
    private final MsunDeptStockCompareService compareService;
    private final ZaoqiangTcmMsunProperties msunProperties;

    public ZaoqiangTcmMsunDeptStockCompareController(
            MsunDeptStockCompareService compareService,
            ZaoqiangTcmMsunProperties msunProperties)
    {
        this.compareService = compareService;
        this.msunProperties = msunProperties;
    }

    @ApiOperation("科室库存明细：SPD 数量 vs HIS 镜像数量（2.5.43 批次优先，否则 2.5.82 合并）")
    @GetMapping("/list")
    public AjaxResult list(
            @ApiParam("科室编码/名称/首拼模糊") @RequestParam(required = false) String departmentKeyword,
            @ApiParam("耗材编码/名称/首拼模糊") @RequestParam(required = false) String materialKeyword,
            @ApiParam("规格模糊") @RequestParam(required = false) String specKeyword,
            @ApiParam("SPD 科室 ID 精确") @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize)
    {
        try
        {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("departmentKeyword", departmentKeyword);
            params.put("materialKeyword", materialKeyword);
            params.put("specKeyword", specKeyword);
            params.put("departmentId", departmentId);
            params.put("pageNum", pageNum);
            params.put("pageSize", pageSize);
            Map<String, Object> data = compareService.queryList(msunProperties, params);
            return enrichEnv(AjaxResult.success(data));
        }
        catch (IllegalArgumentException | IllegalStateException ex)
        {
            return AjaxResult.error(ex.getMessage());
        }
        catch (Exception ex)
        {
            return AjaxResult.error("科室库存核对查询失败: " + ex.getMessage());
        }
    }

    private AjaxResult enrichEnv(AjaxResult result)
    {
        return result
                .put("vendorCode", MsunVendorConstants.VENDOR_CODE)
                .put("vendorName", MsunVendorConstants.VENDOR_NAME)
                .put("hospitalKey", msunProperties.getHospitalKey())
                .put("hospitalName", msunProperties.getHospitalName())
                .put("tenantId", msunProperties.getTenantId())
                .put("activeEnv", msunProperties.getActiveEnv())
                .put("msunBaseUrl", msunProperties.getBaseUrl());
    }
}
