package com.scminterface.framework.web.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.framework.util.ZsUuid7;
import com.scminterface.framework.web.mapper.HospitalMaterialArchiveMapper;

/**
 * 医院归属产品档案（SCM 库）：推送 / 拉取 / 供应商申请审核
 */
@Service
public class HospitalMaterialArchiveInterfaceService
{
    private static final Set<String> ARCH_FIELDS = new HashSet<>(Arrays.asList(
        "material_name", "pinyin_code", "specification", "model", "unit_name", "price", "sale_price",
        "register_no", "register_name", "manufacturer_name", "udi_code", "medical_name", "medical_no",
        "brand", "useto", "quality", "function_desc", "is_way", "country_no", "country_name", "description",
        "period_date", "package_speci", "min_package_qty", "is_gz", "is_billing", "spd_material_code",
        "archive_status"
    ));

    @Autowired
    private HospitalMaterialArchiveMapper hospitalMaterialArchiveMapper;

    @DataSource(DataSourceType.SCM)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pushFromHospital(String hospitalCode, String tenantId, String pushBy,
        String requestId, List<String> fieldWhitelist, List<Map<String, Object>> items)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        if (StringUtils.isEmpty(hospitalCode))
        {
            throw new IllegalArgumentException("hospitalCode 不能为空");
        }
        if (items == null || items.isEmpty())
        {
            throw new IllegalArgumentException("items 不能为空");
        }
        String batchId = ZsUuid7.newString();
        Map<String, Object> batch = new LinkedHashMap<>();
        batch.put("id", batchId);
        batch.put("hospital_code", hospitalCode);
        batch.put("spd_tenant_id", tenantId);
        batch.put("push_by", pushBy);
        batch.put("item_count", items.size());
        batch.put("success_count", 0);
        batch.put("fail_count", 0);
        batch.put("field_whitelist_json", fieldWhitelist == null ? null : JSON.toJSONString(fieldWhitelist));
        batch.put("request_id", requestId);
        batch.put("result_status", "0");
        batch.put("create_by", pushBy);
        hospitalMaterialArchiveMapper.insertPushBatch(batch);

        int ok = 0;
        int fail = 0;
        List<Map<String, Object>> itemResults = new ArrayList<>();
        for (Map<String, Object> raw : items)
        {
            Map<String, Object> itemResult = pushOne(hospitalCode, tenantId, pushBy, batchId, fieldWhitelist, raw);
            itemResults.add(itemResult);
            if ("FAIL".equals(itemResult.get("actionType")))
            {
                fail++;
            }
            else
            {
                ok++;
            }
        }
        String status = fail == 0 ? "1" : (ok == 0 ? "3" : "2");
        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", batchId);
        upd.put("success_count", ok);
        upd.put("fail_count", fail);
        upd.put("result_status", status);
        upd.put("result_msg", "success=" + ok + ",fail=" + fail);
        hospitalMaterialArchiveMapper.updatePushBatch(upd);

