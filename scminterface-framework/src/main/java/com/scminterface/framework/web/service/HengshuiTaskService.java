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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scminterface.common.annotation.DataSource;
import com.scminterface.common.enums.DataSourceType;
import com.scminterface.framework.web.mapper.HisHcInfoMapper;
import com.scminterface.framework.web.mapper.HisMzSfmxMapper;
import com.scminterface.framework.web.mapper.HisZySfmxMapper;
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

    @Autowired
    private SpdSystemConfigMapper spdSystemConfigMapper;

    @Autowired
    private HisHcInfoMapper hisHcInfoMapper;

    @Autowired
    private HisZySfmxMapper hisZySfmxMapper;

    @Autowired
    private HisMzSfmxMapper hisMzSfmxMapper;

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
     * 从HIS数据库的v_inpatient_consumable_charge视图读取最近3天的数据，保存到SPD数据库的his_zy_sfmx表
     * 
     * @return 同步结果
     */
    @DataSource(DataSourceType.SPD)
    public Map<String, Object> syncInpatientCharge()
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

            log.info("开始同步HIS住院收费明细数据，数据库URL: {}", url);

            // 2. 动态创建HIS数据库连接
            Class.forName(driver);
            hisConnection = DriverManager.getConnection(url, username, password);

            // 3. 查询视图v_inpatient_consumable_charge数据（只查询最近3天的数据）
            String sql = "SELECT inpatient_charge_id, patient_id, patient_name, inpatient_no, dept_code, dept_name, " +
                        "doctor_id, doctor_name, charge_item_id, item_name, spec_model, batch_no, expire_date, " +
                        "use_date, charge_date, quantity, unit_price, total_amount, charge_operator, remark " +
                        "FROM v_inpatient_consumable_charge " +
                        "WHERE charge_date >= DATEADD(day, -3, GETDATE())";
            
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
                        case "inpatient_charge_id":
                            item.put("inpatientChargeId", value);
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
                            item.put("useDate", processDateValue(value, sdf));
                            break;
                        case "charge_date":
                            item.put("chargeDate", processDateValue(value, sdf));
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

            // 5. 查询已存在的数据，过滤重复
            Set<Long> existingIds = new HashSet<>();
            try
            {
                List<Long> existingIdList = hisZySfmxMapper.selectAllInpatientChargeIds();
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
                Object chargeIdObj = item.get("inpatientChargeId");
                if (chargeIdObj != null)
                {
                    Long chargeId = null;
                    if (chargeIdObj instanceof Long)
                    {
                        chargeId = (Long) chargeIdObj;
                    }
                    else if (chargeIdObj instanceof Number)
                    {
                        chargeId = ((Number) chargeIdObj).longValue();
                    }
                    else if (chargeIdObj instanceof String)
                    {
                        try
                        {
                            chargeId = Long.parseLong(chargeIdObj.toString().trim());
                        }
                        catch (NumberFormatException e)
                        {
                            log.debug("无法解析inpatient_charge_id: {}", chargeIdObj);
                        }
                    }

                    if (chargeId != null && !existingIds.contains(chargeId))
                    {
                        newDataList.add(item);
                        existingIds.add(chargeId); // 添加到已存在集合，避免本次同步内的重复
                    }
                    else if (chargeId != null)
                    {
                        duplicateCount++;
                    }
                    else
                    {
                        duplicateCount++;
                        log.debug("跳过inpatient_charge_id为空或无效的数据");
                    }
                }
                else
                {
                    duplicateCount++;
                    log.debug("跳过inpatient_charge_id为空的数据");
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
                        hisZySfmxMapper.batchInsertOrUpdate(batch);
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

            log.info("HIS住院收费明细数据同步完成，总计: {}, 新增: {}, 重复: {}, 成功: {}, 失败: {}", 
                dataList.size(), newDataList.size(), duplicateCount, successCount, errorCount);
        }
        catch (Exception e)
        {
            log.error("同步HIS住院收费明细数据异常", e);
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
     * 同步门诊收费明细数据
     * 从HIS数据库的v_outpatient_consumable_charge视图读取最近3天的数据，保存到SPD数据库的his_mz_sfmx表
     * 
     * @return 同步结果
     */
    @DataSource(DataSourceType.SPD)
    public Map<String, Object> syncOutpatientCharge()
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

            log.info("开始同步HIS门诊收费明细数据，数据库URL: {}", url);

            // 2. 动态创建HIS数据库连接
            Class.forName(driver);
            hisConnection = DriverManager.getConnection(url, username, password);

            // 3. 查询视图v_outpatient_consumable_charge数据（只查询最近3天的数据）
            String sql = "SELECT outpatient_charge_id, patient_id, patient_name, outpatient_no, clinic_code, clinic_name, " +
                        "doctor_id, doctor_name, charge_item_id, item_name, spec_model, batch_no, expire_date, " +
                        "charge_date, quantity, unit_price, total_amount, charge_operator, payment_type, receipt_no, remark " +
                        "FROM v_outpatient_consumable_charge " +
                        "WHERE charge_date >= DATEADD(day, -3, GETDATE())";
            
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
                        case "outpatient_charge_id":
                            item.put("outpatientChargeId", value);
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
                            item.put("chargeDate", processDateValue(value, sdf));
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

            // 5. 查询已存在的数据，过滤重复
            Set<Long> existingIds = new HashSet<>();
            try
            {
                List<Long> existingIdList = hisMzSfmxMapper.selectAllOutpatientChargeIds();
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
                Object chargeIdObj = item.get("outpatientChargeId");
                if (chargeIdObj != null)
                {
                    Long chargeId = null;
                    if (chargeIdObj instanceof Long)
                    {
                        chargeId = (Long) chargeIdObj;
                    }
                    else if (chargeIdObj instanceof Number)
                    {
                        chargeId = ((Number) chargeIdObj).longValue();
                    }
                    else if (chargeIdObj instanceof String)
                    {
                        try
                        {
                            chargeId = Long.parseLong(chargeIdObj.toString().trim());
                        }
                        catch (NumberFormatException e)
                        {
                            log.debug("无法解析outpatient_charge_id: {}", chargeIdObj);
                        }
                    }

                    if (chargeId != null && !existingIds.contains(chargeId))
                    {
                        newDataList.add(item);
                        existingIds.add(chargeId); // 添加到已存在集合，避免本次同步内的重复
                    }
                    else if (chargeId != null)
                    {
                        duplicateCount++;
                    }
                    else
                    {
                        duplicateCount++;
                        log.debug("跳过outpatient_charge_id为空或无效的数据");
                    }
                }
                else
                {
                    duplicateCount++;
                    log.debug("跳过outpatient_charge_id为空的数据");
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
                        hisMzSfmxMapper.batchInsertOrUpdate(batch);
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

            log.info("HIS门诊收费明细数据同步完成，总计: {}, 新增: {}, 重复: {}, 成功: {}, 失败: {}", 
                dataList.size(), newDataList.size(), duplicateCount, successCount, errorCount);
        }
        catch (Exception e)
        {
            log.error("同步HIS门诊收费明细数据异常", e);
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
}
