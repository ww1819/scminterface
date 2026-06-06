# 众阳 HIS — 枣强县中医院接口现场测试指南

> **层级**：众阳健康（`vendorCode=msun`） → **枣强县中医院**（`hospitalKey=zaoqiang-tcm-001`）  
> **对接名称**：SPD对接  
> **依据文档**：接口文档1（科室/人员）、接口文档2（SPD 药品材料）  
> **当前环境**：`active-env=prod`，众阳基址 `https://zqxzyyy.msuncloud.cn`  
> **镜像落库**：探针/查询默认写入 `m_msun_*`（`mirror.enabled=true` 且 SPD 数据源可用）  
> **SPD 单据推送（唯一实现）**：`MsunSpdBillPushService` + `ZaoqiangTcmMsunSpdBillPushController`（组包/调 HIS/回写/推送后校验）；SPD 业务端 `MsunHisBillPushServiceImpl` **仅 HTTP 委托**，不保留重复组包逻辑。  
> **低层推送（已废弃）**：`ZaoqiangTcmMsunSpdPushController` 的 `/push/drug-stocks-new|return` 仅供排错，SPD 系统勿再调用。

---

## 0. 代码中的 URL 约定

与 `ZaoqiangTcmHospitalConstants`、`MsunVendorConstants` 一致：

| 层级 | 常量 / 配置前缀 | HTTP 前缀 |
|------|-----------------|-----------|
| 众阳厂家 | `MsunVendorConstants.API_PREFIX` | `/api/vendor/msun` |
| 医院列表 | — | `GET /api/vendor/msun/hospitals` |
| 枣强探针（科室/人员） | `ZaoqiangTcmHospitalConstants.API_PREFIX` | `/api/vendor/msun/hospitals/zaoqiang-tcm-001` |
| 枣强 SPD 查询 | `ZaoqiangTcmHospitalConstants.SPD_QUERY_API_PREFIX` | `/api/vendor/msun/hospitals/zaoqiang-tcm-001/spd/query` |
| 枣强 SPD 推送/同步 | `ZaoqiangTcmHospitalConstants.SPD_API_PREFIX` | `/api/spd/msun/hospitals/zaoqiang-tcm-001` |
| 枣强单据推送（联调 JWT） | `API_PREFIX + /spd/bill-push` | `/api/vendor/msun/hospitals/zaoqiang-tcm-001/spd/bill-push` |
| 枣强单据推送（SPD 白名单） | `SPD_API_PREFIX + /bill-push` | `/api/spd/msun/hospitals/zaoqiang-tcm-001/bill-push` |

医院登记：`MsunHospitalRegistry.ZAOQIANG_TCM` ↔ SPD `TenantEnum.ZQ_TCM` / `MsunHisTenantRegistry.ZAOQIANG_TCM`（联调页下拉数据来源）。  
Java 包根路径仅为 `com.scminterface.customer.msun`（**无** `customer/zaoqiangTcm` 旧包）。  
新增其他众阳医院时：在 `MsunHospitalRegistry` 追加项，并在 `customer/msun/hospital/<医院>/` 下新建独立子包。

---

## 1. 推荐方式：统一联调页

1. 启动 scminterface（`server.port=8088`）
2. 确认 `application.yml`：

```yaml
scminterface:
  vendor:
    msun:
      hospitals:
        zaoqiang-tcm-001:
          enabled: true          # false 时不注册枣强 Controller
          active-env: prod       # prod | test，凭证见 ZaoqiangTcmMsunEnvProfile
          depts-path: /msun-middle-base-common/v1/depts
          identities-path: /msun-middle-base-common/v1/identities
```

3. 浏览器登录：`http://<前置机IP>:8088/login.html`（默认 `admin` / `admin123`）
4. 打开联调页：**主页 → 众阳HIS联调**，或 `http://<前置机IP>:8088/msun-probe.html`
5. 页顶选择 **众阳 → 枣强县中医院**（调用 `GET /api/vendor/msun/hospitals` 加载已启用医院）

### 页面与脚本

| 文件 | 作用 |
|------|------|
| `scminterface-admin/.../static/msun-probe.html` | 联调页（JWT 白名单，可直接打开） |
| `static/js/msun-probe.js` | 调用逻辑；基址 = 所选医院 `apiPrefix` |
| `static/js/msun-param-schema.js` | 各接口入参 schema，与 Controller 路径一致 |
| `static/js/msun-bill-push.js` | 「出退库推送」Tab：出库(201)在上、退库(401)在下；推送结果分段 Headers/入参/HIS回参/SPD落库 |
| `static/msun-bill-push.html` | 单据推送独立页（可选，逻辑与联调页 Tab 相同） |

联调页打开后需登录；**页面 HTML 免 token，后端探针 API 需 Bearer token**（页面内 `common.js` 自动携带）。

### 页面功能

| 区域 | 说明 |
|------|------|
| 厂家/医院选择 | 切换 `apiPrefix`，本地参数按 `hospitalKey` 分 key 存储 |
| 环境与清单 | `GET .../env`、一键顺序测试、测试清单、保存参数 |
| 科室/人员 | 对应 `ZaoqiangTcmMsunProbeController` |
| SPD 查询 | 对应 `ZaoqiangTcmMsunSpdQueryController` |
| **出退库推送** | 对应 `ZaoqiangTcmMsunSpdBillPushController`；上下结构，打开 Tab 即加载 201/401 列表 |
| 高级 JSON | POST 接口（2.5.102）可 JSON 覆盖表单 |
| 接口下方回参 | 各接口表单下内联展示；单据推送在列表下方分段展示 |
| **获取全部数据** | 环境与清单页：按 **科室→分类→供应商→厂商→产品档案→出退库→汇总库存→明细库存** 顺序自动翻页拉取；可设出退库起止时间 |

---

## 2. 前置机 HTTP 接口（与代码一一对应）

