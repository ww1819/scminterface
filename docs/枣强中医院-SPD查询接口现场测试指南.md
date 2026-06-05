# 枣强中医院 SPD 查询接口现场测试指南

> **租户**：`zaoqiang-tcm-001`  
> **依据文档**：`接口文档2.docx`（枣强县中医院）  
> **本阶段**：仅查询、查看回参，**不落库**；推送接口 2.5.41/2.5.42 **未实现调用**

---

## 1. 接口文档2 核对结果

| 您列出的接口 | 文档2中是否存在 | 方法 | 路径 | 实现状态 |
|-------------|----------------|------|------|----------|
| 2.5.44 药品、材料字典查询 | ✅ 有 | GET | `/msun-middle-base-resource/v1/drug-dict-infos` | 已实现探针 |
| 2.5.58 SPD 药品材料分类字典 | ✅ 有 | GET | `/msun-middle-base-dict/v1/dict-category` | 已实现探针 |
| 2.5.62 SPD 供应商查询 | ✅ 有 | GET | `/msun-middle-base-dict/v1/drug-supplieres` | 已实现探针 |
| 2.5.63 SPD 生产厂商查询 | ✅ 有 | GET | `/msun-middle-base-dict/v1/drug-produceres` | 已实现探针 |
| 2.5.43 药房批次库存 | ✅ 有 | GET | `/msun-middle-base-resource/v1/drug-batch-stocks` | 已实现探针 |
| 2.5.102 一级库入退库记录 | ✅ 有 | POST | `/msun-middle-base-resource/v1/query-yk-instock` | 已实现探针 |
| 2.5.41 药品材料入库（推送） | ✅ 有 | POST | `/msun-middle-base-resource/v1/drug-stocks-new` | **未实现**（避免写库） |
| 2.5.42 药品材料退库（推送） | ✅ 有 | POST | `/msun-middle-base-resource/v1/drug-stocks-new/d` | **未实现**（避免写库） |

**结论**：您列出的 8 个接口在文档2中**均有完整说明**；其中 6 个查询接口已提供 HTTP 探针与本地 main 联调代码。

---

## 2. 环境与开关

与科室/人员探针相同：

```yaml
scminterface:
  customer:
    zaoqiang-tcm-001:
      msun:
        enabled: true       # 已默认启用
        active-env: prod    # 已切换枣强正式库；灰度联调可改 test
```

## 2.1 可视化联调页（推荐）

启动 scminterface 后浏览器访问：

`http://<前置机IP>:8088/zaoqiang-msun-probe.html`

（需先登录 `admin` / `admin123`，主页「枣强众阳联调」入口亦可进入。）

页面左侧可编辑各接口参数，右侧实时显示 JSON 回参，便于后续字段映射开发。

凭证见代码：`ZaoqiangTcmMsunEnvProfile.java`（勿在现场随意改密钥）。

---

## 3. 方式一：本地 main 联调（推荐）

### 运行

| IDE 配置 | 环境 |
|----------|------|
| **枣强SPD查询探针-测试库** | 灰度 test |
| **枣强SPD查询探针-正式库** | 正式 prod |

主类：`com.scminterface.customer.zaoqiangTcm.msun.test.ZaoqiangTcmMsunSpdQueryProbeMain`

### 自动执行顺序

1. **2.5.44** — `limitCount=5`，`materialOrDrug=0`（药品）
2. **2.5.58** — `keyWord=西药`，`limitCount=10`
3. **2.5.62** — 供应商，`limitCount=5`
4. **2.5.63** — 生产厂商，`limitCount=5`
5. **2.5.43** — 若 2.5.44 与 2.1.9 科室能取到 `drugId/deptId/drugSpecPackingId` 则自动调用，否则打印跳过说明
6. **2.5.102** — 最近 7 天 `startTime`/`endTime`

控制台输出格式化 JSON 及 `success/code/message`。

### 命令行

```powershell
cd <scminterface 根目录>
mvn install -pl scminterface-common,scminterface-framework -am -DskipTests
mvn -pl scminterface-framework org.codehaus.mojo:exec-maven-plugin:3.1.0:java `
  "-Dexec.mainClass=com.scminterface.customer.zaoqiangTcm.msun.test.ZaoqiangTcmMsunSpdQueryProbeMain"
