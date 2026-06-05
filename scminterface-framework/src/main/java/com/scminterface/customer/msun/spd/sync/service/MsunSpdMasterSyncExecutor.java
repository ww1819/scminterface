package com.scminterface.customer.msun.spd.sync.service;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.spd.sync.mapper.MsunSpdMasterSyncMapper;
import com.scminterface.customer.msun.spd.sync.support.MsunSpdFieldSupport;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 镜像批次 → SPD 主数据写入（独立 Bean 保证 {@link DataSource} 切面）。
 */
@Service
public class MsunSpdMasterSyncExecutor
{
    private static final String DEFAULT_USER_PASSWORD =
            "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";

    private static final Set<String> MIRROR_TABLES = new HashSet<>();

    static
    {
        MIRROR_TABLES.add("m_dept");
        MIRROR_TABLES.add("m_user_identity");
        MIRROR_TABLES.add("m_supplier");
        MIRROR_TABLES.add("m_producer");
        MIRROR_TABLES.add("m_dict_category");
        MIRROR_TABLES.add("m_drug_dict");
    }

    private final MsunSpdMasterSyncMapper syncMapper;

    public MsunSpdMasterSyncExecutor(MsunSpdMasterSyncMapper syncMapper)
    {
        this.syncMapper = syncMapper;
    }

    @DataSource(DataSourceType.SPD)
    public int execute(MsunHospitalRuntime runtime, String apiCode, String batchNo)
    {
        if (runtime == null || StringUtils.isEmpty(batchNo))
        {
            return 0;
        }
        switch (apiCode)
        {
            case "2.1.9":
                return syncDepartments(runtime, batchNo);
            case "2.1.12":
                return syncUsers(runtime, batchNo);
            case "2.5.62":
                return syncSuppliers(runtime, batchNo);
            case "2.5.63":
                return syncFactories(runtime, batchNo);
            case "2.5.58":
                return syncCategories(runtime, batchNo);
            case "2.5.44":
                return syncMaterials(runtime, batchNo);
            default:
                return 0;
        }
    }

    private int syncDepartments(MsunHospitalRuntime runtime, String batchNo)
    {
        List<Map<String, Object>> rows = listMirror("m_dept", runtime, batchNo);
        int count = 0;
        for (Map<String, Object> row : rows)
        {
            String hisId = str(row, "dept_id");
            if (StringUtils.isEmpty(hisId))
            {
                continue;
            }
            Map<String, Object> spd = new HashMap<>(16);
            spd.put("code", MsunSpdFieldSupport.truncate(str(row, "dept_code"), 64));
            spd.put("name", MsunSpdFieldSupport.truncate(str(row, "dept_name"), 255));
            spd.put("referredName", MsunSpdFieldSupport.truncate(str(row, "input_code"), 64));
            spd.put("hisId", hisId);
            spd.put("tenantId", runtime.getTenantId());
            spd.put("delFlag", MsunSpdFieldSupport.toFdDelFlag(str(row, "invalid_flag")));
            spd.put("parentId", null);
            spd.put("createBy", MsunSpdFieldSupport.syncBy());
            spd.put("updateBy", MsunSpdFieldSupport.syncBy());
            count += syncMapper.upsertFdDepartment(spd);
        }
        for (Map<String, Object> row : rows)
        {
            String hisId = str(row, "dept_id");
            String parentHisId = str(row, "parent_id");
            if (StringUtils.isNotEmpty(hisId) && StringUtils.isNotEmpty(parentHisId))
            {
                syncMapper.updateFdDepartmentParent(runtime.getTenantId(), hisId, parentHisId);
            }
        }
        return count;
    }