**枣强基址**（以下路径均以此为前缀）：

`http://<IP>:8088/api/vendor/msun/hospitals/zaoqiang-tcm-001`

### 2.1 环境与基础字典 — `ZaoqiangTcmMsunProbeController`

| 文档编号 | 说明 | 方法 | 前置机完整路径 | 众阳实际路径 |
|----------|------|------|----------------|--------------|
| — | 当前连接环境（不含密钥） | GET | `.../env` | — |
| 2.1.9 | 科室基本信息 | GET | `.../depts` | `/msun-middle-base-common/v1/depts` |
| 2.1.12 | 用户身份信息 | GET | `.../identities` | `/msun-middle-base-common/v1/identities` |
| 2.1.12 | 快速探针：取首个科室再查人员 | GET | `.../identities/sample?roleType=0` | 同上 identities |

**2.1.9 查询参数**：`hospitalAreaId`、`invalidFlag`（默认 -1）、`deptId`、`deptName`  
**2.1.12 查询参数**：`roleType`、`deptId`、`identityId`、`userId` — **至少传一个**；否则返回 400 提示，可用 `/identities/sample` 自动探测。

### 2.2 SPD 查询 — `ZaoqiangTcmMsunSpdQueryController`

前缀：`.../spd/query`

| 文档编号 | 说明 | 方法 | 前置机完整路径 | 众阳实际路径 | 状态 |
|----------|------|------|----------------|--------------|------|
| 2.5.44 | 药品、材料字典 | GET | `.../spd/query/drug-dict-infos` | `/msun-middle-base-resource/v1/drug-dict-infos` | 已实现 |
| 2.5.58 | 分类字典 | GET | `.../spd/query/dict-category` | `/msun-middle-base-dict/v1/dict-category` | 已实现 |
| 2.5.62 | 供应商 | GET | `.../spd/query/drug-suppliers` | `/msun-middle-base-dict/v1/drug-supplieres` | 已实现 |
| 2.5.63 | 生产厂商 | GET | `.../spd/query/drug-producers` | `/msun-middle-base-dict/v1/drug-produceres` | 已实现 |
| 2.5.43 | 药房批次库存 | GET | `.../spd/query/drug-batch-stocks` | `/msun-middle-base-resource/v1/drug-batch-stocks` | 已实现 |
| 2.5.102 | 一级库入退库记录 | POST | `.../spd/query/yk-instock` | `/msun-middle-base-resource/v1/query-yk-instock` | 已实现 |

**2.5.44**：`hospitalId` / `orgId` 留空时，服务端用 `ZaoqiangTcmMsunEnvProfile` 中正式值（`11273002` / `11273`）。  
**2.5.43**：`deptId`、`drugId`、`drugSpecPackingId` 三者必填（Controller 层校验）。回参合并库存 ID 字段名为 **`ycStockQueryId`**（非 2.5.41 的 `stockQueryId`），代码见 `MsunHisBatchStockSupport`。  
**2.5.102**：请求体 JSON，`startTime`、`endTime` 必填，格式 `yyyy-MM-dd HH:mm:ss`。  
**2.5.41 / 2.5.42 出库退库推送**：不经过本查询 Controller，统一走 **§2.4 单据推送 API**（`MsunSpdBillPushService` 内部再调众阳）。

### 2.3 参数依赖（联调易错）

| 接口 | 说明 |
|------|------|
| 2.5.43 | `deptId` ← 2.1.9；`drugId`、`drugSpecPackingId` ← 2.5.44，须成对有效；匹配退库库存用 **`ycStockQueryId`** |
| 2.5.102 | `startTime`、`endTime` 必填 |
| 2.5.58 | 参数名 `keyWord`（注意大小写） |
| 2.5.62/63 | `materialOrDrug` 文档有 0/1/2 差异，以 HIS 回参为准 |

### 2.4 SPD 系统调用（JWT 白名单，供 SPD 业务端）

前缀：`http://<IP>:8088/api/spd/msun/hospitals/zaoqiang-tcm-001`（`JwtAuthenticationFilter` 已放行 `/api/spd/msun/hospitals`）

| 说明 | 方法 | 前置机路径 | 实现类 |
|------|------|------------|--------|
| 主数据一键同步 | POST | `.../sync/{type}` | `ZaoqiangTcmMsunMasterSyncController` |
| **出库/退库单据推送（推荐）** | POST | `.../bill-push/push/{billId}` | `ZaoqiangTcmMsunSpdBillPushController` → `MsunSpdBillPushService` |
| 批量单据推送 | POST | `.../bill-push/push`（body: `{ billIds: [] }`） | 同上 |
| 2.5.43 实时查询（排错/旧链路） | GET | `.../query/drug-batch-stocks` | `ZaoqiangTcmMsunSpdPushController` |
| ~~2.5.41 裸推送~~ | POST | ~~`.../push/drug-stocks-new`~~ | **已废弃**，勿由 SPD 直接组 Body 调用 |
| ~~2.5.42 裸推送~~ | POST | ~~`.../push/drug-stocks-return`~~ | **已废弃**，同上 |

**调用链（与代码一致）**

```
SPD 前端/Controller（MsunHisBillPushController）
  → MsunHisBillPushServiceImpl（仅租户门禁 + 退库本地 qty 校验）
  → HTTP POST .../bill-push/push/{billId}
  → MsunSpdBillPushService（唯一组包/调 HIS/回写/推送后校验）
  → 众阳 2.5.41 / 2.5.42
```

联调页走 JWT 路径：`/api/vendor/msun/hospitals/zaoqiang-tcm-001/spd/bill-push/push/{billId}`，与 SPD 白名单路径**共用同一 Service**，避免两处逻辑分叉。

**组包对照键（`MsunSpdBillPushConstants`，2.5.41 出库明细）**

