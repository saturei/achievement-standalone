# 成果管理模块 API 接口文档

## 基础信息

- **Base URL**: `/api`
- **认证方式**: Spring Security (Basic Auth / 开发环境自动生成密码)；权限控制通过 `X-Current-User` 请求头
- **数据格式**: JSON
- **后端端口**: 8080
- **内网访问**: 后端 `http://{IP}:8080/`，前端 `http://{IP}:3001/`（开发模式）

---

## 一、成果管理接口

### 1. 获取成果列表

**GET** `/api/achievements`

获取成果列表，支持分页和多条件级联筛选。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认20 |
| keyword | string | 否 | 关键词搜索（匹配名称、描述、ID） |
| departmentName | string | 否 | 部门名称筛选 |
| organizationNames | string | 否 | 机构名称多选，逗号分隔 |
| status | string | 否 | 状态筛选：pre_register, register, recorded, offline, deleted |
| productId | string | 否 | 产品ID筛选 |
| includeDeleted | bool | 否 | 是否包含已删除记录，默认false |

#### 响应示例

```json
{
  "total": 249,
  "page": 1,
  "pageSize": 20,
  "items": [
    {
      "id": "ACH_64090B1E",
      "name": "成果名称",
      "version": "V1.0.0",
      "changeVersion": "V1.0.1",
      "productId": "CP_0004",
      "productName": null,
      "type": null,
      "description": "成果描述",
      "status": "REGISTER",
      "owner": "彭楫洲",
      "departmentName": "业务方案中心",
      "organizationName": "业务方案中风险机构",
      "achievementForm": "方案成果",
      "createdAt": "2026-04-28T18:33:29.656",
      "updatedAt": "2026-05-07T10:31:26.003"
    }
  ]
}
```

> **筛选特性**：所有筛选条件为 AND 关系。选择产品后会级联缩小部门和机构下拉范围。

---

### 2. 获取成果统计

**GET** `/api/achievements/statistics`

```json
{
  "totalCount": 249,
  "preRegisterCount": 20,
  "registerCount": 180,
  "recordCount": 40,
  "offlineCount": 9,
  "byStatus": { "pre_register": 20, "register": 180, "recorded": 40, "offline": 9 },
  "byType": { "模块": 50, "功能": 30 },
  "byProduct": { "CP_0001": 30, "CP_0007": 45 }
}
```

---

### 3. 获取选项列表（级联筛选用）

**GET** `/api/achievements/filter-options`

根据当前已选条件，返回可用的部门和机构下拉选项。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | string | 否 | 当前关键词 |
| departmentName | string | 否 | 当前部门 |
| organizationNames | string | 否 | 当前机构（逗号分隔） |
| status | string | 否 | 当前状态 |
| productId | string | 否 | 当前产品ID |

#### 响应示例

```json
{
  "departments": ["AI方案中心", "FM平台中心", "业务方案中心", "数据中心", "能力中心"],
  "organizations": ["AI场景研发机构", "AI技术底座机构", "..."]
}
```

> 选择 `productId=CP_0007`（Finmall平台）后，返回 `departments: ["FM平台中心"]`, `organizations: ["生产工具机构"]`

---

### 4. 获取部门/机构/负责人/类型列表

| 端点 | 说明 |
|------|------|
| **GET** `/api/achievements/departments` | 获取所有去重部门列表 |
| **GET** `/api/achievements/organizations` | 获取所有去重机构列表 |
| **GET** `/api/achievements/owners` | 获取所有去重负责人列表 |
| **GET** `/api/achievements/types` | 获取所有去重类型列表 |

---

### 5. 获取成果详情

**GET** `/api/achievements/{achievementId}`

---

### 6. 成果预注册

**POST** `/api/achievements/pre-register`

```json
{
  "name": "成果名称",
  "version": "V1.0.0",
  "productId": "CP_0001",
  "productExternalVersion": "V1.0.0",
  "organizationId": "ORG001",
  "organizationName": "机构名称",
  "departmentId": "DEPT001",
  "departmentName": "部门名称",
  "hasBaseline": "无基线",
  "requirementProposer": "需求提出人",
  "achievementForm": "成果形态",
  "saleType": "可售类型",
  "applicationScenario": "应用场景",
  "type": "模块",
  "description": "成果描述",
  "achievementTarget": "成果目标",
  "plannedAcceptanceDate": "2024-06-30",
  "acceptanceMethod": "验收方式",
  "acceptor": "验收人",
  "relatedProjectId": "PROJ001",
  "relatedProjectName": "项目名称",
  "relatedOrderId": "ORD001",
  "relatedOrderName": "订单名称",
  "acceptanceRequirements": "验收要求",
  "owner": "负责人",
  "riskTags": ["风险标签1", "风险标签2"]
}
```

---

### 7. 成果注册