        out.put("batchId", batchId);
        out.put("successCount", ok);
        out.put("failCount", fail);
        out.put("resultStatus", status);
        out.put("items", itemResults);
        return out;
    }

    private Map<String, Object> pushOne(String hospitalCode, String tenantId, String pushBy, String batchId,
        List<String> whitelist, Map<String, Object> raw)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        String spdMaterialId = str(raw.get("spdMaterialId"));
        if (StringUtils.isEmpty(spdMaterialId))
        {
            spdMaterialId = str(raw.get("spd_material_id"));
        }
        String supplierCode = str(raw.get("scmSupplierCode"));
        if (StringUtils.isEmpty(supplierCode))
        {
            supplierCode = str(raw.get("scm_supplier_code"));
        }
        result.put("spdMaterialId", spdMaterialId);
        if (StringUtils.isEmpty(spdMaterialId) || StringUtils.isEmpty(supplierCode))
        {
            result.put("actionType", "FAIL");
            result.put("errorMsg", "spdMaterialId/scmSupplierCode 不能为空");
            insertFailItem(batchId, spdMaterialId, supplierCode, raw, "spdMaterialId/scmSupplierCode 不能为空");
            return result;
        }
        try
        {
            Map<String, Object> existing = hospitalMaterialArchiveMapper.selectArchiveByHospitalAndSpdMaterial(
                hospitalCode, spdMaterialId);
            String beforeJson = existing == null ? null : JSON.toJSONString(existing);
            String oldSupplier = existing == null ? null : str(existing.get("scm_supplier_code"));
            Map<String, Object> merged = existing == null ? new LinkedHashMap<>() : new LinkedHashMap<>(existing);
            applyWhitelist(merged, raw, whitelist);
            merged.put("hospital_code", hospitalCode);
            merged.put("scm_supplier_code", supplierCode);
            merged.put("spd_tenant_id", tenantId);
            merged.put("spd_material_id", spdMaterialId);
            if (StringUtils.isEmpty(str(merged.get("material_name"))))
            {
                String name = str(raw.get("materialName"));
                if (StringUtils.isEmpty(name))
                {
                    name = str(raw.get("material_name"));
                }
                merged.put("material_name", name);
            }
            if (StringUtils.isEmpty(str(merged.get("material_name"))))
            {
                throw new IllegalArgumentException("materialName 不能为空");
            }
            merged.put("last_push_batch_id", batchId);
            String action;
            String archiveId;
            if (existing == null)
            {
                archiveId = ZsUuid7.newString();
                merged.put("id", archiveId);
                merged.put("archive_status", "0");
                merged.put("create_by", pushBy);
                hospitalMaterialArchiveMapper.insertArchive(toDbKeys(merged));
                action = "INSERT";
            }
            else
            {
                archiveId = str(existing.get("id"));
                merged.put("id", archiveId);
                merged.put("update_by", pushBy);
                hospitalMaterialArchiveMapper.updateArchive(toDbKeys(merged));
                action = "UPDATE";
                if (StringUtils.isNotEmpty(oldSupplier) && !oldSupplier.equals(supplierCode))
                {
                    hospitalMaterialArchiveMapper.voidPendingApplies(archiveId, pushBy, "医院推送换绑供应商，自动作废待审申请");
                }
            }
            Map<String, Object> after = hospitalMaterialArchiveMapper.selectArchiveById(archiveId);
            String afterJson = JSON.toJSONString(after);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ZsUuid7.newString());
            item.put("batch_id", batchId);
            item.put("archive_id", archiveId);
            item.put("spd_material_id", spdMaterialId);
            item.put("scm_supplier_code", supplierCode);
            item.put("action_type", action);
            item.put("payload_json", JSON.toJSONString(raw));
            item.put("before_json", beforeJson);
            item.put("after_json", afterJson);
            hospitalMaterialArchiveMapper.insertPushItem(item);

            Map<String, Object> clog = new LinkedHashMap<>();
            clog.put("id", ZsUuid7.newString());
            clog.put("archive_id", archiveId);
            clog.put("hospital_code", hospitalCode);
            clog.put("change_source", "HOSPITAL_PUSH");
            clog.put("source_id", batchId);
            clog.put("before_json", beforeJson);
            clog.put("after_json", afterJson);
            clog.put("changed_fields", whitelist == null ? null : String.join(",", whitelist));
            clog.put("oper_by", pushBy);
            hospitalMaterialArchiveMapper.insertChangeLog(clog);

            result.put("actionType", action);
            result.put("archiveId", archiveId);
            result.put("scmUpdateTime", after != null ? after.get("update_time") : null);
            return result;
        }
        catch (Exception e)
        {
            result.put("actionType", "FAIL");
            result.put("errorMsg", e.getMessage());
            insertFailItem(batchId, spdMaterialId, supplierCode, raw, e.getMessage());
            return result;
        }
    }

    private void insertFailItem(String batchId, String spdMaterialId, String supplierCode, Map<String, Object> raw,
        String err)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", ZsUuid7.newString());
        item.put("batch_id", batchId);
        item.put("spd_material_id", spdMaterialId == null ? "" : spdMaterialId);
        item.put("scm_supplier_code", supplierCode);
        item.put("action_type", "FAIL");
        item.put("payload_json", JSON.toJSONString(raw));
        item.put("error_msg", err);
        hospitalMaterialArchiveMapper.insertPushItem(item);
    }

    @DataSource(DataSourceType.SCM)
    public List<Map<String, Object>> pull(String hospitalCode, String scmSupplierCode, String keyword)
    {
        if (StringUtils.isEmpty(hospitalCode))
        {
            throw new IllegalArgumentException("hospitalCode 不能为空");
        }
        return hospitalMaterialArchiveMapper.selectArchivesForPull(hospitalCode, scmSupplierCode, keyword);
    }

    @DataSource(DataSourceType.SCM)
    @Transactional(rollbackFor = Exception.class)
    public String submitModifyApply(String archiveId, String hospitalCode, String scmSupplierCode,
        String applyBy, Map<String, Object> propose, List<String> changedFields)
    {
        Map<String, Object> arch = hospitalMaterialArchiveMapper.selectArchiveById(archiveId);
        if (arch == null)
        {
            throw new IllegalArgumentException("档案不存在");
        }
        if (!hospitalCode.equals(str(arch.get("hospital_code"))))
        {
            throw new IllegalArgumentException("医院编码不匹配");
        }
        if (!scmSupplierCode.equals(str(arch.get("scm_supplier_code"))))
        {
            throw new IllegalArgumentException("仅档案所属供应商可申请修改");
        }
        List<Map<String, Object>> pending = hospitalMaterialArchiveMapper.selectPendingApplies(archiveId, scmSupplierCode);
        if (pending != null && !pending.isEmpty())
        {
            throw new IllegalArgumentException("已存在待审修改申请，请先处理或作废后再提交");
        }
        String id = ZsUuid7.newString();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("archive_id", archiveId);
        row.put("hospital_code", hospitalCode);
        row.put("scm_supplier_code", scmSupplierCode);
        row.put("propose_json", JSON.toJSONString(propose));
        row.put("base_json", JSON.toJSONString(arch));
        row.put("changed_fields", changedFields == null ? null : String.join(",", changedFields));
        row.put("apply_by", applyBy);
        row.put("create_by", applyBy);
        hospitalMaterialArchiveMapper.insertModifyApply(row);
        return id;
    }

    @DataSource(DataSourceType.SCM)
    @Transactional(rollbackFor = Exception.class)
    public void auditModifyApply(String applyId, String decision, String auditBy, String auditRemark)
    {
        Map<String, Object> apply = hospitalMaterialArchiveMapper.selectModifyApplyById(applyId);
        if (apply == null)
        {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!"0".equals(str(apply.get("apply_status"))))
        {
            throw new IllegalArgumentException("申请不是待审状态");
        }
        String status;
        if ("pass".equalsIgnoreCase(decision) || "1".equals(decision))
        {
            status = "1";
        }
        else if ("reject".equalsIgnoreCase(decision) || "2".equals(decision))
        {
            status = "2";
        }
        else if ("void".equalsIgnoreCase(decision) || "3".equals(decision))
        {
            status = "3";
        }
        else
        {
            throw new IllegalArgumentException("decision 须为 pass/reject/void");
        }
        if ("1".equals(status))
        {
            String archiveId = str(apply.get("archive_id"));
            Map<String, Object> current = hospitalMaterialArchiveMapper.selectArchiveById(archiveId);
            if (current == null)
            {
                throw new IllegalArgumentException("正式档不存在");
            }
            String beforeJson = JSON.toJSONString(current);
            JSONObject propose = JSON.parseObject(str(apply.get("propose_json")));
            String changed = str(apply.get("changed_fields"));
            List<String> fields = new ArrayList<>();
            if (StringUtils.isNotEmpty(changed))
            {
                for (String f : changed.split(","))
                {
                    if (StringUtils.isNotEmpty(f))
                    {
                        fields.add(f.trim());
                    }
                }
            }
            else if (propose != null)
            {
                fields.addAll(propose.keySet());
            }
            Map<String, Object> upd = new LinkedHashMap<>();
            upd.put("id", archiveId);
            for (String f : fields)
            {
                String dbKey = toSnake(f);
                if (!ARCH_FIELDS.contains(dbKey) && !"scm_supplier_code".equals(dbKey))
                {
                    continue;
                }
                Object val = propose == null ? null : propose.get(f);
                if (val == null && propose != null)
                {
                    val = propose.get(dbKey);
                }
                if (val != null)
                {
                    upd.put(dbKey, val);
                }
            }
            upd.put("last_apply_id", applyId);
            upd.put("update_by", auditBy);
            hospitalMaterialArchiveMapper.updateArchive(upd);
            Map<String, Object> after = hospitalMaterialArchiveMapper.selectArchiveById(archiveId);
            Map<String, Object> clog = new LinkedHashMap<>();
            clog.put("id", ZsUuid7.newString());
            clog.put("archive_id", archiveId);
            clog.put("hospital_code", str(apply.get("hospital_code")));
            clog.put("change_source", "SUPPLIER_APPLY_APPROVED");
            clog.put("source_id", applyId);
            clog.put("before_json", beforeJson);
            clog.put("after_json", JSON.toJSONString(after));
            clog.put("changed_fields", changed);
            clog.put("oper_by", auditBy);
            hospitalMaterialArchiveMapper.insertChangeLog(clog);
        }
        Map<String, Object> updApply = new LinkedHashMap<>();
        updApply.put("id", applyId);
        updApply.put("apply_status", status);
        updApply.put("audit_by", auditBy);
        updApply.put("audit_remark", auditRemark);
        updApply.put("update_by", auditBy);
        hospitalMaterialArchiveMapper.updateModifyApply(updApply);
    }

    @DataSource(DataSourceType.SCM)
    public List<Map<String, Object>> listModifyApplies(String hospitalCode, String scmSupplierCode, String applyStatus)
    {
        return hospitalMaterialArchiveMapper.selectModifyApplies(hospitalCode, scmSupplierCode, applyStatus);
    }

    @DataSource(DataSourceType.SCM)
    public Map<String, Object> getArchive(String id)
    {
        return hospitalMaterialArchiveMapper.selectArchiveById(id);
    }

    private void applyWhitelist(Map<String, Object> target, Map<String, Object> raw, List<String> whitelist)
    {
        Map<String, Object> normalized = camelToSnakeMap(raw);
        if (whitelist == null || whitelist.isEmpty())
        {
            for (String k : ARCH_FIELDS)
            {
                if (normalized.containsKey(k) && normalized.get(k) != null)
                {
                    target.put(k, normalized.get(k));
                }
            }
            return;
        }
        for (String f : whitelist)
        {
            String dbKey = toSnake(f);
            if (!ARCH_FIELDS.contains(dbKey))
            {
                continue;
            }
            if (normalized.containsKey(dbKey))
            {
                target.put(dbKey, normalized.get(dbKey));
            }
        }
    }

    private static Map<String, Object> camelToSnakeMap(Map<String, Object> raw)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        if (raw == null)
        {
            return m;
        }
        for (Map.Entry<String, Object> e : raw.entrySet())
        {
            m.put(toSnake(e.getKey()), e.getValue());
        }
        // aliases
        if (raw.containsKey("materialName"))
        {
            m.put("material_name", raw.get("materialName"));
        }
        if (raw.containsKey("specification") || raw.containsKey("speci"))
        {
            m.put("specification", raw.get("specification") != null ? raw.get("specification") : raw.get("speci"));
        }
        if (raw.containsKey("manufacturerName"))
        {
            m.put("manufacturer_name", raw.get("manufacturerName"));
        }
        if (raw.containsKey("udiCode") || raw.containsKey("udiNo"))
        {
            m.put("udi_code", raw.get("udiCode") != null ? raw.get("udiCode") : raw.get("udiNo"));
        }
        if (raw.containsKey("unitName") || raw.containsKey("unit"))
        {
            m.put("unit_name", raw.get("unitName") != null ? raw.get("unitName") : raw.get("unit"));
        }
        if (raw.containsKey("function"))
        {
            m.put("function_desc", raw.get("function"));
        }
        if (raw.containsKey("spdMaterialCode") || raw.containsKey("code"))
        {
            m.put("spd_material_code", raw.get("spdMaterialCode") != null ? raw.get("spdMaterialCode") : raw.get("code"));
        }
        return m;
    }

    private static Map<String, Object> toDbKeys(Map<String, Object> src)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : src.entrySet())
        {
            m.put(toSnake(e.getKey()), e.getValue());
        }
        return m;
    }

    private static String toSnake(String s)
    {
        if (s == null)
        {
            return null;
        }
        if (s.indexOf('_') >= 0)
        {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (Character.isUpperCase(c))
            {
                sb.append('_').append(Character.toLowerCase(c));
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String str(Object o)
    {
        return o == null ? null : String.valueOf(o).trim();
    }
}
