package com.scminterface.framework.web.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.customer.hengsuiThird.his.mapper.HisInpatientChargeMirrorSyncMapper;
import com.scminterface.customer.hengsuiThird.his.mapper.HisOutpatientChargeMirrorSyncMapper;
import com.scminterface.customer.hengsuiThird.his.mapper.HisPatientChargeMirrorUnifiedSyncMapper;
import com.scminterface.customer.hengsuiThird.his.model.HisIdFingerprintRow;
import com.scminterface.customer.hengsuiThird.his.model.HisInpatientChargeMirrorRow;
import com.scminterface.customer.hengsuiThird.his.model.HisOutpatientChargeMirrorRow;
import com.scminterface.customer.hengsuiThird.his.model.HisPatientChargeMirrorUnifiedRow;
import com.scminterface.customer.hengsuiThird.his.HisChargeMirrorFetchSql;
import com.scminterface.customer.hengsuiThird.his.service.SpdPatientChargeInternalClient;
import com.scminterface.customer.hengsuiThird.his.support.HisChargeMirrorSyncSupport;
import com.scminterface.framework.web.mapper.HisHcInfoMapper;
import com.scminterface.framework.web.mapper.SpdSystemConfigMapper;

/**
 * 衡水定时任务服务
 * 
 * @author scminterface
 */
@Service
public class HengshuiTaskService
{
    private static final Logger log = LoggerFactory.getLogger(HengshuiTaskService.class);

    private static final int HIS_ID_QUERY_BATCH = 400;
    private static final int INSERT_BATCH_SIZE = 80;

    /** 与 SPD HisBillingTenantConstants.TENANT_HENGSHUI_THIRD 一致；可通过参数 his.charge.mirror.tenant_id 覆盖 */
    private static final String DEFAULT_CHARGE_MIRROR_TENANT_ID = "hengsui-third-001";

    private static final String CONFIG_CHARGE_MIRROR_TENANT_ID = "his.charge.mirror.tenant_id";

    private static final String SYNC_CREATE_BY = "scminterface";

    @Autowired
    private SpdSystemConfigMapper spdSystemConfigMapper;

    @Autowired
    private HisHcInfoMapper hisHcInfoMapper;

    @Autowired
    private HisInpatientChargeMirrorSyncMapper hisInpatientChargeMirrorSyncMapper;

    @Autowired
    private HisOutpatientChargeMirrorSyncMapper hisOutpatientChargeMirrorSyncMapper;

    @Autowired
    private HisPatientChargeMirrorUnifiedSyncMapper hisPatientChargeMirrorUnifiedSyncMapper;

    @Autowired
    private SpdPatientChargeInternalClient spdPatientChargeInternalClient;