**POST** `/api/achievements/{achievementId}/register`

状态从 PRE_REGISTER → REGISTER。

```json
{
  "registerTime": "2024-03-01T00:00:00",
  "registerNotes": "注册备注",
  "version": "V1.0.0",
  "achievementTarget": "成果目标",
  "plannedAcceptanceDate": "2024-06-30",
  "estimatedAcceptanceMonth": "2024-06",
  "acceptanceMethod": "验收方式",
  "acceptor": "验收人",
  "acceptanceOrganization": "验收机构",
  "acceptanceRequirements": "验收要求",
  "functionListFile": "功能清单文件",
  "packageIds": "套餐ID"
}
```

---

### 8. 成果登记

**POST** `/api/achievements/{achievementId}/record`

状态从 REGISTER → RECORDED。

```json
{
  "recordTime": "2024-06-01T00:00:00",
  "recordNotes": "登记备注",
  "version": "V1.0.0",
  "actualAcceptanceDate": "2024-05-30",
  "demoUrl": "https://demo.example.com",
  "codeRepositoryUrl": "https://git.example.com/repo",
  "deliverables": "交付物说明"
}
```

---

### 9. 成果变更

**POST** `/api/achievements/{achievementId}/change`

对成果进行变更，生成新变更版本号（如 V1.0.0 → V1.0.1）。

```json
{
  "changeDescription": "变更说明",
  "description": "新描述",
  "owner": "新负责人",
  "relatedProjectId": "PROJ002",
  "relatedOrderId": "ORD002",
  "acceptanceRequirements": "新验收要求",
  "registerTime": "2024-04-01T00:00:00",
  "riskTags": ["延期"],
  "productExternalVersion": "V1.1.0",
  "moduleId": "M002",
  "moduleName": "新模块名称",
  "version": "V1.1.0"
}
```

---

### 10. 成果下架

**PUT** `/api/achievements/{achievementId}/offline`

状态从 RECORDED → OFFLINE。

```json
{ "offlineReason": "下架原因" }
```

---

### 11. 成果上架

**PUT** `/api/achievements/{achievementId}/online`

状态从 OFFLINE → RECORDED。

```json
{ "reason": "上架原因" }
```

---

### 12. 成果删除

**PUT** `/api/achievements/{achievementId}/delete`

逻辑删除（仅 PRE_REGISTER 状态可删）。

---

### 13. 获取成果版本变更历史

**GET** `/api/achievements/{achievementId}/history`

---

## 二、目标统计接口

### 1. 获取目标统计数据

**GET** `/api/targets/statistics`

返回目标统计卡片数据 + 表格数据。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| dimension | string | 否 | 统计维度，默认 organization |
| year | int | 是 | 年度，如 2026 |
| product | string | 否 | 部门筛选 |
| organization | string | 否 | 机构筛选（多选逗号分隔） |
| owner | string | 否 | 负责人筛选 |
| subCategory | string | 否 | 细分目标筛选 |

#### 响应

包含 `statistics`（卡片汇总）、`summary`（顶部四个卡片数据）、`rdActual`、`rdPlanned`、`rdTarget` 以及表格用 `DimensionStatistics` 列表。

---

### 2. 获取月度分布数据

**GET** `/api/targets/distribution?year=2026`

用于渲染研发成果月度分布图表。

---

### 3. 获取季度汇总

**GET** `/api/targets/quarterly-summary?year=2026`

用于签约/确权/预算的季度对比柱状图。

---

### 4. 目标 CRUD

| 方法 | 端点 | 说明 |
|------|------|------|
| **POST** | `/api/targets` | 创建目标 |
| **PUT** | `/api/targets/{id}` | 更新目标 |
| **DELETE** | `/api/targets/{id}` | 删除目标 |

---

### 5. 筛选选项

| 端点 | 说明 |
|------|------|
| **GET** `/api/targets/products` | 获取部门列表 |
| **GET** `/api/targets/organizations` | 获取机构列表 |
| **GET** `/api/targets/owners` | 获取负责人列表 |

---

## 三、成果状态流转

```
预注册 (PRE_REGISTER)
    ├── 注册 (REGISTER)   - POST /api/achievements/{id}/register
    │       └── 登记 (RECORDED) - POST /api/achievements/{id}/record
    │               ├── 下架 (OFFLINE) - PUT /api/achievements/{id}/offline
    │               │       └── 上架 (RECORDED) - PUT /api/achievements/{id}/online
    │               └── 变更           - POST /api/achievements/{id}/change
    └── 删除 (DELETED)   - PUT /api/achievements/{id}/delete
```

---

## 四、状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 五、签约明细接口

> 签约明细是目标统计中「签约实际值」的数据来源。录入后系统自动根据签约风险等级推导细分目标（高→签约收入（高）、中→签约收入（中））。

### 1. 获取签约明细列表