| 字段 | 规则 | 落库 |
|------|------|------|
| `spdDetailId` | `{billId}:{entryId}` | `stk_io_bill_entry.his_spd_detail_id` |
| `memo` | `ZQ-{tenantId}-{entryId}` | `his_memo` |

**组包对照键（2.5.42 退库明细）**

| 接口字段 | 枣强现场取值 | SPD 来源 |
|----------|--------------|----------|
| `outStockDetailDTOList[].pharmacyStockId` | **传 `his_stock_query_id`** | 2.5.41 回参 `stockQueryId` → 落库；无则 2.5.43 按 `ycBatchNo` 解析 |
| `quantity` | 退库数量 | `stk_io_bill_entry.qty` |
| `memo` | `ZQ-{tenantId}-{entryId}` | 退库明细自身 `entryId` |

> 2.5.41 出库回参字段为 **`stockQueryId`**；2.5.43 批次库存回参字段为 **`ycStockQueryId`**，二者值相同、字段名不同，匹配逻辑见 `MsunHisBatchStockSupport`。

**SPD 本地校验（`MsunHisBillPushServiceImpl.validateReturnGate`）**

| 规则 | 说明 |
|------|------|
| 收货确认 | `stk_dep_inventory.receipt_confirm_status = 1` |
| 本地库存 | `entry.qty ≤` 科室库存 `qty` |
| HIS 可退量 | **不在 SPD 校验**；由 `MsunSpdBillPushService` 推送前调 2.5.43 |

**推送后即时校验（`MsunSpdBillPushVerifyService`，前置机内执行）**

| 单据 | 校验接口 | 异常标注 |
|------|----------|----------|
| 出库/退库 | 2.5.102（`instockCode`=单号，审核时间 ±15min） | `his_push_msg`：未生成出退库明细 |
| 出库 | 2.5.43 批次库存 | `his_push_msg`：未查到批次库存 |

> HIS HTTP 已成功但校验未查到明细/库存时，`his_push_status` 仍为 `2`；仅 `his_push_msg` 写异常文案，不回滚推送。

### 2.5 镜像查看 — `ZaoqiangTcmMsunMirrorQueryController`

前缀：`.../mirror`

| 说明 | 方法 | 路径 |
|------|------|------|
| 探针落库数据 | GET | `.../mirror/data/{probeKey}` |
| 推送日志 | GET | `.../mirror/bill-his?billId=` |
| 批次库存镜像 | GET | `.../mirror/entry-his?...` |

---

## 3. 回参结构

探针接口统一返回 `AjaxResult`（`code`、`msg`、`data`）。`data` 内固定包含：

```json
{
  "requestParams": { },
  "hisBody": { "success": true, "code": "0000", "message": "...", "data": [] }
}
```

Controller 还会在顶层附加（`enrichEnv`）：

`vendorCode`、`vendorName`、`hospitalKey`、`hospitalName`、`tenantId`（= `zaoqiang-tcm-001`）、`activeEnv`、`msunBaseUrl`

联调页与人工判断以 **`data.hisBody.success`**、**`data.hisBody.code`**、**`data.hisBody.message`** 为准。

各接口调用成功后，表单下方展示三块排错区（与单据推送页一致）：

| 区块 | 内容 |
|------|------|
| **Headers** | 众阳 HIS 签名头（`sign`/`license`/`appId` 已脱敏）；环境/镜像查询为前置机 JWT |
| **入参** | GET 为 Query（`requestParams`）；POST 为 JSON Body |
| **回参** | `hisBody` 或完整 `AjaxResult`；含 `mirrorSync` / `cascadeBatch` 时另附块 |

完整调试对象在 `data.hisInvoke`（服务端 `MsunHisInvokeDebugSupport` 生成）。

`GET .../env` 的 `data` 为环境摘要：`activeEnv`、`label`、`baseUrl`、`appId`、`hospitalId`、`orgId`、`signType`、`documentUrl`、`deptsUrl`、`identitiesUrl`（**不含 appSecret**）。

### 3.1 2.5.41 出库推送成功样本（请求 + 回参）

> **记录时间**：2026-06-06  
> **环境**：`prod` / `zaoqiang-tcm-001`  
> **单据**：出库单号 `CK2026060600002`；主单 `stk_io_bill.id=3`，明细 `stk_io_bill_entry.id=3`  
> **接口**：众阳 `POST /msun-middle-base-resource/v1/drug-stocks-new`；前置机经 `MsunSpdBillPushService` 组包后发出（联调/SPD 均走 `.../bill-push/push/{billId}`，勿直接调已废弃的 `/push/drug-stocks-new`）  
> **用途**：维护人员核对组包逻辑、对照联调页 Headers/Body、推送成功但 SPD 未回写时排错。

#### 3.1.1 请求 Header（众阳 SM2 签名，敏感字段已脱敏）

由 `MsunSignedHttpClient.buildHeaders` 写入 HTTP Header（**非** JSON Body）。`license`、`sign` 随请求时间与 Body 动态生成，下文仅保留结构样本；**勿将完整 `license`/`sign` 写入文档或提交版本库**。

```json
{
  "license": "MEUCIQD****...****fnCU=",
  "hospitalId": "11273002",
  "appId": "app1779****7837",
  "sign": "MEUCIGl/Z5****...****xJp5U=",
  "signType": "SM2",
  "orgId": "11273",
  "timestamp": "1780755841283"
}
```

| Header | 样本/说明 | 代码来源 |
|--------|-----------|----------|
| `appId` | prod：`app1779776749809786837`（文档脱敏展示） | `ZaoqiangTcmMsunEnvProfile` / `MsunHospitalRuntime` |
| `signType` | `SM2` | 同上；`appSecret` 为 SM2 私钥时固定 SM2 |
| `hospitalId` | `11273002` | 环境凭证 |
| `orgId` | `11273` | 环境凭证 |
| `timestamp` | 毫秒时间戳字符串 | `System.currentTimeMillis()` |
| `sign` | **脱敏** | `SM2.sign( MD5(bodyJson + timestamp) )`，见 `MsunSignedHttpClient.postWithDebug` |
| `license` | **脱敏** | `OpenapiUtil.getLicense(appId, appSecret, signType, timestamp)` |
| `loginUser` | 可选 | 配置非空时追加 |