    /**
     * 处理日期字段值，如果为空字符串则返回null
     * 
     * @param value 原始值
     * @param sdf 日期格式化器
     * @return 处理后的日期字符串或null
     */
    private String processDateValue(Object value, SimpleDateFormat sdf)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Date)
        {
            return sdf.format((Date) value);
        }
        String strValue = value.toString().trim();
        if (strValue.isEmpty())
        {
            return null;
        }
        return strValue;
    }

    private String resolveChargeMirrorTenantId()
    {
        String v = null;
        try
        {
            v = spdSystemConfigMapper.selectValueByKey(CONFIG_CHARGE_MIRROR_TENANT_ID);
        }
        catch (Exception e)
        {
            log.debug("读取 {} 失败: {}", CONFIG_CHARGE_MIRROR_TENANT_ID, e.getMessage());
        }
        String t = HisChargeMirrorSyncSupport.trimToNull(v);
        return t != null ? t : DEFAULT_CHARGE_MIRROR_TENANT_ID;
    }

    private static String strTrim(Object o)
    {
        if (o == null)
        {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String clipOutpatientChargeDate(String s)
    {
        if (s == null)
        {
            return null;
        }
        String t = s.trim();
        if (t.length() > 32)
        {
            return t.substring(0, 32);
        }
        return t;
    }

    /**
     * 同步收费项目数据
     * 从HIS数据库的v_charge_item视图读取数据，保存到SPD数据库的his_hc_info表
     * 
     * @return 同步结果
     */
    @DataSource(DataSourceType.SPD)
    public Map<String, Object> syncChargeItem()
    {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;
        int duplicateCount = 0;
        List<Map<String, Object>> newDataList = new ArrayList<>();
        Connection hisConnection = null;

        try
        {
            // 1. 从系统参数配置读取HIS数据库连接信息
            String driver = spdSystemConfigMapper.selectValueByKey("his.jdbc.driver");
            String url = spdSystemConfigMapper.selectValueByKey("his.jdbc.url");
            String username = spdSystemConfigMapper.selectValueByKey("his.jdbc.username");
            String password = spdSystemConfigMapper.selectValueByKey("his.jdbc.password");

            if (driver == null || url == null || username == null || password == null)
            {
                log.error("HIS数据库连接配置不完整");
                result.put("success", false);
                result.put("message", "HIS数据库连接配置不完整，请检查系统参数配置");
                return result;
            }

            log.info("开始同步HIS收费项目数据，数据库URL: {}", url);

            // 2. 动态创建HIS数据库连接
            Class.forName(driver);
            hisConnection = DriverManager.getConnection(url, username, password);

            // 3. 查询视图v_charge_item数据
            String sql = "SELECT charge_item_id, item_code, item_name, item_type, consumable_type, " +
                        "spec_model, unit, price, manufacturer, register_no, is_active, " +
                        "create_time, update_time FROM v_charge_item";
            
            PreparedStatement pstmt = hisConnection.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<Map<String, Object>> dataList = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // 4. 转换数据格式
            while (rs.next())
            {
                Map<String, Object> item = new HashMap<>();
                for (int i = 1; i <= columnCount; i++)
                {
                    String columnName = metaData.getColumnName(i).toLowerCase();
                    Object value = rs.getObject(i);

                    // 处理字段名映射
                    switch (columnName)
                    {
                        case "charge_item_id":
                            item.put("chargeItemId", value != null ? value.toString().trim() : null);
                            break;
                        case "item_code":
                            item.put("itemCode", value != null ? value.toString().trim() : null);
                            break;
                        case "item_name":
                            item.put("itemName", value != null ? value.toString().trim() : null);
                            break;
                        case "item_type":
                            item.put("itemType", value != null ? value.toString().trim() : null);
                            break;
                        case "consumable_type":
                            item.put("consumableType", value != null ? value.toString().trim() : null);
                            break;
                        case "spec_model":
                            item.put("specModel", value != null ? value.toString().trim() : null);
                            break;
                        case "unit":
                            item.put("unit", value != null ? value.toString().trim() : null);
                            break;
                        case "price":
                            item.put("price", value);
                            break;
                        case "manufacturer":
                            item.put("manufacturer", value != null ? value.toString().trim() : null);
                            break;
                        case "register_no":
                            item.put("registerNo", value != null ? value.toString().trim() : null);
                            break;
                        case "is_active":
                            item.put("isActive", value != null ? value.toString().trim() : null);
                            break;
                        case "create_time":
                            if (value instanceof Date)
                            {
                                item.put("createTime", sdf.format((Date) value));
                            }
                            else if (value != null)
                            {
                                item.put("createTime", value.toString());
                            }
                            break;
                        case "update_time":
                            if (value instanceof Date)
                            {
                                item.put("updateTime", sdf.format((Date) value));
                            }
                            else if (value != null)
                            {
                                item.put("updateTime", value.toString());
                            }
                            break;
                    }
                }
                dataList.add(item);
            }

            rs.close();
            pstmt.close();

            log.info("从HIS数据库读取到 {} 条数据", dataList.size());

            // 5. 查询已存在的数据，过滤重复
            Set<String> existingIds = new HashSet<>();
            try
            {
                List<String> existingIdList = hisHcInfoMapper.selectAllChargeItemIds();
                if (existingIdList != null && !existingIdList.isEmpty())
                {
                    existingIds.addAll(existingIdList);
                    log.info("已存在 {} 条数据，将过滤重复数据", existingIds.size());
                }
            }
            catch (Exception e)
            {
                log.warn("查询已存在数据失败，将处理所有数据: {}", e.getMessage());
            }

            // 6. 过滤掉已存在的数据
            newDataList.clear();
            duplicateCount = 0;
            for (Map<String, Object> item : dataList)
            {
                String chargeItemId = (String) item.get("chargeItemId");
                if (chargeItemId != null && !chargeItemId.trim().isEmpty())
                {
                    // 去除前后空格并检查是否已存在
                    String trimmedId = chargeItemId.trim();
                    if (!existingIds.contains(trimmedId))
                    {
                        newDataList.add(item);
                        existingIds.add(trimmedId); // 添加到已存在集合，避免本次同步内的重复
                    }
                    else
                    {
                        duplicateCount++;
                    }
                }
                else
                {
                    // charge_item_id为空的数据也跳过
                    duplicateCount++;
                    log.debug("跳过charge_item_id为空的数据");
                }
            }

            log.info("过滤后，新增数据: {} 条，重复数据: {} 条", newDataList.size(), duplicateCount);

            // 7. 批量保存新数据到SPD数据库
            if (!newDataList.isEmpty())
            {
                // 分批处理，每批1000条
                int batchSize = 1000;
                for (int i = 0; i < newDataList.size(); i += batchSize)
                {
                    int end = Math.min(i + batchSize, newDataList.size());
                    List<Map<String, Object>> batch = newDataList.subList(i, end);
                    try
                    {
                        hisHcInfoMapper.batchInsertOrUpdate(batch);
                        successCount += batch.size();
                        log.debug("成功保存 {} 条数据（批次 {}-{}）", batch.size(), i + 1, end);
                    }
                    catch (Exception e)
                    {
                        errorCount += batch.size();
                        log.error("保存数据批次失败（{}-{}）", i + 1, end, e);
                    }
                }
            }
            else
            {
                log.info("没有新数据需要保存");
            }

            result.put("success", true);
            result.put("totalCount", dataList.size());
            result.put("newCount", newDataList.size());
            result.put("duplicateCount", duplicateCount);
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            result.put("message", String.format("同步完成，总计: %d, 新增: %d, 重复: %d, 成功: %d, 失败: %d", 
                dataList.size(), newDataList.size(), duplicateCount, successCount, errorCount));

            log.info("HIS收费项目数据同步完成，总计: {}, 新增: {}, 重复: {}, 成功: {}, 失败: {}", 
                dataList.size(), newDataList.size(), duplicateCount, successCount, errorCount);
        }
        catch (Exception e)
        {
            log.error("同步HIS收费项目数据异常", e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
            errorCount++;
        }
        finally
        {
            // 关闭HIS数据库连接
            if (hisConnection != null)
            {
                try
                {
                    hisConnection.close();
                }
                catch (Exception e)
                {
                    log.warn("关闭HIS数据库连接异常", e);
                }
            }
        }

        return result;
    }

    /**
     * 同步住院收费明细数据
     * 从HIS数据库的v_inpatient_consumable_charge视图读取最近3天的数据，写入 SPD 库
     * {@code his_inpatient_charge_mirror} 与 {@code his_patient_charge_mirror_unified}（去重逻辑对齐 SPD）
     * 
     * @return 同步结果
     */
    @DataSource(DataSourceType.SPD)
    public Map<String, Object> syncInpatientCharge()
    {
        Map<String, Object> result = new HashMap<>();
        int errorCount = 0;
        Connection hisConnection = null;

        try
        {
            String driver = spdSystemConfigMapper.selectValueByKey("his.jdbc.driver");
            String url = spdSystemConfigMapper.selectValueByKey("his.jdbc.url");
            String username = spdSystemConfigMapper.selectValueByKey("his.jdbc.username");
            String password = spdSystemConfigMapper.selectValueByKey("his.jdbc.password");

            if (driver == null || url == null || username == null || password == null)
            {
                log.error("HIS数据库连接配置不完整");
                result.put("success", false);
                result.put("message", "HIS数据库连接配置不完整，请检查系统参数配置");
                return result;
            }

            String tenantId = resolveChargeMirrorTenantId();
            log.info("开始同步HIS住院收费明细数据至镜像表，数据库URL: {}，tenantId: {}", url, tenantId);

            Class.forName(driver);
            hisConnection = DriverManager.getConnection(url, username, password);

            String sql = HisChargeMirrorFetchSql.SQLSERVER_INPATIENT_RECENT_3D;

            PreparedStatement pstmt = hisConnection.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<Map<String, Object>> dataList = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            while (rs.next())
            {
                Map<String, Object> item = new HashMap<>();
                for (int i = 1; i <= columnCount; i++)
                {
                    String columnName = metaData.getColumnName(i).toLowerCase();
                    Object value = rs.getObject(i);

                    switch (columnName)
                    {
                        case "inpatient_charge_id":
                            item.put("inpatientChargeId", value);
                            break;
                        case "inpatient_charge_id_tf":
                            item.put("inpatientChargeIdTf", value);
                            break;
                        case "patient_id":
                            item.put("patientId", value);
                            break;
                        case "patient_name":
                            item.put("patientName", value != null ? value.toString().trim() : null);
                            break;
                        case "inpatient_no":
                            item.put("inpatientNo", value != null ? value.toString().trim() : null);
                            break;
                        case "dept_code":
                            item.put("deptCode", value != null ? value.toString().trim() : null);
                            break;
                        case "dept_name":
                            item.put("deptName", value != null ? value.toString().trim() : null);
                            break;
                        case "doctor_id":
                            item.put("doctorId", value != null ? value.toString().trim() : null);
                            break;
                        case "doctor_name":
                            item.put("doctorName", value != null ? value.toString().trim() : null);
                            break;
                        case "charge_item_id":
                            item.put("chargeItemId", value != null ? value.toString().trim() : null);
                            break;
                        case "item_name":
                            item.put("itemName", value != null ? value.toString().trim() : null);
                            break;
                        case "spec_model":
                            item.put("specModel", value != null ? value.toString().trim() : null);
                            break;
                        case "batch_no":
                            item.put("batchNo", value != null ? value.toString().trim() : null);
                            break;
                        case "expire_date":
                            item.put("expireDate", processDateValue(value, sdf));
                            break;
                        case "use_date":
                            item.put("useDate", value);
                            break;
                        case "charge_date":
                            item.put("chargeDate", value);
                            break;
                        case "quantity":
                            item.put("quantity", value);
                            break;
                        case "unit_price":
                            item.put("unitPrice", value);
                            break;
                        case "total_amount":
                            item.put("totalAmount", value);
                            break;
                        case "charge_operator":
                            item.put("chargeOperator", value != null ? value.toString().trim() : null);
                            break;
                        case "remark":
                            item.put("remark", value != null ? value.toString().trim() : null);
                            break;
                    }
                }
                dataList.add(item);
            }

            rs.close();
            pstmt.close();

            log.info("从HIS数据库读取到 {} 条数据（最近3天）", dataList.size());

            String fetchBatchId = UUID.randomUUID().toString();
            Date createTime = new Date();
            int[] counts = mergeInsertInpatientRows(tenantId, fetchBatchId, SYNC_CREATE_BY, createTime, dataList);
            int inserted = counts[0];
            int skipped = counts[1];
            int drift = counts[2];

            result.put("success", true);
            result.put("fetchBatchId", fetchBatchId);
            result.put("totalCount", dataList.size());
            result.put("insertedCount", inserted);
            result.put("skippedCount", skipped);
            result.put("driftCount", drift);
            result.put("newCount", inserted);
            result.put("duplicateCount", skipped + drift);
            result.put("successCount", inserted);
            result.put("errorCount", 0);
            result.put("message", String.format(
                "同步完成(镜像表)，总计: %d, 新增: %d, 指纹一致跳过: %d, 指纹不一致(已存在): %d, 批次: %s",
                dataList.size(), inserted, skipped, drift, fetchBatchId));

            log.info("HIS住院收费明细镜像同步完成，总计: {}, 新增: {}, 跳过: {}, drift: {}, batch: {}",
                dataList.size(), inserted, skipped, drift, fetchBatchId);

            triggerSpdAutoProcessAfterSync(tenantId, fetchBatchId, "INPATIENT", inserted);
        }
        catch (Exception e)
        {
            log.error("同步HIS住院收费明细数据异常", e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
            errorCount++;
            result.put("errorCount", errorCount);
        }
        finally
        {
            if (hisConnection != null)
            {
                try
                {
                    hisConnection.close();
                }
                catch (Exception e)
                {
                    log.warn("关闭HIS数据库连接异常", e);
                }
            }
        }

        return result;
    }

    /**
     * 同步门诊收费明细数据
     * 从HIS数据库的v_outpatient_consumable_charge视图读取最近3天的数据，写入 SPD 库
     * {@code his_outpatient_charge_mirror} 与 {@code his_patient_charge_mirror_unified}（去重逻辑对齐 SPD）
     * 
     * @return 同步结果
     */
    @DataSource(DataSourceType.SPD)
    public Map<String, Object> syncOutpatientCharge()
    {
        Map<String, Object> result = new HashMap<>();
        int errorCount = 0;
        Connection hisConnection = null;

        try
        {
            String driver = spdSystemConfigMapper.selectValueByKey("his.jdbc.driver");
            String url = spdSystemConfigMapper.selectValueByKey("his.jdbc.url");
            String username = spdSystemConfigMapper.selectValueByKey("his.jdbc.username");
            String password = spdSystemConfigMapper.selectValueByKey("his.jdbc.password");

            if (driver == null || url == null || username == null || password == null)
            {
                log.error("HIS数据库连接配置不完整");
                result.put("success", false);
                result.put("message", "HIS数据库连接配置不完整，请检查系统参数配置");
                return result;
            }

            String tenantId = resolveChargeMirrorTenantId();
            log.info("开始同步HIS门诊收费明细数据至镜像表，数据库URL: {}，tenantId: {}", url, tenantId);

            Class.forName(driver);
            hisConnection = DriverManager.getConnection(url, username, password);

            String sql = HisChargeMirrorFetchSql.SQLSERVER_OUTPATIENT_RECENT_3D;

            PreparedStatement pstmt = hisConnection.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<Map<String, Object>> dataList = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            while (rs.next())
            {
                Map<String, Object> item = new HashMap<>();
                for (int i = 1; i <= columnCount; i++)
                {
                    String columnName = metaData.getColumnName(i).toLowerCase();
                    Object value = rs.getObject(i);

                    switch (columnName)
                    {
                        case "outpatient_charge_id":
                            item.put("outpatientChargeId", value);
                            break;
                        case "outpatient_charge_id_tf":
                            item.put("outpatientChargeIdTf", value);
                            break;
                        case "patient_id":
                            item.put("patientId", value);
                            break;
                        case "patient_name":
                            item.put("patientName", value != null ? value.toString().trim() : null);
                            break;
                        case "outpatient_no":
                            item.put("outpatientNo", value != null ? value.toString().trim() : null);
                            break;
                        case "clinic_code":
                            item.put("clinicCode", value != null ? value.toString().trim() : null);
                            break;
                        case "clinic_name":
                            item.put("clinicName", value != null ? value.toString().trim() : null);
                            break;
                        case "doctor_id":
                            item.put("doctorId", value != null ? value.toString().trim() : null);
                            break;
                        case "doctor_name":
                            item.put("doctorName", value != null ? value.toString().trim() : null);
                            break;
                        case "charge_item_id":
                            item.put("chargeItemId", value != null ? value.toString().trim() : null);
                            break;
                        case "item_name":
                            item.put("itemName", value != null ? value.toString().trim() : null);
                            break;
                        case "spec_model":
                            item.put("specModel", value != null ? value.toString().trim() : null);
                            break;
                        case "batch_no":
                            item.put("batchNo", value != null ? value.toString().trim() : null);
                            break;
                        case "expire_date":
                            item.put("expireDate", processDateValue(value, sdf));
                            break;
                        case "charge_date":
                            item.put("chargeDate", value);
                            break;
                        case "quantity":
                            item.put("quantity", value);
                            break;
                        case "unit_price":
                            item.put("unitPrice", value);
                            break;
                        case "total_amount":
                            item.put("totalAmount", value);
                            break;
                        case "charge_operator":
                            item.put("chargeOperator", value != null ? value.toString().trim() : null);
                            break;
                        case "payment_type":
                            item.put("paymentType", value != null ? value.toString().trim() : null);
                            break;
                        case "receipt_no":
                            item.put("receiptNo", value != null ? value.toString().trim() : null);
                            break;
                        case "remark":
                            item.put("remark", value != null ? value.toString().trim() : null);
                            break;
                    }
                }
                dataList.add(item);
            }

            rs.close();
            pstmt.close();

            log.info("从HIS数据库读取到 {} 条数据（最近3天）", dataList.size());

            String fetchBatchId = UUID.randomUUID().toString();
            Date createTime = new Date();
            int[] counts = mergeInsertOutpatientRows(tenantId, fetchBatchId, SYNC_CREATE_BY, createTime, dataList);
            int inserted = counts[0];
            int skipped = counts[1];
            int drift = counts[2];

            result.put("success", true);
            result.put("fetchBatchId", fetchBatchId);
            result.put("totalCount", dataList.size());
            result.put("insertedCount", inserted);
            result.put("skippedCount", skipped);
            result.put("driftCount", drift);
            result.put("newCount", inserted);
            result.put("duplicateCount", skipped + drift);
            result.put("successCount", inserted);
            result.put("errorCount", 0);
            result.put("message", String.format(
                "同步完成(镜像表)，总计: %d, 新增: %d, 指纹一致跳过: %d, 指纹不一致(已存在): %d, 批次: %s",
                dataList.size(), inserted, skipped, drift, fetchBatchId));

            log.info("HIS门诊收费明细镜像同步完成，总计: {}, 新增: {}, 跳过: {}, drift: {}, batch: {}",
                dataList.size(), inserted, skipped, drift, fetchBatchId);

            triggerSpdAutoProcessAfterSync(tenantId, fetchBatchId, "OUTPATIENT", inserted);
        }
        catch (Exception e)
        {
            log.error("同步HIS门诊收费明细数据异常", e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
            errorCount++;
            result.put("errorCount", errorCount);
        }
        finally
        {
            if (hisConnection != null)
            {
                try
                {
                    hisConnection.close();
                }
                catch (Exception e)
                {
                    log.warn("关闭HIS数据库连接异常", e);
                }
            }
        }

        return result;
    }

    private HisInpatientChargeMirrorRow mapToInpatientMirrorRow(
        Map<String, Object> m, String tenantId, String fetchBatchId, String createBy, Date createTime)
    {
        String hid = HisChargeMirrorSyncSupport.toHisIdString(m.get("inpatientChargeId"));
        if (HisChargeMirrorSyncSupport.isBlank(hid))
        {
            return null;
        }
        HisInpatientChargeMirrorRow r = new HisInpatientChargeMirrorRow();
        r.setTenantId(tenantId);
        r.setFetchBatchId(fetchBatchId);
        r.setHisInpatientChargeId(hid);
        r.setHisInpatientChargeIdTf(HisChargeMirrorSyncSupport.toHisIdString(m.get("inpatientChargeIdTf")));
        r.setPatientId(HisChargeMirrorSyncSupport.toHisIdString(m.get("patientId")));
        r.setPatientName(strTrim(m.get("patientName")));
        r.setInpatientNo(strTrim(m.get("inpatientNo")));
        r.setDeptCode(HisChargeMirrorSyncSupport.trimToNull(strTrim(m.get("deptCode"))));
        r.setDeptName(strTrim(m.get("deptName")));
        r.setDoctorId(HisChargeMirrorSyncSupport.trimToNull(strTrim(m.get("doctorId"))));
        r.setDoctorName(strTrim(m.get("doctorName")));
        r.setChargeItemId(HisChargeMirrorSyncSupport.trimToNull(strTrim(m.get("chargeItemId"))));
        r.setItemName(strTrim(m.get("itemName")));
        r.setSpecModel(strTrim(m.get("specModel")));
        r.setBatchNo(strTrim(m.get("batchNo")));
        r.setExpireDate(strTrim(m.get("expireDate")));
        r.setUseDate(HisChargeMirrorSyncSupport.parseHisDateTime(m.get("useDate")));
        r.setChargeDate(HisChargeMirrorSyncSupport.parseHisDateTime(m.get("chargeDate")));
        r.setQuantity(HisChargeMirrorSyncSupport.toBigDecimal(m.get("quantity")));
        r.setUnitPrice(HisChargeMirrorSyncSupport.toBigDecimal(m.get("unitPrice")));
        r.setTotalAmount(HisChargeMirrorSyncSupport.toBigDecimal(m.get("totalAmount")));
        r.setChargeOperator(strTrim(m.get("chargeOperator")));
        r.setRemark(strTrim(m.get("remark")));
        r.setProcessStatus("PENDING_CONSUME");
        r.setCreateBy(createBy);
        r.setCreateTime(createTime);
        r.setRowFingerprint(HisChargeMirrorSyncSupport.fingerprintInpatient(r));
        return r;
    }

    private HisOutpatientChargeMirrorRow mapToOutpatientMirrorRow(
        Map<String, Object> m, String tenantId, String fetchBatchId, String createBy, Date createTime)
    {
        String hid = HisChargeMirrorSyncSupport.toHisIdString(m.get("outpatientChargeId"));
        if (HisChargeMirrorSyncSupport.isBlank(hid))
        {
            return null;
        }
        HisOutpatientChargeMirrorRow r = new HisOutpatientChargeMirrorRow();
        r.setTenantId(tenantId);
        r.setFetchBatchId(fetchBatchId);
        r.setHisOutpatientChargeId(hid);
        r.setHisOutpatientChargeIdTf(HisChargeMirrorSyncSupport.toHisIdString(m.get("outpatientChargeIdTf")));
        r.setPatientId(HisChargeMirrorSyncSupport.toHisIdString(m.get("patientId")));
        r.setPatientName(strTrim(m.get("patientName")));
        r.setOutpatientNo(strTrim(m.get("outpatientNo")));
        r.setClinicCode(HisChargeMirrorSyncSupport.trimToNull(strTrim(m.get("clinicCode"))));
        r.setClinicName(strTrim(m.get("clinicName")));
        r.setDoctorId(HisChargeMirrorSyncSupport.trimToNull(strTrim(m.get("doctorId"))));
        r.setDoctorName(strTrim(m.get("doctorName")));
        r.setChargeItemId(HisChargeMirrorSyncSupport.trimToNull(strTrim(m.get("chargeItemId"))));
        r.setItemName(strTrim(m.get("itemName")));
        r.setSpecModel(strTrim(m.get("specModel")));
        r.setBatchNo(strTrim(m.get("batchNo")));
        r.setExpireDate(strTrim(m.get("expireDate")));
        Object rawCharge = m.get("chargeDate");
        Date chargeAt = HisChargeMirrorSyncSupport.parseHisDateTime(rawCharge);
        String chargeDisp = chargeAt != null
            ? clipOutpatientChargeDate(HisChargeMirrorSyncSupport.formatChargeDateDisplay(chargeAt))
            : clipOutpatientChargeDate(rawCharge == null ? null : String.valueOf(rawCharge).trim());
        r.setChargeDate(chargeDisp);
        r.setQuantity(HisChargeMirrorSyncSupport.toBigDecimal(m.get("quantity")));
        r.setUnitPrice(HisChargeMirrorSyncSupport.toBigDecimal(m.get("unitPrice")));
        r.setTotalAmount(HisChargeMirrorSyncSupport.toBigDecimal(m.get("totalAmount")));
        r.setChargeOperator(strTrim(m.get("chargeOperator")));
        r.setPaymentType(strTrim(m.get("paymentType")));
        r.setReceiptNo(HisChargeMirrorSyncSupport.trimToNull(strTrim(m.get("receiptNo"))));
        r.setRemark(strTrim(m.get("remark")));
        r.setProcessStatus("PENDING_CONSUME");
        r.setCreateBy(createBy);
        r.setCreateTime(createTime);
        r.setRowFingerprint(HisChargeMirrorSyncSupport.fingerprintOutpatient(r));
        return r;
    }

    private int[] mergeInsertInpatientRows(
        String tenantId, String fetchBatchId, String createBy, Date createTime, List<Map<String, Object>> dataList)
    {
        List<HisInpatientChargeMirrorRow> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> m : dataList)
        {
            HisInpatientChargeMirrorRow r = mapToInpatientMirrorRow(m, tenantId, fetchBatchId, createBy, createTime);
            if (r == null)
            {
                continue;
            }
            if (!seen.add(r.getHisInpatientChargeId()))
            {
                continue;
            }
            candidates.add(r);
        }
        if (candidates.isEmpty())
        {
            return new int[] { 0, 0, 0 };
        }
        List<String> ids = new ArrayList<>(candidates.size());
        for (HisInpatientChargeMirrorRow c : candidates)
        {
            ids.add(c.getHisInpatientChargeId());
        }
        Map<String, String> existing = loadInpatientFingerprints(tenantId, ids);
        List<HisInpatientChargeMirrorRow> toInsert = new ArrayList<>();
        int skipped = 0;
        int drift = 0;
        for (HisInpatientChargeMirrorRow r : candidates)
        {
            String hid = r.getHisInpatientChargeId();
            String fp = r.getRowFingerprint();
            String old = existing.get(hid);
            if (old == null)
            {
                r.setId(UUID.randomUUID().toString());
                toInsert.add(r);
                existing.put(hid, fp);
            }
            else if (HisChargeMirrorSyncSupport.fingerprintEquals(old, fp))
            {
                skipped++;
            }
            else
            {
                drift++;
            }
        }
        insertInpatientBatches(toInsert);
        return new int[] { toInsert.size(), skipped, drift };
    }

    private int[] mergeInsertOutpatientRows(
        String tenantId, String fetchBatchId, String createBy, Date createTime, List<Map<String, Object>> dataList)
    {
        List<HisOutpatientChargeMirrorRow> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> m : dataList)
        {
            HisOutpatientChargeMirrorRow r = mapToOutpatientMirrorRow(m, tenantId, fetchBatchId, createBy, createTime);
            if (r == null)
            {
                continue;
            }
            if (!seen.add(r.getHisOutpatientChargeId()))
            {
                continue;
            }
            candidates.add(r);
        }
        if (candidates.isEmpty())
        {
            return new int[] { 0, 0, 0 };
        }
        List<String> ids = new ArrayList<>(candidates.size());
        for (HisOutpatientChargeMirrorRow c : candidates)
        {
            ids.add(c.getHisOutpatientChargeId());
        }
        Map<String, String> existing = loadOutpatientFingerprints(tenantId, ids);
        List<HisOutpatientChargeMirrorRow> toInsert = new ArrayList<>();
        int skipped = 0;
        int drift = 0;
        for (HisOutpatientChargeMirrorRow r : candidates)
        {
            String hid = r.getHisOutpatientChargeId();
            String fp = r.getRowFingerprint();
            String old = existing.get(hid);
            if (old == null)
            {
                r.setId(UUID.randomUUID().toString());
                toInsert.add(r);
                existing.put(hid, fp);
            }
            else if (HisChargeMirrorSyncSupport.fingerprintEquals(old, fp))
            {
                skipped++;
            }
            else
            {
                drift++;
            }
        }
        insertOutpatientBatches(toInsert);
        return new int[] { toInsert.size(), skipped, drift };
    }

    private Map<String, String> loadInpatientFingerprints(String tenantId, List<String> ids)
    {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < ids.size(); i += HIS_ID_QUERY_BATCH)
        {
            int end = Math.min(i + HIS_ID_QUERY_BATCH, ids.size());
            List<String> sub = ids.subList(i, end);
            List<HisIdFingerprintRow> rows = hisInpatientChargeMirrorSyncMapper.selectFingerprintsByHisIds(tenantId, sub);
            if (rows != null)
            {
                for (HisIdFingerprintRow row : rows)
                {
                    if (row != null && row.getHisChargeId() != null)
                    {
                        map.put(row.getHisChargeId(), row.getRowFingerprint());
                    }
                }
            }
        }
        return map;
    }

    private Map<String, String> loadOutpatientFingerprints(String tenantId, List<String> ids)
    {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < ids.size(); i += HIS_ID_QUERY_BATCH)
        {
            int end = Math.min(i + HIS_ID_QUERY_BATCH, ids.size());
            List<String> sub = ids.subList(i, end);
            List<HisIdFingerprintRow> rows = hisOutpatientChargeMirrorSyncMapper.selectFingerprintsByHisIds(tenantId, sub);
            if (rows != null)
            {
                for (HisIdFingerprintRow row : rows)
                {
                    if (row != null && row.getHisChargeId() != null)
                    {
                        map.put(row.getHisChargeId(), row.getRowFingerprint());
                    }
                }
            }
        }
        return map;
    }

    private void insertInpatientBatches(List<HisInpatientChargeMirrorRow> rows)
    {
        if (rows == null || rows.isEmpty())
        {
            return;
        }
        for (int i = 0; i < rows.size(); i += INSERT_BATCH_SIZE)
        {
            int end = Math.min(i + INSERT_BATCH_SIZE, rows.size());
            List<HisInpatientChargeMirrorRow> slice = new ArrayList<>(rows.subList(i, end));
            hisInpatientChargeMirrorSyncMapper.insertBatch(slice);
            List<HisPatientChargeMirrorUnifiedRow> unified = new ArrayList<>(slice.size());
            for (HisInpatientChargeMirrorRow e : slice)
            {
                unified.add(HisChargeMirrorSyncSupport.unifiedFromInpatient(e));
            }
            if (!unified.isEmpty())
            {
                hisPatientChargeMirrorUnifiedSyncMapper.insertBatch(unified);
            }
        }
    }

    private void insertOutpatientBatches(List<HisOutpatientChargeMirrorRow> rows)
    {
        if (rows == null || rows.isEmpty())
        {
            return;
        }
        for (int i = 0; i < rows.size(); i += INSERT_BATCH_SIZE)
        {
            int end = Math.min(i + INSERT_BATCH_SIZE, rows.size());
            List<HisOutpatientChargeMirrorRow> slice = new ArrayList<>(rows.subList(i, end));
            hisOutpatientChargeMirrorSyncMapper.insertBatch(slice);
            List<HisPatientChargeMirrorUnifiedRow> unified = new ArrayList<>(slice.size());
            for (HisOutpatientChargeMirrorRow e : slice)
            {
                unified.add(HisChargeMirrorSyncSupport.unifiedFromOutpatient(e));
            }
            if (!unified.isEmpty())
            {
                hisPatientChargeMirrorUnifiedSyncMapper.insertBatch(unified);
            }
        }
    }

    /**
     * 本批次有新增镜像行时，委托 SPD 执行自动低值消耗/退费（开关读 sb_tenant_setting，与 SPD 一致）。
     */
    private void triggerSpdAutoProcessAfterSync(String tenantId, String fetchBatchId, String visitKind, int inserted)
    {
        if (inserted <= 0 || fetchBatchId == null)
        {
            return;
        }
        try
        {
            spdPatientChargeInternalClient.processFetchBatchAfterSync(tenantId, fetchBatchId, visitKind);
        }
        catch (Exception e)
        {
            log.warn("触发 SPD 计费自动处理失败 batch={} visitKind={} err={}", fetchBatchId, visitKind, e.toString());
        }
    }
}
