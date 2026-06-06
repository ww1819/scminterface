# 众阳 HIS 镜像表说明（索引文档）

镜像表建在 **SPD 业务库**（与业务表同库），命名 `m_msun_*`。

## DDL 唯一来源（应用 auto-schema）

建表与增量补列由 scminterface **运行时自动执行**，脚本位置：

```
scminterface-framework/src/main/resources/sql/mysql/msun_his_mirror/
  ├── 01_table.sql    # CREATE TABLE IF NOT EXISTS（按接口按需创建）
  ├── 02_column.sql   # add_mirror_column 增量字段
  └── 99_drop_mirror_tables_optional.sql  # 可选：清理全部镜像表
```

配置：`scminterface.vendor.msun.mirror.auto-schema=true`（默认开启）。首次探针/查询/落库时自动建表补列。

`auto-schema=false` 时须由 DBA 手工执行上述 `01_table.sql`、`02_column.sql`（路径同上 classpath）。

## SPD 业务表字段（非镜像表）

众阳对接在 **SPD 业务表**上的对照列、推送状态、组合唯一键，统一维护在：

```
spd/spd-admin/src/main/resources/sql/mysql/material/column.sql
```

区块：

- `众阳 HIS 镜像 → SPD 主数据同步`：`his_spec_packing_id`、`his_unit_id`、`his_identity_id` 及 UPSERT 唯一键
- `众阳 HIS 单据推送`：`stk_io_bill` / `stk_io_bill_entry` / `stk_dep_inventory` / `fd_warehouse.his_id` 等

**勿**在 scminterface 仓库单独维护 SPD 业务表 DDL。

## 表清单

| 表名 | 接口 |
|------|------|
| `m_msun_sync_batch` | — |
| `m_msun_dept` / `m_msun_dept_category_rel` | 2.1.9 |
| `m_msun_user_identity` / `m_msun_user_identity_account` | 2.1.12 |
| `m_msun_drug_dict` | 2.5.44 |
| `m_msun_dict_category` | 2.5.58 |
| `m_msun_supplier` | 2.5.62 |
| `m_msun_producer` | 2.5.63 |
| `m_msun_merge_stock` | 2.5.82 |
| `m_msun_drug_batch_stock` | 2.5.43 |
| `m_msun_yk_instock` / `m_msun_yk_instock_detail` | 2.5.102 |
| `m_msun_push_log` | 2.5.41/42 |

## 联调与查看

- 接口测试：`scminterface-admin/static/msun-probe.html`
- 镜像查看：`GET .../mirror/data/{probeKey}`

详细规范见：`scminterface/docs/接口对接规范-镜像表与自动Schema.md`