联调页 **Headers** 区块展示的是上述 Map；复制排错时可与 `m_msun_push_log` 或 `hisInvoke.requestHeaders` 对照（日志中 `sign`/`license` 亦为当时有效值，勿外泄）。

#### 3.1.2 请求 Body（2.5.41 入参）

由 **`MsunSpdBillPushService.buildOutboundBody`** 唯一组装（SPD 侧已无重复实现）。联调页 **入参 Request Body** 来自 `results[].hisInvoke.requestBody`，已剔除内部字段 `_spdLogMeta`。

```json
{
  "supplierId": 41,
  "storageDeptId": 8837823902808417000,
  "pharmacyDeptId": 8837863243411958000,
  "invoiceCode": "CK2026060600002",
  "inStockStatus": "",
  "spdMainId": "CK2026060600002",
  "saveCorrelationFlag": "1",
  "inStockDetailDTOList": [
    {
      "drugId": 1565,
      "drugSpecPackingId": 6102,
      "quantity": 100,
      "buyPrice": 0.19,
      "retailPrice": 0.19,
      "invoiceCode": "CK2026060600002",
      "produceDate": "2026-06-06 22:24:00",
      "effectiveDate": "2027-05-12 00:00:00",
      "ycBatchNo": "25041824-1",
      "spdDetailId": "3:3",
      "memo": "ZQ-zaoqiang-tcm-001-3"
    }
  ]
}
```

| 字段 | 样本值 | SPD / 代码来源 |
|------|--------|----------------|
| `supplierId` | `41` | `fd_supplier.his_id`（`resolveSupplierHisId`） |
| `storageDeptId` | 药库科室 HIS ID | `fd_warehouse` 关联科室 `his_id`（`resolveWarehouseHisId`） |
| `pharmacyDeptId` | 领用科室 HIS ID | `fd_department.his_id`（`resolveDepartmentHisId`） |
| `invoiceCode` / `spdMainId` | `CK2026060600002` | `stk_io_bill.bill_no` |
| `inStockStatus` | `""`（空串） | `MsunSpdBillPushConstants.IN_STOCK_STATUS_PHARMACY` → 入库到**药房** |
| `saveCorrelationFlag` | `"1"` | 常量 `SAVE_CORRELATION_FLAG` |
| `inStockDetailDTOList[].drugId` | `1565` | `fd_material.his_id` |
| `inStockDetailDTOList[].drugSpecPackingId` | `6102` | `fd_material.his_spec_packing_id` |
| `inStockDetailDTOList[].quantity` | `100` | `stk_io_bill_entry.qty` |
| `buyPrice` / `retailPrice` | `0.19` | `stk_io_bill_entry.unit_price` |
| `produceDate` | `yyyy-MM-dd HH:mm:ss` | `begin_time`，空则当前时间（`MsunHisDateTimeSupport`） |
| `effectiveDate` | `yyyy-MM-dd HH:mm:ss` | `end_time`（`formatHisEffectiveDate`，禁止 `T` 分隔符） |
| `ycBatchNo` | `25041824-1` | `stk_io_bill_entry.batch_number` |
| `spdDetailId` | `3:3` | `{billId}:{entryId}` → `his_spd_detail_id` |
| `memo` | `ZQ-zaoqiang-tcm-001-3` | `ZQ-{tenantId}-{entryId}` → `his_memo` |

> **雪花 ID 注意**：`storageDeptId`、`pharmacyDeptId` 为 19 位长整型。浏览器/部分 JSON 预览可能四舍五入末位，排错请以 Java 日志、`m_msun_push_log.request_json` 或数据库 `his_id` 字符串为准。

#### 3.1.3 hisBody 本体（众阳 HTTP 原始回参）

以下为 `wrapRawResponse` 解析后的 **`hisBody`** 内容（`success=true`、`code=0000` 表示 HIS 写库成功）：

```json
{
  "success": true,
  "decorate": true,
  "code": "0000",
  "message": "成功",
  "data": [
    {
      "pharmacyStockId": "8837866950092152196",
      "pharmacyDeptId": "8837863243411957635",
      "ycId": "1565",
      "ycBatch": null,
      "ycBatchNo": "25041824-1",
      "expireDate": "2027-05-12 00:00:00",
      "quantity": 100,
      "originalQuantity": 100,
      "stockPackId": "50",
      "packageSpec": "1.000000个/个",
      "retailPrice": 0.19,
      "stockType": "1         ",
      "ycName": "三伏贴皮",
      "ycCode": "02000201",
      "ycSpec": "6*6",
      "producerId": "-1",
      "producerName": null,
      "discount": 100,
      "flagInvalid": "0",
      "tradePrice": 0.19,
      "supplierId": "41",
      "supplierName": null,
      "approvalNo": null,
      "catagoryId": "1",
      "catagoryName": "卫生材料",
      "stockQueryId": "8837866950089530760",
      "instockTime": "2026-06-06 22:24:04",
      "buyPrice": 0.19,
      "hospitalId": "11273002",
      "produceDate": "2026-06-06 22:24:00",
      "storageOutstockDetailId": "8837866949759753608",
      "drugSpecPackingId": "6102",
      "storageStockId": "8837866949707849090",
      "changePackageRena": null,
      "dictProvinceId": null,
      "stopQuantity": 0,
      "ycShelvesId": null,
      "pharmacyInstockDetailId": null,
      "pharmacyOutstockDetailId": null,
      "inventoryUpdateTime": "2026-06-06 22:24:04",
      "memo": "ZQ-zaoqiang-tcm-001-3",
      "spdDetailId": "3:3",
      "orgId": "11273"
    }
  ],
  "traceId": "19af9a57-3e76-4712-80ad-4ccd47813f04",
  "exceptionName": null,
  "qualityMonitor": null,
  "ignoreQualityMonitor": false,
  "level": "info",
  "service": "msun-middle-base-resource",
  "businessException": false
}
```

