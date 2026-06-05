# 众阳 HIS — 枣强县中医院接口现场测试指南

> **层级**：众阳健康（`vendorCode=msun`） → **枣强县中医院**（`hospitalKey=zaoqiang-tcm-001`）  
> **对接名称**：SPD对接  
> **依据文档**：接口文档1（科室/人员）、接口文档2（SPD 药品材料）  
> **当前环境**：`active-env=prod`，众阳基址 `https://zqxzyyy.msuncloud.cn`  
> **本阶段**：探针仅查询、原样回参，**不落库**；推送 2.5.41 / 2.5.42 **代码未实现、HTTP 未开放**

---

## 0. 代码中的 URL 约定

与 `ZaoqiangTcmHospitalConstants`、`MsunVendorConstants` 一致：

| 层级 | 常量 / 配置前缀 | HTTP 前缀 |
|------|-----------------|-----------|
| 众阳厂家 | `MsunVendorConstants.API_PREFIX` | `/api/vendor/msun` |
| 医院列表 | — | `GET /api/vendor/msun/hospitals` |
| 枣强探针（科室/人员） | `ZaoqiangTcmHospitalConstants.API_PREFIX` | `/api/vendor/msun/hospitals/zaoqiang-tcm-001` |
| 枣强 SPD 查询 | `ZaoqiangTcmHospitalConstants.SPD_QUERY_API_PREFIX` | `/api/vendor/msun/hospitals/zaoqiang-tcm-001/spd/query` |

医院登记：`MsunHospitalRegistry.ZAOQIANG_TCM`（联调页下拉数据来源）。  
新增其他众阳医院时：在登记册追加项，并在 `customer/msun/hospital/<医院>/` 下新建独立子包。

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

联调页打开后需登录；**页面 HTML 免 token，后端探针 API 需 Bearer token**（页面内 `common.js` 自动携带）。

### 页面功能

| 区域 | 说明 |
|------|------|
| 厂家/医院选择 | 切换 `apiPrefix`，本地参数按 `hospitalKey` 分 key 存储 |
| 环境与清单 | `GET .../env`、一键顺序测试、测试清单、保存参数 |
| 科室/人员 | 对应 `ZaoqiangTcmMsunProbeController` |
| SPD 查询 | 对应 `ZaoqiangTcmMsunSpdQueryController` |
| 高级 JSON | POST 接口（2.5.102）可 JSON 覆盖表单 |
| 右侧回参 | 当前回参、测试记录、导出反馈 |

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
| 2.5.41 | 药品材料入库 | POST | — | `/msun-middle-base-resource/v1/drug-stocks-new` | **未实现** |
| 2.5.42 | 药品材料退库 | POST | — | `/msun-middle-base-resource/v1/drug-stocks-new/d` | **未实现** |

**2.5.44**：`hospitalId` / `orgId` 留空时，服务端用 `ZaoqiangTcmMsunEnvProfile` 中正式值（`11273002` / `11273`）。  
**2.5.43**：`deptId`、`drugId`、`drugSpecPackingId` 三者必填（Controller 层校验）。  
**2.5.102**：请求体 JSON，`startTime`、`endTime` 必填，格式 `yyyy-MM-dd HH:mm:ss`。

### 2.3 参数依赖（联调易错）

| 接口 | 说明 |
|------|------|
| 2.5.43 | `deptId` ← 2.1.9；`drugId`、`drugSpecPackingId` ← 2.5.44，须成对有效 |
| 2.5.102 | `startTime`、`endTime` 必填 |
| 2.5.58 | 参数名 `keyWord`（注意大小写） |
| 2.5.62/63 | `materialOrDrug` 文档有 0/1/2 差异，以 HIS 回参为准 |

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

`GET .../env` 的 `data` 为环境摘要：`activeEnv`、`label`、`baseUrl`、`appId`、`hospitalId`、`orgId`、`signType`、`documentUrl`、`deptsUrl`、`identitiesUrl`（**不含 appSecret**）。

---

## 4. 环境与凭证

配置项前缀：`scminterface.vendor.msun.hospitals.zaoqiang-tcm-001`（见 `ZaoqiangTcmMsunProperties`）。