**GET** `/api/detail/signings`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| organization | String | 否 | 所属机构筛选 |
| signMonth | String | 否 | 签约归属月筛选，YYYY-MM 格式 |

响应：`List<ContractSigning>`

### 2. 新增签约明细

**POST** `/api/detail/signings`

请求体字段：`contractId`、`contractName`、`orderId`、`leadId`、`packageId`、`customerName`、`amount`(元)、`accountingType`、`signingRiskLevel`(高/中/低)、`recognitionRiskLevel`、`operator`、`remark`、`region`、`productId`、`organization`、`signMonth`

### 3. 编辑签约明细

**PUT** `/api/detail/signings/{id}`

### 4. 删除签约明细

**DELETE** `/api/detail/signings/{id}`

### 5. 批量导入签约明细

**POST** `/api/detail/signings/batch`

请求体：`List<ContractSigning>`。按 `contractId` + `packageId` 去重，返回 `{ count, skipped, total }`。

### 6. 签约明细细分目标迁移

**POST** `/api/detail/signings/migrate-subcategory`

为存量记录根据 `signingRiskLevel` 批量补充 `subCategory` 字段。

---

## 六、确权/收入明细接口

> 确权明细是目标统计中「确权实际值」的数据来源。**确权实际值取自 `revenue_amount`（含税收入金额）**，非 `recognition_amount`（税前确权金额）。根据确权风险等级推导细分目标（高→确权收入（高）、中→确权收入（中））。

### 1. 获取确权明细列表

**GET** `/api/detail/recognitions`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| organization | String | 否 | 所属机构筛选 |
| recognitionMonth | String | 否 | 确权归属月筛选，YYYY-MM 格式 |

### 2. 新增确权明细

**POST** `/api/detail/recognitions`

请求体字段：`orderId`、`contractId`、`contractName`、`recognitionAmount`(确权金额,元)、`revenueAmount`(收入金额,含税,元)、`recognitionMonth`、`recognitionRiskLevel`(高/中/低)、`customerName`、`operator`、`remark`、`organization`

### 3. 编辑确权明细

**PUT** `/api/detail/recognitions/{id}`

### 4. 删除确权明细

**DELETE** `/api/detail/recognitions/{id}`

### 5. 批量导入确权明细

**POST** `/api/detail/recognitions/batch`

请求体：`List<RevenueRecognition>`。按 `contractId` + `orderId` 去重。

### 6. 确权明细细分目标迁移

**POST** `/api/detail/recognitions/migrate-subcategory`

为存量记录补充 `recognitionRiskLevel` 和 `subCategory`（空风险等级默认设为"高"）。

### 7. 机构名称批量修正

**POST** `/api/detail/migrate/fix-org-name`

将「AI方案研发机构」统一修正为「AI方案设计机构」，覆盖 `contract_signings`、`revenue_recognitions`、`targets` 三张表。

---

## 七、用户管理接口

> 权限控制通过 `X-Current-User` 请求头实现。前端从用户下拉选择当前身份，请求头自动注入用户名，后端 `UserContextFilter` 解析并存入 `ThreadLocal`。Service 层根据用户角色过滤数据。

### 1. 获取启用的用户列表

**GET** `/api/users`

响应自动注入管理员账号（username=admin, role=ADMIN）。

### 2. 获取全部用户配置

**GET** `/api/users/config`

### 3. 新增用户

**POST** `/api/users`

| 字段 | 说明 |
|------|------|
| username | 用户名，唯一 |
| displayName | 显示名称 |
| role | 角色：ADMIN / DEPT_LEADER / ORG_LEADER / USER |
| department | 所属部门（DEPT_LEADER / ORG_LEADER 必填） |
| organization | 所属机构（ORG_LEADER 必填） |
| enabled | 启用状态 (1/0) |

### 4. 编辑用户

**PUT** `/api/users/{id}`

### 5. 删除用户

**DELETE** `/api/users/{id}`

### 权限粒度

| 角色 | 可见范围 |
|------|----------|
| ADMIN | 全量数据 |
| DEPT_LEADER | 本部门及其下属机构 |
| ORG_LEADER | 仅本机构 |
| USER | 自己作为 owner 的行 |

---

## 八、目标统计与明细联动

目标统计页的「签约实际值」和「确权实际值」的数据来源：

| 统计指标 | 优先数据源 | 回退数据源 |
|----------|-----------|-----------|
| 签约实际值 | `contract_signings.amount` 按 机构+年度+季度+细分目标 聚合 | targets.q1~q4_actual |
| 确权实际值 | `revenue_recognitions.revenue_amount`（含税）按 机构+年度+季度+细分目标 聚合 | targets.q1~q4_actual |
| 费用实际值 | targets.q1~q4_actual（手工编辑） | — |
| 研发成果实际 | achievements 表实时统计 | — |