```

---

## 4. 方式二：HTTP 探针（需 Token）

基址：`http://<IP>:8088/api/customer/zaoqiang-tcm-001/msun/spd/query`

先 `POST /login` 获取 Token（`admin` / `admin123`），Header：`Authorization: Bearer <token>`

| 接口 | 方法 | 路径 | 主要参数 |
|------|------|------|----------|
| 2.5.44 | GET | `/drug-dict-infos` | `limitCount`、`materialOrDrug`(0药品1材料)、`drugCode`、`drugName` |
| 2.5.58 | GET | `/dict-category` | `keyWord`、`limitCount` |
| 2.5.62 | GET | `/drug-suppliers` | `keyWord`、`limitCount`、`materialOrDrug` |
| 2.5.63 | GET | `/drug-producers` | `keyWord`、`limitCount`、`materialOrDrug` |
| 2.5.43 | GET | `/drug-batch-stocks` | **必填** `deptId`、`drugId`、`drugSpecPackingId` |
| 2.5.102 | POST | `/yk-instock` | **必填** `startTime`、`endTime`；可选 `deptId`、`type`(0入库1退库) |

### 示例

```http
GET /api/customer/zaoqiang-tcm-001/msun/spd/query/drug-dict-infos?limitCount=5&materialOrDrug=0
Authorization: Bearer <token>
```

```http
POST /api/customer/zaoqiang-tcm-001/msun/spd/query/yk-instock
Content-Type: application/json
Authorization: Bearer <token>

{
  "startTime": "2026-05-01 00:00:00",
  "endTime": "2026-06-05 23:59:59",
  "type": "0"
}
```

```http
GET /api/customer/zaoqiang-tcm-001/msun/spd/query/drug-batch-stocks?deptId=847&drugId=5773375049433221252&drugSpecPackingId=5773375059029788801
```

（批次库存示例 ID 来自文档2灰度示例，正式环境请用 2.5.44 回参替换。）

---

## 5. 参数依赖说明（现场易错点）

| 接口 | 说明 |
|------|------|
| 2.5.43 | `deptId` 来自 **2.1.9 科室**；`drugId`、`drugSpecPackingId` 来自 **2.5.44 字典**，三者须为众阳系统内有效对照 |
| 2.5.102 | `startTime`、`endTime` 格式 `yyyy-MM-dd HH:mm:ss`，必填 |
| 2.5.62/63 | 文档中 `materialOrDrug` 材料取值存在 1/2 表述差异，以众阳在线文档或回参为准 |
| 2.5.58 | 请求参数名为 `keyWord`（文档表格偶有 `Keyword` 写法，以示例 URL 为准） |

---

## 6. 判定与排错

| 现象 | 处理 |
|------|------|
| `success: true`，`code: 0000` | 正常，保存 `data.hisBody` 样例 JSON |
| `openapi@9984` | appId 未授权该接口，联系众阳开通 SPD 查询分组 |
| 2.5.43 参数错误 | 用 2.5.44 返回的 `drugId` 与 `drugSpecPackingId` 成对传入 |
| 401 | 重新登录获取 Token |

---

## 7. 代码位置

| 说明 | 路径 |
|------|------|
| 接口路径常量 | `msun/spd/ZaoqiangTcmMsunSpdApiPaths.java` |
| HTTP 探针 | `msun/web/ZaoqiangTcmMsunSpdQueryController.java` |
| 业务调用 | `msun/service/ZaoqiangTcmMsunSpdQueryService.java` |
| 本地 main | `msun/test/ZaoqiangTcmMsunSpdQueryProbeMain.java` |

---

## 8. 后续（推送接口）

文档2 中 **2.5.41 入库**、**2.5.42 退库** 为 POST 写库类接口，需在查询联调与字段映射确认后再开发，并需众阳提供 `loginUser` 等写操作头信息。

---

*文档与 scminterface 枣强 SPD 查询探针代码同步维护。*
