package com.scminterface.web.controller.scm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.web.service.HospitalMaterialArchiveInterfaceService;

/**
 * 医院归属产品档案 — SCM 侧 REST（正式档查询 / 供应商修改申请 / 医院审核）
 */
@RestController
@RequestMapping("/api/scm/hospitalMaterialArchive")
public class HospitalMaterialArchiveScmController
{
    @Autowired
    private HospitalMaterialArchiveInterfaceService hospitalMaterialArchiveInterfaceService;

    @GetMapping("/archives")
    public AjaxResult listArchives(@RequestParam("hospitalCode") String hospitalCode,
        @RequestParam(value = "scmSupplierCode", required = false) String scmSupplierCode,
        @RequestParam(value = "keyword", required = false) String keyword)
    {
        if (StringUtils.isEmpty(hospitalCode))
        {
            return AjaxResult.error("hospitalCode 不能为空");
        }
        try
        {
            return AjaxResult.success(hospitalMaterialArchiveInterfaceService.pull(
                hospitalCode.trim(), trim(scmSupplierCode), trim(keyword)));
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/archives/{id}")
    public AjaxResult getArchive(@PathVariable("id") String id)
    {
        if (StringUtils.isEmpty(id))
        {
            return AjaxResult.error("id 不能为空");
        }
        try
        {
            Map<String, Object> arch = hospitalMaterialArchiveInterfaceService.getArchive(id.trim());
            if (arch == null || arch.isEmpty())
            {
                return AjaxResult.error("档案不存在");
            }
            return AjaxResult.success(arch);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/applies")
    public AjaxResult listApplies(@RequestParam(value = "hospitalCode", required = false) String hospitalCode,
        @RequestParam(value = "scmSupplierCode", required = false) String scmSupplierCode,
        @RequestParam(value = "applyStatus", required = false) String applyStatus)
    {
        try
        {
            return AjaxResult.success(hospitalMaterialArchiveInterfaceService.listModifyApplies(
                trim(hospitalCode), trim(scmSupplierCode), trim(applyStatus)));
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/applies/submit")
    public AjaxResult submitApply(@RequestBody Map<String, Object> body)
    {
        if (body == null)
        {
            return AjaxResult.error("请求体不能为空");
        }
        String archiveId = str(body.get("archiveId"));
        String hospitalCode = str(body.get("hospitalCode"));
        String scmSupplierCode = str(body.get("scmSupplierCode"));
        if (StringUtils.isEmpty(archiveId) || StringUtils.isEmpty(hospitalCode)
            || StringUtils.isEmpty(scmSupplierCode))
        {
            return AjaxResult.error("archiveId/hospitalCode/scmSupplierCode 不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> propose = body.get("propose") instanceof Map
            ? (Map<String, Object>) body.get("propose") : body;
        @SuppressWarnings("unchecked")
        List<String> changedFields = body.get("changedFields") instanceof List
            ? (List<String>) body.get("changedFields") : null;
        String applyBy = firstNonEmpty(str(body.get("applyBy")), str(body.get("operBy")), "supplier");
        try
        {
            String applyId = hospitalMaterialArchiveInterfaceService.submitModifyApply(
                archiveId, hospitalCode, scmSupplierCode, applyBy, propose, changedFields);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("applyId", applyId);
            return AjaxResult.success(data);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/applies/audit")
    public AjaxResult auditApply(@RequestBody Map<String, Object> body)
    {
        if (body == null)
        {
            return AjaxResult.error("请求体不能为空");
        }
        String applyId = str(body.get("applyId"));
        String decision = str(body.get("decision"));
        if (StringUtils.isEmpty(applyId) || StringUtils.isEmpty(decision))
        {
            return AjaxResult.error("applyId/decision 不能为空");
        }
        String auditBy = firstNonEmpty(str(body.get("auditBy")), str(body.get("operBy")), "hospital");
        String auditRemark = str(body.get("auditRemark"));
        try
        {
            hospitalMaterialArchiveInterfaceService.auditModifyApply(applyId, decision, auditBy, auditRemark);
            return AjaxResult.success();
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    private static String str(Object o)
    {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static String trim(String s)
    {
        return s == null ? null : s.trim();
    }

    private static String firstNonEmpty(String... vals)
    {
        if (vals == null)
        {
            return null;
        }
        for (String v : vals)
        {
            if (StringUtils.isNotEmpty(v))
            {
                return v.trim();
            }
        }
        return null;
    }
}