    private int syncUsers(MsunHospitalRuntime runtime, String batchNo)
    {
        List<Map<String, Object>> rows = listMirror("m_user_identity", runtime, batchNo);
        int count = 0;
        String tenantId = runtime.getTenantId();
        for (Map<String, Object> row : rows)
        {
            String identityId = str(row, "identity_id");
            if (StringUtils.isEmpty(identityId))
            {
                continue;
            }
            String loginName = buildLoginName(row, identityId);
            Long deptId = resolveDeptId(tenantId, str(row, "dept_id"));

            Map<String, Object> spd = new HashMap<>(20);
            spd.put("userName", MsunSpdFieldSupport.truncate(loginName, 30));
            spd.put("nickName", MsunSpdFieldSupport.truncate(str(row, "user_name"), 30));
            spd.put("hisId", str(row, "user_id"));
            spd.put("hisIdentityId", identityId);
            spd.put("customerId", tenantId);
            spd.put("deptId", deptId);
            spd.put("status", "0");
            spd.put("delFlag", "0");
            spd.put("password", DEFAULT_USER_PASSWORD);
            spd.put("createBy", MsunSpdFieldSupport.syncBy());
            spd.put("updateBy", MsunSpdFieldSupport.syncBy());
            count += syncMapper.upsertSysUser(spd);

            Long userId = syncMapper.selectSysUserIdByHisIdentity(tenantId, identityId);
            if (userId != null && deptId != null)
            {
                Map<String, Object> rel = new HashMap<>(8);
                rel.put("userId", userId);
                rel.put("departmentId", deptId);
                rel.put("status", 0);
                rel.put("tenantId", tenantId);
                rel.put("createBy", MsunSpdFieldSupport.syncBy());
                syncMapper.insertSysUserDepartmentIfAbsent(rel);
            }
        }
        return count;
    }

    private int syncSuppliers(MsunHospitalRuntime runtime, String batchNo)
    {
        List<Map<String, Object>> rows = listMirror("m_supplier", runtime, batchNo);
        int count = 0;
        for (Map<String, Object> row : rows)
        {
            String hisId = str(row, "supplier_id");
            if (StringUtils.isEmpty(hisId))
            {
                continue;
            }
            Map<String, Object> spd = new HashMap<>(20);
            spd.put("code", MsunSpdFieldSupport.truncate(
                    MsunSpdFieldSupport.firstNonBlank(str(row, "supplier_code"), hisId), 64));
            spd.put("name", MsunSpdFieldSupport.truncate(str(row, "supplier_name"), 255));
            spd.put("referredCode", MsunSpdFieldSupport.truncate(str(row, "input_code"), 64));
            spd.put("hisId", hisId);
            spd.put("tenantId", runtime.getTenantId());
            spd.put("delFlag", MsunSpdFieldSupport.toFdDelFlag(str(row, "invalid_flag")));
            spd.put("address", MsunSpdFieldSupport.truncate(str(row, "address"), 500));
            spd.put("phone", MsunSpdFieldSupport.truncate(str(row, "phone"), 32));
            spd.put("contacts", MsunSpdFieldSupport.truncate(str(row, "saleman"), 64));
            spd.put("contactsPhone", MsunSpdFieldSupport.truncate(str(row, "saleman_phone"), 32));
            spd.put("taxNumber", MsunSpdFieldSupport.truncate(str(row, "social_credit_code"), 64));
            spd.put("createBy", MsunSpdFieldSupport.syncBy());
            spd.put("updateBy", MsunSpdFieldSupport.syncBy());
            count += syncMapper.upsertFdSupplier(spd);
        }
        return count;
    }

    private int syncFactories(MsunHospitalRuntime runtime, String batchNo)
    {
        List<Map<String, Object>> rows = listMirror("m_producer", runtime, batchNo);
        int count = 0;
        for (Map<String, Object> row : rows)
        {
            String hisId = str(row, "producer_id");
            if (StringUtils.isEmpty(hisId))
            {
                continue;
            }
            Map<String, Object> spd = new HashMap<>(16);
            spd.put("factoryCode", MsunSpdFieldSupport.truncate(
                    MsunSpdFieldSupport.firstNonBlank(str(row, "producer_code"), hisId), 64));
            spd.put("factoryName", MsunSpdFieldSupport.truncate(str(row, "producer_cnname"), 255));
            spd.put("factoryReferredCode", MsunSpdFieldSupport.truncate(str(row, "input_code"), 64));
            spd.put("factoryAddress", MsunSpdFieldSupport.truncate(str(row, "address"), 500));
            spd.put("factoryContact", MsunSpdFieldSupport.truncate(str(row, "phone"), 128));
            spd.put("hisId", hisId);
            spd.put("tenantId", runtime.getTenantId());
            spd.put("delFlag", MsunSpdFieldSupport.toFdDelFlag(str(row, "invalid_flag")));
            spd.put("createBy", MsunSpdFieldSupport.syncBy());
            spd.put("updateBy", MsunSpdFieldSupport.syncBy());
            count += syncMapper.upsertFdFactory(spd);
        }
        return count;
    }

