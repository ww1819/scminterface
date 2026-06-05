# 众阳 HIS 接口镜像库（msun_his_mirror）

## 库名建议

在 MySQL 中创建独立库：

```sql
msun_his_mirror
```

中文说明：**众阳 HIS 接口镜像库** — 仅存联调/同步镜像数据，与 SCM、SPD 正式业务库隔离。

## 手工执行顺序

| 顺序 | 脚本 | 说明 |
|------|------|------|
| 1 | `00_create_database.sql` | 建库 |
| 2 | `01_table.sql` | 建表（11 张业务/关联表 + 批次表） |
| 3 | `02_column.sql` | 增量字段存储过程（后续扩字段用） |
| 4 | `03_seed_probe_sample.sql` | 探针样本落库（可选） |

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
| `m_yk_instock_detail` | 2.5.102 | 入退库明细 stockDetailList |

## 设计说明

- 每张业务表含镜像元数据：`hospital_key`、`active_env`、`sync_batch_no`、`his_trace_id`、`request_params_json`、`raw_item_json`、`mirror_time`。
- `raw_item_json` 保留 HIS 原始行 JSON，字段不全时可从 JSON 补数。
- 后续 HIS 新增字段：在 `02_column.sql` 末尾追加 `CALL add_mirror_column(...)` 后手工执行。

## 重新生成样本落库 SQL

若回参 txt 更新，可在本目录执行：

```bash
python gen_seed_probe_sample.py
```

生成 `03_seed_probe_sample.sql`（实施人员核对后手工执行）。