#### 3.1.4 外层包装（前置机 / scminterface）

同一笔推送在不同调用链上的 JSON 层级不同，回写代码须能解析到 **`hisBody`**（见 `MsunHisResponseSupport.resolveHisBody`）：

| 调用链 | 结构 | `hisBody` 位置 |
|--------|------|----------------|
| scminterface 直连众阳（`MsunSpdPushService`） | `{ requestParams, hisBody }` | 根级 `hisBody` |
| 单据推送 `.../bill-push/push/{id}` 成功且已调 HIS | `{ code, msg, data: { results: [{ hisInvoke: { hisBody, requestBody, requestHeaders } }] } }` | `data.results[].hisInvoke.hisBody` |
| 低层裸推送 `ZaoqiangTcmMsunSpdPushController`（已废弃） | `{ code, msg, data: { data: { hisBody }, hisInvoke } }` | `data.data.hisBody` |
| 仅内层包装 | `{ data: { hisBody, requestParams } }` | `data.hisBody` |

**联调页「出退库推送」Tab**（`msun-bill-push.js`）推送后在列表下方分段展示：**Headers / 入参 Request Body / 回参 Response（HIS hisBody）/ SPD 落库回写**；`status=skipped` 时无 Headers/入参/HIS 块属正常（见 §3.2.2）。

**判断推送成功**：`hisBody.success === true` 且 `hisBody.code === "0000"`；明细数组在 `hisBody.data[]`。

#### 3.1.5 明细匹配键（回写 SPD 前）

| HIS 字段 | 样本值 | 含义 |
|----------|--------|------|
| `memo` | `ZQ-zaoqiang-tcm-001-3` | `ZQ-{tenantId}-{entryId}` → 本例 **entryId=3** |
| `spdDetailId` | `3:3` | `{billId}:{entryId}` → 本例 **billId=3, entryId=3** |

回写时优先按 `memo` 匹配，其次 `spdDetailId`，最后按 `entryId` 字符串兜底（见 `MsunSpdBillPushService.applyOutboundResponse`）。

#### 3.1.6 HIS 回参 → SPD 落库字段

| HIS 回参字段 | SPD 表.字段 | 样本值 | 说明 |
|--------------|-------------|--------|------|
| `pharmacyStockId` | `stk_io_bill_entry.his_pharmacy_stock_id` | `8837866950092152196` | 药房批次库存 ID；**2.5.42 退库必填** |
| `storageStockId` | `stk_io_bill_entry.his_storage_stock_id` | `8837866949707849090` | 药库批次库存 ID |
| `stockQueryId` | `stk_io_bill_entry.his_stock_query_id` | `8837866950089530760` | 库存查询 ID |
| 同上三字段 | `stk_dep_inventory.his_pharmacy_stock_id` 等 | 同上 | 按明细 `dep_inventory_id`（或 `kc_no`）回写科室库存 |
| `traceId`（hisBody 根级） | `stk_io_bill.his_trace_id` | `19af9a57-3e76-4712-80ad-4ccd47813f04` | 主单追溯 |
| — | `stk_io_bill.his_push_status` | `2` | `0` 未推 / `1` 推送中 / `2` 成功 / `3` 失败 |
| — | `stk_io_bill_entry.his_push_status` | `2` | 明细推送状态 |
| — | `stk_io_bill.his_push_msg` | `NULL` | 成功时清空；校验异常另写文案但不改 `his_push_status` |

> **科室库存 `qty` 与 HIS `stockAmount`（易混）**  
> `stk_dep_inventory.qty` 为 **SPD 本地库存**（出库审核后已扣减，本例可为 `0`）。  
> 2.5.43 回参 `stockAmount`（如 `100`）为 **HIS 药房可退量**，仅用于退库推送前校验（`MsunSpdBillPushService.assertReturnQtyWithinHis`），**不回写** `stk_dep_inventory.qty`。  
> 推送成功对科室库存表仅更新 `his_pharmacy_stock_id` / `his_storage_stock_id` / `his_stock_query_id`（见 `updateDepInventoryHisStock`）。

**手工补标 SQL 模板**（将 `<dep_inventory_id>` 换为明细上的 `dep_inventory_id`）：

```sql
UPDATE stk_io_bill_entry SET
  his_pharmacy_stock_id = '8837866950092152196',
  his_storage_stock_id  = '8837866949707849090',
  his_stock_query_id    = '8837866950089530760',
  his_push_status = '2', his_push_msg = NULL, update_time = NOW()
WHERE id = 3 AND tenant_id = 'zaoqiang-tcm-001';

UPDATE stk_io_bill SET
  his_push_status = '2', his_push_time = NOW(),
  his_trace_id = '19af9a57-3e76-4712-80ad-4ccd47813f04',
  his_push_msg = NULL, update_time = NOW()
WHERE id = 3 AND tenant_id = 'zaoqiang-tcm-001';

UPDATE stk_dep_inventory SET
  his_pharmacy_stock_id = '8837866950092152196',
  his_storage_stock_id  = '8837866949707849090',
  his_stock_query_id    = '8837866950089530760',
  update_time = NOW()
WHERE id = <dep_inventory_id> AND tenant_id = 'zaoqiang-tcm-001';
```

#### 3.1.7 排错检查清单