    private int syncCategories(MsunHospitalRuntime runtime, String batchNo)
    {
        List<Map<String, Object>> rows = listMirror("m_dict_category", runtime, batchNo);
        int count = 0;
        for (Map<String, Object> row : rows)
        {
            String hisId = str(row, "his_dict_id");
            if (StringUtils.isEmpty(hisId))
            {
                continue;
            }
            Map<String, Object> spd = new HashMap<>(12);
            spd.put("warehouseCategoryCode", MsunSpdFieldSupport.truncate(hisId, 64));
            spd.put("warehouseCategoryName", MsunSpdFieldSupport.truncate(str(row, "his_dict_name"), 255));
            spd.put("referredName", null);
            spd.put("hisId", hisId);
            spd.put("tenantId", runtime.getTenantId());
            spd.put("delFlag", 0);
            spd.put("createBy", MsunSpdFieldSupport.syncBy());
            spd.put("updateBy", MsunSpdFieldSupport.syncBy());
            count += syncMapper.upsertFdWarehouseCategory(spd);
        }
        return count;
    }

    private int syncMaterials(MsunHospitalRuntime runtime, String batchNo)
    {
        List<Map<String, Object>> rows = listMirror("m_drug_dict", runtime, batchNo);
        String tenantId = runtime.getTenantId();
        int count = 0;
        for (Map<String, Object> row : rows)
        {
            ensureUnit(tenantId, str(row, "min_packing_id"), str(row, "min_packing_name"));
            ensureUnit(tenantId, str(row, "dose_unit_id"), str(row, "dose_unit_name"));
            ensureUnit(tenantId, str(row, "min_dose_unit_id"), str(row, "dose_unit_name"));
        }
        for (Map<String, Object> row : rows)
        {
            String drugId = str(row, "drug_id");
            String specPackingId = str(row, "drug_spec_packing_id");
            if (StringUtils.isEmpty(drugId))
            {
                continue;
            }
            if (StringUtils.isEmpty(specPackingId))
            {
                specPackingId = "0";
            }
            Long supplierId = resolveSupplierId(tenantId, str(row, "supplier_id"));
            Long factoryId = resolveFactoryId(tenantId, str(row, "produce_id"));
            Long storeroomId = resolveCategoryId(tenantId, str(row, "drug_catagory_id"));
            Long unitId = resolveUnitId(tenantId, str(row, "min_packing_id"), str(row, "min_packing_name"));

            Map<String, Object> spd = new HashMap<>(24);
            spd.put("code", MsunSpdFieldSupport.truncate(
                    MsunSpdFieldSupport.firstNonBlank(str(row, "drug_code"), drugId), 50));
            spd.put("name", MsunSpdFieldSupport.truncate(str(row, "drug_name"), 100));
            spd.put("speci", MsunSpdFieldSupport.truncate(str(row, "spec"), 100));
            spd.put("model", MsunSpdFieldSupport.truncate(str(row, "model_type"), 100));
            spd.put("price", toDecimal(row, "buy_price"));
            spd.put("salePrice", toDecimal(row, "retail_price"));
            spd.put("referredName", MsunSpdFieldSupport.truncate(str(row, "input_code"), 100));
            spd.put("registerNo", MsunSpdFieldSupport.truncate(str(row, "approved_no"), 100));
            spd.put("medicalNo", MsunSpdFieldSupport.truncate(str(row, "national_medical_insurance_code"), 100));
            spd.put("hisId", drugId);
            spd.put("hisSpecPackingId", specPackingId);
            spd.put("tenantId", tenantId);
            spd.put("delFlag", MsunSpdFieldSupport.toFdDelFlag(str(row, "invalid_flag")));
            spd.put("supplierId", supplierId);
            spd.put("factoryId", factoryId);
            spd.put("storeroomId", storeroomId);
            spd.put("unitId", unitId);
            spd.put("createBy", MsunSpdFieldSupport.syncBy());
            spd.put("updateBy", MsunSpdFieldSupport.syncBy());
            count += syncMapper.upsertFdMaterial(spd);
        }
        return count;
    }

