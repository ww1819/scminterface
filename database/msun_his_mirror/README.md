# 众阳云健康 HIS 接口镜像表（SPD 业务库内，手工维护）

## 定位

- **不建独立镜像库**。`m_*` 表建在 **SPD 业务库**（如 `aspt`）中，与现有业务表同库。
- **【非标准对象】众阳云健康（msun）专用**，表注释含「众阳云健康HIS镜像表」，与其他 HIS 厂家镜像表区分；**不纳入**新客户标准库自动初始化。
- **新客户**：标准库初始化完成后，若不需要接口镜像，可执行 `99_drop_mirror_tables_optional.sql` 删除全部 `m_*` 表；需要时由实施人员按接口手工执行建表脚本。

## 手工执行顺序（按需）

| 顺序 | 脚本 | 说明 |
|------|------|------|
| — | `00_create_database.sql` | 仅说明，**勿建独立库** |
| 1 | `01_table.sql` | 建表（可按需只执行部分 `CREATE TABLE`） |
| 2 | `02_column.sql` | 增量字段存储过程 |
| 3 | `04_table_drug_batch_stock.sql` | 2.5.43 批次库存（可选） |
| 4 | `03_seed_probe_sample.sql` | 样本数据（可选，手工） |
| — | `99_drop_mirror_tables_optional.sql` | 新客户清理全部镜像表（可选） |

执行前在客户端中 `USE aspt;`（或实际 SPD 库名）。

## 应用自动落库

镜像表已手工创建，且配置满足时，**探针页**与**正式 API** 调用均写入 SPD 库 `m_*` 表：

```yaml
spring.datasource.druid.spd.enabled: true
scminterface.vendor.msun.mirror.enabled: true
```

落库使用 **SPD 数据源**（`@DataSource(SPD)`），无需额外数据源配置。

表不存在时落库失败仅记日志，不影响接口返回。

## 表清单

| 表名 | 接口 | 说明 |
|------|------|------|
| `m_sync_batch` | — | 同步批次日志 |
| `m_dept` | 2.1.9 | 科室基本信息 |
| `m_dept_category_rel` | 2.1.9 | 科室分类 categoryIdList |
| `m_user_identity` | 2.1.12 | 用户身份信息 |
| `m_user_identity_account` | 2.1.12 | 账号 accountList |
| `m_drug_dict` | 2.5.44 | 药品/材料字典 |
| `m_dict_category` | 2.5.58 | SPD 分类字典 |
| `m_supplier` | 2.5.62 | SPD 供应商 |
| `m_producer` | 2.5.63 | SPD 生产厂商 |
| `m_yk_instock` | 2.5.102 | 一级库入退库主表 |
| `m_yk_instock_detail` | 2.5.102 | 入退库明细 |
| `m_drug_batch_stock` | 2.5.43 | 药房批次库存（见 04 脚本） |

## 设计说明

- 主键统一 **UUID7**，`VARCHAR(36)`（含连字符）；应用落库由 `ZsUuid7.newString()` 生成，upsert 冲突时不更新主键列。
- 每张表含镜像元数据：`hospital_key`、`tenant_id`（枣强=`zaoqiang-tcm-001`）、`active_env`、`sync_batch_no`、`raw_item_json` 等。
- 应用落库时 `tenant_id` 取自医院运行时 `MsunHospitalRuntime.getTenantId()`。
- `raw_item_json` 保留 HIS 原始行，字段不全时可从 JSON 补数。
- 后续 HIS 新增字段：在 `02_column.sql` 末尾追加 `CALL add_mirror_column(...)` 后手工执行。

## 重新生成样本落库 SQL

```bash
python gen_seed_probe_sample.py
```

生成 `03_seed_probe_sample.sql`（需在脚本内将 `USE` 改为实际 SPD 库名后执行）。