| 现象 | 建议操作 |
|------|----------|
| 组包与代码不一致 | 对照 **§3.1.1 Header**、**§3.1.2 Body** 与 `MsunSpdBillPushService.buildOutboundBody`；联调页三块：Headers / Request Body / Response |
| `sign` / `license` 校验失败 | 核对 `appId`/`orgId`/`hospitalId` 是否成套（§4 环境表）；`timestamp` 是否与 Body 同一次请求；**勿手工拼 sign** |
| `effectiveDate` 格式错误 | 必须为 `yyyy-MM-dd HH:mm:ss`，见 §3.1.2 样本 |
| HIS 成功、SPD 标志仍为 `0`/`1`/`3` | 查 `m_msun_push_log.response_json` 是否含 **§3.1.3** `hisBody`；确认回写代码是否从正确层级取 `hisBody`（勿只读 `response.data.hisBody` 而漏掉 `data.data.hisBody` 或根级 `hisBody`） |
| 报「HIS回参未匹配明细」 | 核对 **§3.1.2** 入参与 **§3.1.3** 回参的 `memo` / `spdDetailId`；本例应为 `ZQ-zaoqiang-tcm-001-3` 与 `3:3` |
| `his_pharmacy_stock_id` 为空 | 看回参 `pharmacyStockId`；入库到药房时取 `pharmacyStockId`，入药库时可能只有 `storageStockId`（代码 `firstNonEmpty` 兜底） |
| 推送日志有、业务表无 | 确认 `spring.datasource.druid.spd.enabled=true`；组包/回写仅在 `MsunSpdBillPushService`，SPD 为 HTTP 委托 |
| 对账「当时推了什么」 | `GET .../mirror/bill-his?billId=3` 或查 `m_msun_push_log` |

### 3.2 单据推送接口回参（出退库推送页 / `spd/bill-push`）

> **接口**：`POST .../spd/bill-push/push/{billId}` 或 `POST .../spd/bill-push/push`（`billIds` 数组）  
> **双路径**：联调 JWT `.../api/vendor/msun/hospitals/zaoqiang-tcm-001/spd/bill-push/...`；SPD 白名单 `.../api/spd/msun/hospitals/zaoqiang-tcm-001/bill-push/...`  
> **用途**：区分**本次真正调 HIS**（`pushedCount`）与**已跳过**（`skipCount`，明细 `his_push_status=2`）。

#### 3.2.1 外层包装（前置机 `ZaoqiangTcmMsunSpdBillPushController`）

顶层字段：`code`、`msg`、`tenantId`、`activeEnv`、`hospitalName`、`data`。

`data` 汇总：

| 字段 | 含义 |
|------|------|
| `total` | 本次请求推送的单据数 |
| `pushedCount` | **本次真正调用 HIS 且成功** 的单据数 |
| `skipCount` | 跳过数（明细 `his_push_status=2`，未再调 HIS） |
| `failCount` | 失败数 |
| `successCount` | 与 `pushedCount` 相同（兼容旧字段，**不含**跳过） |
| `message` | 汇总文案，与顶层 `msg` 一致 |
| `results[]` | 每张单据一条 |

单条 `results[]`：

| 字段 | `status=pushed` | `status=skipped` | `status=failed` |
|------|-----------------|------------------|-----------------|
| `success` | `true` | `true` | `false` |
| `status` | `pushed` | `skipped` | `failed` |
| `skipped` | `false` | `true` | `false` |
| `billId` / `billNo` | 有 | 有 | 有 |
| `billType` | `201`/`401` | 有 | 有 |
| `pushedEntryCount` | 本次推送明细数 | `0` | — |
| `entryTotal` | 主单明细总数 | 有 | — |
| `alreadyPushedEntryCount` | — | 已成功明细数 | — |
| `traceId` | HIS `traceId` | 无 | 无 |
| `hisInvoke` | Headers/入参/回参 | **无**（未调 HIS） | 可能有部分入参 |
| `message` | — | 跳过原因 | 失败原因 |

#### 3.2.2 退库单「已跳过」样本（2026-06-06）

退库单 `TK2026060600001`（`billId=5`），明细 `his_push_status` 已为 `2`，再次点击推送时**不会重复调 2.5.42**：

```json
{
  "code": 200,
  "msg": "已跳过：1 张单据明细均已推送成功，未重复调用 HIS",
  "tenantId": "zaoqiang-tcm-001",
  "activeEnv": "prod",
  "hospitalName": "枣强县中医院",
  "data": {
    "total": 1,
    "pushedCount": 0,
    "skipCount": 1,
    "failCount": 0,
    "successCount": 0,
    "message": "已跳过：1 张单据明细均已推送成功，未重复调用 HIS",
    "results": [
      {
        "billId": 5,
        "success": true,
        "status": "skipped",
        "billNo": "TK2026060600001",
        "billType": 401,
        "skipped": true,
        "entryTotal": 1,
        "alreadyPushedEntryCount": 1,
        "pushedEntryCount": 0,
        "message": "无待推送明细（均已成功），未调用 HIS"
      }
    ]
  }
}
```

> **易错点**：旧版将跳过计入 `successCount=1` 且 `msg=推送完成`，易误判为「刚推成功」。修正后 `pushedCount=0`、`skipCount=1`，联调页无 `hisInvoke` 块属正常。

若需**强制重推**，须先将明细 `his_push_status` 改为 `0` 或 `3`（并确认 HIS 侧允许），再重新推送。

#### 3.2.3 2.5.42 退库推送成功样本（请求 + 回参）

> **单据**：退库单号 `TK2026060600001`；主单 `stk_io_bill.id=5`，明细 `stk_io_bill_entry.id=4`  
> **众阳路径**：`POST /msun-middle-base-resource/v1/drug-stocks-new/d`；前置机经 `MsunSpdBillPushService.buildReturnBody` 组包（`.../bill-push/push/5`）

**请求 Body**（`MsunSpdBillPushService.buildReturnBody`）：