    private void ensureUnit(String tenantId, String hisUnitId, String unitName)
    {
        String resolvedId = MsunSpdFieldSupport.resolveHisUnitId(hisUnitId, unitName);
        if (StringUtils.isEmpty(resolvedId) || StringUtils.isEmpty(unitName))
        {
            return;
        }
        if (syncMapper.selectFdUnitIdByHisUnitId(tenantId, resolvedId) != null)
        {
            return;
        }
        Map<String, Object> spd = new HashMap<>(10);
        spd.put("unitCode", MsunSpdFieldSupport.truncate(resolvedId, 64));
        spd.put("unitName", MsunSpdFieldSupport.truncate(unitName, 255));
        spd.put("hisUnitId", resolvedId);
        spd.put("tenantId", tenantId);
        spd.put("delFlag", 0);
        spd.put("createBy", MsunSpdFieldSupport.syncBy());
        spd.put("updateBy", MsunSpdFieldSupport.syncBy());
        syncMapper.upsertFdUnit(spd);
    }

    private Long resolveDeptId(String tenantId, String hisDeptId)
    {
        if (StringUtils.isEmpty(hisDeptId))
        {
            return null;
        }
        return syncMapper.selectFdDepartmentIdByHisId(tenantId, hisDeptId);
    }

    private Long resolveSupplierId(String tenantId, String hisSupplierId)
    {
        if (StringUtils.isEmpty(hisSupplierId))
        {
            return null;
        }
        return syncMapper.selectFdSupplierIdByHisId(tenantId, hisSupplierId);
    }

    private Long resolveFactoryId(String tenantId, String hisFactoryId)
    {
        if (StringUtils.isEmpty(hisFactoryId))
        {
            return null;
        }
        return syncMapper.selectFdFactoryIdByHisId(tenantId, hisFactoryId);
    }

    private Long resolveCategoryId(String tenantId, String hisCategoryId)
    {
        if (StringUtils.isEmpty(hisCategoryId))
        {
            return null;
        }
        return syncMapper.selectFdWarehouseCategoryIdByHisId(tenantId, hisCategoryId);
    }

    private Long resolveUnitId(String tenantId, String hisUnitId, String unitName)
    {
        String resolvedId = MsunSpdFieldSupport.resolveHisUnitId(hisUnitId, unitName);
        if (StringUtils.isEmpty(resolvedId))
        {
            return null;
        }
        return syncMapper.selectFdUnitIdByHisUnitId(tenantId, resolvedId);
    }

    private List<Map<String, Object>> listMirror(String table, MsunHospitalRuntime runtime, String batchNo)
    {
        if (!MIRROR_TABLES.contains(table))
        {
            throw new IllegalArgumentException("非法镜像表: " + table);
        }
        return syncMapper.listMirrorRowsByBatch(
                table,
                runtime.getHospitalKey(),
                runtime.getTenantId(),
                runtime.getActiveEnv(),
                batchNo);
    }

    private static String buildLoginName(Map<String, Object> row, String identityId)
    {
        String userCode = str(row, "user_code");
        String staffCode = str(row, "staff_code");
        String base = MsunSpdFieldSupport.firstNonBlank(userCode, staffCode);
        if (StringUtils.isNotEmpty(base))
        {
            return base + "_" + identityId;
        }
        return "his_" + identityId;
    }

    private static String str(Map<String, Object> row, String key)
    {
        Object v = row.get(key);
        return v == null ? null : String.valueOf(v).trim();
    }

    private static BigDecimal toDecimal(Map<String, Object> row, String key)
    {
        String s = str(row, key);
        if (StringUtils.isEmpty(s))
        {
            return null;
        }
        try
        {
            return new BigDecimal(s);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}
