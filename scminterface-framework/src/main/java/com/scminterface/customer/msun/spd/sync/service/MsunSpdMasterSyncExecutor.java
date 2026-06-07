package com.scminterface.customer.msun.spd.sync.service;

import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.common.utils.StringUtils;
import com.scminterface.customer.msun.hospital.MsunHospitalRuntime;
import com.scminterface.customer.msun.mirror.support.MsunHisMirrorTableNames;
import com.scminterface.customer.msun.spd.sync.mapper.MsunSpdMasterSyncMapper;
import com.scminterface.customer.msun.spd.sync.support.MsunSpdFieldSupport;
import com.scminterface.customer.msun.spd.sync.support.MsunSpdMasterSyncConstants;
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
        MIRROR_TABLES.add(MsunHisMirrorTableNames.DEPT);
        MIRROR_TABLES.add(MsunHisMirrorTableNames.USER_IDENTITY);
        MIRROR_TABLES.add(MsunHisMirrorTableNames.SUPPLIER);
        MIRROR_TABLES.add(MsunHisMirrorTableNames.PRODUCER);
        MIRROR_TABLES.add(MsunHisMirrorTableNames.DICT_CATEGORY);
        MIRROR_TABLES.add(MsunHisMirrorTableNames.DRUG_DICT);
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
        List<Map<String, Object>> rows = listMirror(MsunHisMirrorTableNames.DEPT, runtime, batchNo);
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
        List<Map<String, Object>> rows = listMirror(MsunHisMirrorTableNames.USER_IDENTITY, runtime, batchNo);
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
        List<Map<String, Object>> rows = listMirror(MsunHisMirrorTableNames.SUPPLIER, runtime, batchNo);
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
        List<Map<String, Object>> rows = listMirror(MsunHisMirrorTableNames.PRODUCER, runtime, batchNo);
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
        List<Map<String, Object>> rows = listMirror(MsunHisMirrorTableNames.DICT_CATEGORY, runtime, batchNo);
        String tenantId = runtime.getTenantId();
        int count = 0;
        for (Map<String, Object> row : rows)
        {
            String hisId = str(row, "his_dict_id");
            if (StringUtils.isEmpty(hisId) || !MsunSpdMasterSyncConstants.isAllowedMaterialCategory(hisId))
            {
                continue;
            }
            Map<String, Object> spd = new HashMap<>(12);
            spd.put("warehouseCategoryCode", MsunSpdFieldSupport.truncate(hisId, 64));
            spd.put("warehouseCategoryName", MsunSpdFieldSupport.truncate(str(row, "his_dict_name"), 255));
            spd.put("referredName", null);
            spd.put("hisId", hisId);
            spd.put("tenantId", tenantId);
            spd.put("delFlag", 0);
            spd.put("createBy", MsunSpdFieldSupport.syncBy());
            spd.put("updateBy", MsunSpdFieldSupport.syncBy());
            count += syncMapper.upsertFdWarehouseCategory(spd);
        }
        syncMapper.purgeFdWarehouseCategoryOutsideHisIds(
                tenantId, MsunSpdMasterSyncConstants.ALLOWED_MATERIAL_CATEGORY_HIS_IDS);
        return count;
    }

    /**
     * 2.5.44 材料字典 → fd_material。
     * 供应商/生产厂家/库房分类/单位：优先用 SPD 已有主数据（含独立镜像批次同步结果）；
     * 若 SPD 中不存在，则以本行字典字段补全（不依赖供应商/厂商/分类镜像表是否已拉取）。
     */
    private int syncMaterials(MsunHospitalRuntime runtime, String batchNo)
    {
        List<Map<String, Object>> rows = listMirror(MsunHisMirrorTableNames.DRUG_DICT, runtime, batchNo);
        String tenantId = runtime.getTenantId();
        int count = 0;
        for (Map<String, Object> row : rows)
        {
            if (!isAllowedMaterialDictRow(row))
            {
                continue;
            }
            ensureUnitFromDict(tenantId, row);
            ensureSupplierFromDict(tenantId, row);
            ensureFactoryFromDict(tenantId, row);
            ensureWarehouseCategoryFromDict(tenantId, row);
        }
        for (Map<String, Object> row : rows)
        {
            if (!isAllowedMaterialDictRow(row))
            {
                continue;
            }
            String drugId = str(row, "drug_id");
            // drug_spec_packing_id = 众阳HIS产品档案唯一键 → fd_material.his_spec_packing_id
            String specPackingId = str(row, "drug_spec_packing_id");
            if (StringUtils.isEmpty(specPackingId))
            {
                continue;
            }
            Long supplierId = resolveOrEnsureSupplierId(tenantId, row);
            Long factoryId = resolveOrEnsureFactoryId(tenantId, row);
            Long storeroomId = resolveOrEnsureWarehouseCategoryId(tenantId, row);
            Long unitId = resolveOrEnsureUnitId(tenantId, row);

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
            // his_spec_packing_id：众阳HIS产品档案唯一键（drug_spec_packing_id）
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
        syncMapper.purgeFdMaterialOutsideCategoryHisIds(
                tenantId, MsunSpdMasterSyncConstants.ALLOWED_MATERIAL_CATEGORY_HIS_IDS);
        return count;
    }

    /** 2.5.44：仅材料行且分类在白名单内 */
    private static boolean isAllowedMaterialDictRow(Map<String, Object> row)
    {
        if (!isMaterialRow(row))
        {
            return false;
        }
        return MsunSpdMasterSyncConstants.isAllowedMaterialCategory(str(row, "drug_catagory_id"));
    }

    /** 字典最小包装单位 → fd_unit */
    private void ensureUnitFromDict(String tenantId, Map<String, Object> row)
    {
        String hisUnitId = str(row, "min_packing_id");
        String unitName = str(row, "min_packing_name");
        upsertUnitIfAbsent(tenantId, hisUnitId, unitName);
    }

    private Long resolveOrEnsureUnitId(String tenantId, Map<String, Object> row)
    {
        String hisUnitId = str(row, "min_packing_id");
        String unitName = str(row, "min_packing_name");
        upsertUnitIfAbsent(tenantId, hisUnitId, unitName);
        return resolveUnitId(tenantId, hisUnitId, unitName);
    }

    private void upsertUnitIfAbsent(String tenantId, String hisUnitId, String unitName)
    {
        String resolvedId = MsunSpdFieldSupport.resolveHisUnitId(hisUnitId, unitName);
        if (StringUtils.isEmpty(resolvedId))
        {
            return;
        }
        if (syncMapper.selectFdUnitIdByHisUnitId(tenantId, resolvedId) != null)
        {
            return;
        }
        String displayName = MsunSpdFieldSupport.firstNonBlank(unitName, hisUnitId, resolvedId);
        Map<String, Object> spd = new HashMap<>(10);
        spd.put("unitCode", MsunSpdFieldSupport.truncate(resolvedId, 64));
        spd.put("unitName", MsunSpdFieldSupport.truncate(displayName, 255));
        spd.put("hisUnitId", resolvedId);
        spd.put("tenantId", tenantId);
        spd.put("delFlag", 0);
        spd.put("createBy", MsunSpdFieldSupport.syncBy());
        spd.put("updateBy", MsunSpdFieldSupport.syncBy());
        syncMapper.upsertFdUnit(spd);
    }

    private void ensureSupplierFromDict(String tenantId, Map<String, Object> row)
    {
        upsertSupplierFromDictIfAbsent(tenantId, row);
    }

    private Long resolveOrEnsureSupplierId(String tenantId, Map<String, Object> row)
    {
        String hisSupplierId = str(row, "supplier_id");
        if (StringUtils.isEmpty(hisSupplierId))
        {
            return null;
        }
        Long id = syncMapper.selectFdSupplierIdByHisId(tenantId, hisSupplierId);
        if (id != null)
        {
            return id;
        }
        upsertSupplierFromDictIfAbsent(tenantId, row);
        return syncMapper.selectFdSupplierIdByHisId(tenantId, hisSupplierId);
    }

    private void upsertSupplierFromDictIfAbsent(String tenantId, Map<String, Object> row)
    {
        String hisSupplierId = str(row, "supplier_id");
        if (StringUtils.isEmpty(hisSupplierId))
        {
            return;
        }
        if (syncMapper.selectFdSupplierIdByHisId(tenantId, hisSupplierId) != null)
        {
            return;
        }
        String supplierName = MsunSpdFieldSupport.firstNonBlank(str(row, "supplier_name"), hisSupplierId);
        Map<String, Object> spd = new HashMap<>(12);
        spd.put("code", MsunSpdFieldSupport.truncate(hisSupplierId, 64));
        spd.put("name", MsunSpdFieldSupport.truncate(supplierName, 255));
        spd.put("referredCode", null);
        spd.put("hisId", hisSupplierId);
        spd.put("tenantId", tenantId);
        spd.put("delFlag", 0);
        spd.put("address", null);
        spd.put("phone", null);
        spd.put("contacts", null);
        spd.put("contactsPhone", null);
        spd.put("taxNumber", null);
        spd.put("createBy", MsunSpdFieldSupport.syncBy());
        spd.put("updateBy", MsunSpdFieldSupport.syncBy());
        syncMapper.upsertFdSupplier(spd);
    }

    private void ensureFactoryFromDict(String tenantId, Map<String, Object> row)
    {
        upsertFactoryFromDictIfAbsent(tenantId, row);
    }

    private Long resolveOrEnsureFactoryId(String tenantId, Map<String, Object> row)
    {
        String hisFactoryId = str(row, "produce_id");
        if (MsunSpdFieldSupport.isPlaceholderHisId(hisFactoryId))
        {
            return null;
        }
        Long id = syncMapper.selectFdFactoryIdByHisId(tenantId, hisFactoryId);
        if (id != null)
        {
            return id;
        }
        upsertFactoryFromDictIfAbsent(tenantId, row);
        return syncMapper.selectFdFactoryIdByHisId(tenantId, hisFactoryId);
    }

    private void upsertFactoryFromDictIfAbsent(String tenantId, Map<String, Object> row)
    {
        String hisFactoryId = str(row, "produce_id");
        if (MsunSpdFieldSupport.isPlaceholderHisId(hisFactoryId))
        {
            return;
        }
        if (syncMapper.selectFdFactoryIdByHisId(tenantId, hisFactoryId) != null)
        {
            return;
        }
        String factoryName = MsunSpdFieldSupport.firstNonBlank(str(row, "produce_name"), hisFactoryId);
        Map<String, Object> spd = new HashMap<>(12);
        spd.put("factoryCode", MsunSpdFieldSupport.truncate(hisFactoryId, 64));
        spd.put("factoryName", MsunSpdFieldSupport.truncate(factoryName, 255));
        spd.put("factoryReferredCode", null);
        spd.put("factoryAddress", null);
        spd.put("factoryContact", null);
        spd.put("hisId", hisFactoryId);
        spd.put("tenantId", tenantId);
        spd.put("delFlag", 0);
        spd.put("createBy", MsunSpdFieldSupport.syncBy());
        spd.put("updateBy", MsunSpdFieldSupport.syncBy());
        syncMapper.upsertFdFactory(spd);
    }

    private void ensureWarehouseCategoryFromDict(String tenantId, Map<String, Object> row)
    {
        upsertWarehouseCategoryFromDictIfAbsent(tenantId, row);
    }

    private Long resolveOrEnsureWarehouseCategoryId(String tenantId, Map<String, Object> row)
    {
        String hisCategoryId = str(row, "drug_catagory_id");
        if (StringUtils.isEmpty(hisCategoryId))
        {
            return null;
        }
        Long id = syncMapper.selectFdWarehouseCategoryIdByHisId(tenantId, hisCategoryId);
        if (id != null)
        {
            return id;
        }
        upsertWarehouseCategoryFromDictIfAbsent(tenantId, row);
        return syncMapper.selectFdWarehouseCategoryIdByHisId(tenantId, hisCategoryId);
    }

    /** 字典分类 → SPD 库房分类 fd_warehouse_category */
    private void upsertWarehouseCategoryFromDictIfAbsent(String tenantId, Map<String, Object> row)
    {
        String hisCategoryId = str(row, "drug_catagory_id");
        if (StringUtils.isEmpty(hisCategoryId)
                || !MsunSpdMasterSyncConstants.isAllowedMaterialCategory(hisCategoryId))
        {
            return;
        }
        if (syncMapper.selectFdWarehouseCategoryIdByHisId(tenantId, hisCategoryId) != null)
        {
            return;
        }
        String categoryName = MsunSpdFieldSupport.firstNonBlank(str(row, "drug_catagory_name"), hisCategoryId);
        Map<String, Object> spd = new HashMap<>(10);
        spd.put("warehouseCategoryCode", MsunSpdFieldSupport.truncate(hisCategoryId, 64));
        spd.put("warehouseCategoryName", MsunSpdFieldSupport.truncate(categoryName, 255));
        spd.put("referredName", null);
        spd.put("hisId", hisCategoryId);
        spd.put("tenantId", tenantId);
        spd.put("delFlag", 0);
        spd.put("createBy", MsunSpdFieldSupport.syncBy());
        spd.put("updateBy", MsunSpdFieldSupport.syncBy());
        syncMapper.upsertFdWarehouseCategory(spd);
    }

    private Long resolveDeptId(String tenantId, String hisDeptId)
    {
        if (StringUtils.isEmpty(hisDeptId))
        {
            return null;
        }
        return syncMapper.selectFdDepartmentIdByHisId(tenantId, hisDeptId);
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

    /**
     * 仅同步材料：material_or_drug=1；回参未带时从 request_params_json.materialOrDrug=1 推断。
     */
    private static boolean isMaterialRow(Map<String, Object> row)
    {
        String flag = str(row, "material_or_drug");
        if ("1".equals(flag))
        {
            return true;
        }
        if ("0".equals(flag))
        {
            return false;
        }
        return MsunSpdFieldSupport.inferMaterialFromDrugDictRequest(str(row, "request_params_json"));
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