```json
{
  "storageDeptId": 8837823902808417000,
  "pharmacyDeptId": 8837863243411958000,
  "isReturnToSupplier": "1",
  "outStockDetailDTOList": [
    {
      "pharmacyStockId": 8837866950089530760,
      "quantity": 100,
      "memo": "ZQ-zaoqiang-tcm-001-4"
    }
  ]
}
```

| 字段 | 样本/规则 | SPD 来源 |
|------|-----------|----------|
| `storageDeptId` | 药库科室 HIS ID | `fd_warehouse` → `his_id` |
| `pharmacyDeptId` | 退库科室 HIS ID | `fd_department` → `his_id` |
| `isReturnToSupplier` | `"1"` | 退供应商 |
| `pharmacyStockId` | `8837866950089530760` | **`his_stock_query_id`**（非 `his_pharmacy_stock_id`） |
| `quantity` | 退库数量 | `stk_io_bill_entry.qty` |
| `memo` | `ZQ-zaoqiang-tcm-001-4` | `ZQ-{tenantId}-{entryId}` |

推送前会经 2.5.43 校验 `qty ≤ stockAmount`（字段 `ycStockQueryId` 参与匹配）。

**hisBody 成功回参**（2.5.42 与 2.5.41 类似，成功时 `data` 常为空数组）：

```json
{
  "success": true,
  "code": "0000",
  "message": "成功",
  "data": [],
  "traceId": "<HIS-traceId>"
}
```

**本次真正推送成功时**，`results[0]` 额外包含：

```json
{
  "status": "pushed",
  "skipped": false,
  "pushedEntryCount": 1,
  "traceId": "<HIS-traceId>",
  "hisInvoke": {
    "apiCode": "2.5.42",
    "requestHeaders": { },
    "requestBody": { },
    "hisBody": { }
  }
}
```

回写：`stk_io_bill_entry.his_push_status=2`；主单 `his_push_status=2`、`his_trace_id`；**不退写**新的 `pharmacyStockId`（退库回参通常无明细数组）。

#### 3.2.4 退库推送排错清单

| 现象 | 建议操作 |
|------|----------|
| `msg` 为「已跳过」、`pushedCount=0` | 查 `stk_io_bill_entry.his_push_status` 是否已为 `2`；属防重复，非失败 |
| 无 `hisInvoke` 块 | 仅 `status=skipped` 时出现；`status=pushed` 时应有 Headers/入参/回参 |
| 报「退库数量超过HIS可退量」 | 调 2.5.43 核对 `stockAmount`；匹配键用 `ycStockQueryId`（见 §2.4） |
| `pharmacyStockId` 入参错误 | 枣强须传 `his_stock_query_id`，勿传 `his_pharmacy_stock_id` |
| 认为未推过但显示已跳过 | 查是否手工改过 `his_push_status`；查 `m_msun_push_log` 是否有 2.5.42 记录 |

---

## 4. 环境与凭证

配置项前缀：`scminterface.vendor.msun.hospitals.zaoqiang-tcm-001`（见 `ZaoqiangTcmMsunProperties`）。

凭证枚举：`scminterface-framework/src/main/java/com/scminterface/customer/msun/hospital/zaoqiangtcm/config/ZaoqiangTcmMsunEnvProfile.java`

| `active-env` | 标签 | 众阳 baseUrl | hospitalId | orgId | appId |
|--------------|------|--------------|------------|-------|-------|
| `prod`（当前默认） | 枣强正式-SPD对接 | `https://zqxzyyy.msuncloud.cn` | `11273002` | `11273` | `app1779776749809786837` |
| `test` | 灰度测试 | `https://thirdpart-graytest.msunhis.com:9443` | `407545331508854784` | `10001` | `app1730369704514752213` |

签名方式：SM2（`appSecret` 存于 `EnvProfile`，勿写入文档或现场修改）。  
众阳在线文档（prod）：`https://openapi.msuncloud.com/document/app1779776749809786837`

---

## 5. PowerShell 调用示例（需 Token）

```powershell
$base = "http://127.0.0.1:8088"
$login = Invoke-RestMethod -Method Post -Uri "$base/login" -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'
$h = @{ Authorization = "Bearer $($login.data.token)" }

# 厂家 — 已启用医院列表
Invoke-RestMethod -Headers $h -Uri "$base/api/vendor/msun/hospitals"

# 枣强 — 环境
Invoke-RestMethod -Headers $h -Uri "$base/api/vendor/msun/hospitals/zaoqiang-tcm-001/env"

# 枣强 — 2.1.9 科室
Invoke-RestMethod -Headers $h -Uri "$base/api/vendor/msun/hospitals/zaoqiang-tcm-001/depts?invalidFlag=-1"

# 枣强 — 2.5.44 字典
Invoke-RestMethod -Headers $h -Uri "$base/api/vendor/msun/hospitals/zaoqiang-tcm-001/spd/query/drug-dict-infos?limitCount=10&materialOrDrug=0"

# 枣强 — 2.5.102 入退库（POST）
$body = '{"startTime":"2026-05-29 00:00:00","endTime":"2026-06-05 23:59:59"}'
Invoke-RestMethod -Method Post -Headers $h -Uri "$base/api/vendor/msun/hospitals/zaoqiang-tcm-001/spd/query/yk-instock" `
  -ContentType "application/json" -Body $body

# 枣强 — 出库/退库单据推送（SPD 白名单路径，无需 Token；联调页用 vendor 路径 + JWT）
Invoke-RestMethod -Method Post -Uri "$base/api/spd/msun/hospitals/zaoqiang-tcm-001/bill-push/push/3" `
  -ContentType "application/json" -Body "{}"
```

---

## 6. 联调测试（统一入口）

现场与开发联调**统一使用联调页**，不再维护 Java `main` 命令行探针（已移除 `...zaoqiangtcm.test` 包）。

