package com.scminterface.framework.web.controller;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scminterface.common.core.domain.AjaxResult;
import com.scminterface.framework.web.service.HengshuiTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 衡水三数据控制器
 */
@Api(tags = "衡水三数据")
@RestController
@RequestMapping("/api/hengshui")
public class HengshuisanController
{
    private static final Logger log = LoggerFactory.getLogger(HengshuisanController.class);

    @Autowired
    private HengshuiTaskService hengshuiTaskService;

    @ApiOperation("补全历史住院收费镜像执行科室")
    @PostMapping("/charge/backfillExecDept/inpatient")
    public AjaxResult backfillInpatientExecDept(@RequestBody Map<String, String> body)
    {
        String beginDate = body == null ? null : body.get("beginDate");
        String endDate = body == null ? null : body.get("endDate");
        Map<String, Object> result = hengshuiTaskService.backfillInpatientExecDept(beginDate, endDate);
        return toAjaxResult(result);
    }

    @ApiOperation("补全历史门诊收费镜像执行科室")
    @PostMapping("/charge/backfillExecDept/outpatient")
    public AjaxResult backfillOutpatientExecDept(@RequestBody Map<String, String> body)
    {
        String beginDate = body == null ? null : body.get("beginDate");
        String endDate = body == null ? null : body.get("endDate");
        Map<String, Object> result = hengshuiTaskService.backfillOutpatientExecDept(beginDate, endDate);
        return toAjaxResult(result);
    }

    private AjaxResult toAjaxResult(Map<String, Object> result)
    {
        if (result == null)
        {
            return AjaxResult.error("无返回结果");
        }
        if (Boolean.TRUE.equals(result.get("success")))
        {
            AjaxResult ok = AjaxResult.success(String.valueOf(result.get("message")));
            Map<String, Object> data = new HashMap<>();
            data.put("updatedCount", result.get("updatedCount"));
            data.put("skippedCount", result.get("skippedCount"));
            data.put("hisMissingExecCount", result.get("hisMissingExecCount"));
            data.put("notFoundCount", result.get("notFoundCount"));
            data.put("unifiedSyncedCount", result.get("unifiedSyncedCount"));
            ok.put(AjaxResult.DATA_TAG, data);
            return ok;
        }
        return AjaxResult.error(String.valueOf(result.get("message")));
    }
}