凭证枚举：`scminterface-framework/.../hospital/zaoqiangtcm/config/ZaoqiangTcmMsunEnvProfile.java`

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
```

---

## 6. 本地 Java 联调（不启动 Spring）

| 用途 | VS Code / IDE 配置名 | 主类 |
|------|----------------------|------|
| 科室 + 人员 | 枣强众阳探针-正式库 | `com.scminterface.customer.msun.hospital.zaoqiangtcm.test.ZaoqiangTcmMsunProbeMain` |
| 科室 + 人员（灰度） | 枣强众阳探针-测试库 | 同上（无参数，默认 `test`） |
| SPD 查询 | 枣强SPD查询探针-正式库 | `com.scminterface.customer.msun.hospital.zaoqiangtcm.test.ZaoqiangTcmMsunSpdQueryProbeMain` |

正式库传参：`prod`。直连众阳，不经过前置机 Controller。

```powershell
cd <scminterface 根目录>
mvn install -pl scminterface-common,scminterface-framework -am -DskipTests
```

---

## 7. 建议测试顺序

- [ ] 1. 联调页选择 **枣强县中医院**，调用 `.../env` 确认 `activeEnv=prod`
- [ ] 2. **一键顺序测试**（环境 → 科室 → 人员 sample → 字典 → 分类 → 供应商 → 厂商 → 批次[条件允许] → 入退库）
- [ ] 3. 2.5.43 三要素不齐时一键测试会跳过，可手工从 2.1.9 + 2.5.44 填充后单独调用
- [ ] 4. **生成反馈 → 复制反馈** 提交项目组

---

## 8. 常见问题

| 现象 | 处理 |
|------|------|
| `openapi@9984` | 联系众阳为对应 `appId` 开通接口权限 |
| `hisBody.success=true`, `code=0000` | 正常 |
| 401 | 重新登录获取 token |
| 404 | 确认 `zaoqiang-tcm-001.enabled=true` 且服务已重启 |
| 2.5.43 失败 | 检查 `deptId` / `drugId` / `drugSpecPackingId` 是否来自有效 2.1.9 + 2.5.44 回参 |
| 正式失败、灰度成功 | 确认 `active-env=prod`，且 `hospitalId`/`orgId` 与 prod 凭证成套 |

---

## 9. 代码位置（`scminterface-framework`）

```
src/main/java/com/scminterface/customer/msun/
  MsunVendorConstants.java          # 厂家 API 前缀 /api/vendor/msun
  MsunApiPaths.java                 # 科室、人员众阳路径
  support/                          # MsunSignedHttpClient、MsunSm2Util、MsunOpenApiSupport
  spd/MsunSpdApiPaths.java          # SPD 众阳路径常量
  service/
    MsunProbeService.java           # 科室/人员（共用）
    MsunSpdQueryService.java        # SPD 查询（共用）
  web/MsunHospitalListController.java
  hospital/
    MsunHospitalRegistry.java       # 医院登记册
    zaoqiangtcm/
      ZaoqiangTcmHospitalConstants.java
      config/
        ZaoqiangTcmMsunEnvProfile.java    # test/prod 凭证
        ZaoqiangTcmMsunProperties.java    # 运行时配置，实现 MsunHospitalRuntime
        ZaoqiangTcmMsunConfiguration.java
      web/
        ZaoqiangTcmMsunProbeController.java
        ZaoqiangTcmMsunSpdQueryController.java
      test/
        ZaoqiangTcmMsunProbeMain.java
        ZaoqiangTcmMsunSpdQueryProbeMain.java
        ZaoqiangTcmMsunOpenApiRunner.java
```

| 说明 | 路径 |
|------|------|
| 运行时配置 | `scminterface-admin/src/main/resources/application.yml` |
| 联调页 | `scminterface-admin/src/main/resources/static/msun-probe.html` |
| JWT 白名单 | `framework/security/JwtAuthenticationFilter.java`（`/msun-probe.html`） |

---

*文档与 `customer/msun` 包内代码同步维护。*