1. 启动 `scminterface-admin`（VS Code：**scminterfaceApplication**）
2. 浏览器打开：`http://<IP>:8088/msun-probe.html`
3. 登录后选择 **枣强县中医院**，使用各 Tab 调用接口，或 **一键顺序测试** / **获取全部数据**
4. **出退库推送** Tab：上方出库单(201)、下方退库单(401)；推送后在对应区块下方查看分段回参

联调页经前置机 Controller 调用众阳；**单据推送**与 SPD 生产环境共用 `MsunSpdBillPushService`（SPD 经 `/api/spd/.../bill-push` 委托）。

---

## 7. 建议测试顺序

**获取全部数据（实施拉数）**：环境与清单 → 填写出退库起止时间 → **获取全部数据**。库存组数默认最多 30 组（可在页面调整上限）。

- [ ] 1. 联调页选择 **枣强县中医院**，调用 `.../env` 确认 `activeEnv=prod`
- [ ] 2. **一键顺序测试**（环境 → 科室 → 人员 sample → 字典 → 分类 → 供应商 → 厂商 → 批次[条件允许] → 入退库）
- [ ] 3. 2.5.43 三要素不齐时一键测试会跳过，可手工从 2.1.9 + 2.5.44 填充后单独调用
- [ ] 4. **出退库推送** Tab：查询已审核单 → 推送 → 对照 Headers/入参/HIS回参/SPD落库（§3.2）
- [ ] 5. **生成反馈 → 复制反馈** 提交项目组

---

## 8. 常见问题

| 现象 | 处理 |
|------|------|
| `openapi@9984` | 联系众阳为对应 `appId` 开通接口权限 |
| `hisBody.success=true`, `code=0000` | 正常；出库成功请求/回参样本见 **§3.1** |
| HIS 成功但 SPD `his_push_status` 未变 `2` | 确认非 `skipped`（§3.2.2）；对照 **§3.1.4** `hisBody` 层级；查 `m_msun_push_log` |
| 推送显示成功但 `pushedCount=0` | 明细已为 `his_push_status=2`，属跳过非失败（§3.2.2） |
| 报「退库数量超过HIS可退量」 | 2.5.43 的 `stockAmount` 与 `ycStockQueryId` 匹配；勿用错 `stockQueryId` 字段名 |
| 2.5.43 `stockAmount=100` 但 `stk_dep_inventory.qty=0` | 正常：本地 `qty` 已扣；HIS `stockAmount` 不回写 `qty`（§3.1.6） |
| Header `sign`/`license` 与众阳不一致 | 以 **§3.1.1** 核对字段是否齐全；凭证见 §4，禁止在文档中记录完整签名 |
| 401 | 重新登录获取 token |
| 404 | 确认 `zaoqiang-tcm-001.enabled=true` 且服务已重启 |
| 2.5.43 失败 | 检查 `deptId` / `drugId` / `drugSpecPackingId` 是否来自有效 2.1.9 + 2.5.44 回参 |
| 正式失败、灰度成功 | 确认 `active-env=prod`，且 `hospitalId`/`orgId` 与 prod 凭证成套 |

---

## 9. 代码位置

众阳对接代码**全部**位于 `scminterface-framework/src/main/java/com/scminterface/customer/msun/`，按「厂家共用 → 医院客户」分层：

| 层级 | 包路径 | 主要类 |
|------|--------|--------|
| 众阳厂家 | `...customer.msun` | `MsunVendorConstants`、`MsunApiPaths` |
| 厂家支撑 | `...customer.msun.support` | `MsunSignedHttpClient`、`MsunSm2Util`、`MsunOpenApiSupport`、**`MsunHisBatchStockSupport`**（2.5.43 `ycStockQueryId`/`stockAmount`） |
| SPD 路径 | `...customer.msun.spd` | `MsunSpdApiPaths` |
| 共用服务 | `...customer.msun.service` | `MsunProbeService`、`MsunSpdQueryService` |
| 厂家 Web | `...customer.msun.web` | `MsunHospitalListController` |
| 医院登记 | `...customer.msun.hospital` | `MsunHospitalRegistry`、`MsunHospitalRuntime` |
| 枣强客户 | `...customer.msun.hospital.zaoqiangtcm` | `ZaoqiangTcmHospitalConstants` |
| 枣强配置 | `...hospital.zaoqiangtcm.config` | `ZaoqiangTcmMsunEnvProfile`、`ZaoqiangTcmMsunProperties`、`ZaoqiangTcmMsunConfiguration` |
| 枣强入口 | `...hospital.zaoqiangtcm.web` | `ZaoqiangTcmMsunProbeController`、`ZaoqiangTcmMsunSpdQueryController`、**`ZaoqiangTcmMsunSpdBillPushController`**、`ZaoqiangTcmMsunSpdPushController`（裸推送已废弃）、`ZaoqiangTcmMsunMasterSyncController`、`ZaoqiangTcmMsunMirrorQueryController` |
| 单据推送编排 | `...customer.msun.spd.billpush` | **`MsunSpdBillPushService`**、`MsunSpdBillPushVerifyService`、`MsunSpdBillPushMapper` |

其他模块：

| 说明 | 路径 |
|------|------|
| 运行时配置 | `scminterface-admin/src/main/resources/application.yml` |
| 联调页 | `scminterface-admin/src/main/resources/static/msun-probe.html` |
| 联调脚本 | `static/js/msun-probe.js`、`msun-param-schema.js`、**`msun-bill-push.js`** |
| SPD 推送门面（仅 HTTP 委托） | `spd-biz/.../MsunHisBillPushServiceImpl` → `POST .../bill-push/push/{billId}` |
| JWT 白名单 | `scminterface-framework/.../security/JwtAuthenticationFilter.java`（`/msun-probe.html`） |
| 本文档 | `scminterface/docs/众阳 HIS — 枣强县中医院接口现场测试指南.md` |

---

*文档与 `com.scminterface.customer.msun` 包同步维护；旧路径 `customer/zaoqiangTcm` 已废弃删除。*
