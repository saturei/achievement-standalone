# 成果管理系统 — API 接口文档

> 版本: v4.1.0 | Base URL: `http://localhost:8080` | 更新日期: 2026-06-12

---

## 1. 概述

### 1.1 响应格式

所有接口统一返回 JSON。成功响应:

```json
// 单对象
{ "id": "...", "name": "..." }

// 列表
[ { "id": "...", "name": "..." }, ... ]

// 统计
{ "totalCount": 250, "preRegisterCount": 121, "registerCount": 59, "recordCount": 70 }

// 数据仓库同步
{ "O0DHU7u_count": 74, "O0DHU7u_status": "ok", "total_count": 1476, "api_calls_used": 26, "api_calls_remaining": 474 }

// 成果同步
{ "imported": 250, "updated": 6, "skipped": 0, "total": 256 }

// 数据一致性对比
{ "onlyInDingTalkCount": 0, "onlyInLocalCount": 0, "dtNormalCount": 227, "localCount": 250 }
```

错误响应:

```json
{ "error": "错误描述" }
```

### 1.2 身份认证

#### 开发环境 (sqlite/dev profile)

无需认证，所有 API 可直调。兼容旧版 `X-Current-User` header:

```
X-Current-User: admin
```

#### 生产环境 (prod profile)

采用 **JWT Bearer Token** 认证，基于钉钉 JSAPI 授权码完成登录：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/dingtalk/login` | 钉钉授权码换取 JWT Token |
| GET | `/api/auth/me` | 获取当前用户信息 |

**登录流程**:

1. 前端调用 `dd.getAuthCode()` 获取临时授权码
2. 发送 `POST /api/auth/dingtalk/login` body: `{ "authCode": "xxx" }`
3. 后端通过钉钉 API 换取用户身份，签发 JWT Token
4. 后续请求携带 `Authorization: Bearer <token>` header

**请求头**:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

`/api/auth/**` 端点以 `permitAll` 开放，其余 `/api/**` 需 JWT 认证。

---

## 2. 仪表盘 API — `/api/dashboard`

**数据来源**: 钉钉数据仓库 7 张缓存表 (`dt_*`)，通过 `POST /api/data/sync-all` 同步。

### 筛选参数

所有 Dashboard 端点均支持以下可选查询参数：

| 参数 | 类型 | 说明 |
|------|------|------|
| `department` | String | 部门筛选 |
| `organization` | String | 组织机构筛选 |
| `year` | String | 年份 |
| `quarter` | String | 季度 (一季度 / 二季度 / 三季度 / 四季度) |

默认值均为「全部」(不传即不筛选)。例如：

```
GET /api/dashboard/kpi?department=交付一分区&organization=B1&year=2026&quarter=一季度
GET /api/dashboard/product-signing?year=2025&quarter=四季度
GET /api/dashboard/revenue-monthly?department=业务方案中心
```

### 2.1 KPI 总览

```
GET /api/dashboard/kpi
```

**返回**:

```json
{
  "signingContractTotal": 9425.16,    // 签约合同总额(万元)
  "signingOrderTotal": 5456.05,       // 签约派单总额(万元)
  "revenueTotal": 8681.49,            // 确权收入总额(万元)
  "deliveryMargin": 1379.33,          // 交付毛利(万元)
  "acceptedAchievements": 57,         // 已验收成果数
  "totalAchievements": 250,           // 成果总数
  "costTotal": 168414761.74           // 成本合计
}
```

**数据来源表**:

| 指标 | SQL查询表 |
|------|----------|
| signingContractTotal | `dt_signing_contracts` SUM(signing_amount_wan) |
| signingOrderTotal | `dt_signing_orders` SUM(order_amount_wan) |
| revenueTotal | `dt_revenue_details` SUM(revenue_amount_wan) |
| deliveryMargin | `dt_revenue_details` SUM(delivery_margin) |
| acceptedAchievements | `dt_achievements` WHERE actual_accept_date IS NOT NULL |
| costTotal | `dt_department_budgets` SUM(actual_cost_total) |

---

### 2.2 产品签约排名

```
GET /api/dashboard/product-signing
```

**返回**:

```json
[
  { "product_id": "CP_0008", "product_name": "个人金融服务平台", "total_amount": 5234.5 },
  { "product_id": "CP_0012", "product_name": "企业征信系统", "total_amount": 3120.3 }
]
```

**数据来源**: `dt_signing_contracts` + `dt_signing_orders` UNION ALL, 从 `linked_package` 提取产品 ID (`CP_XXXX_YYY` → `CP_XXXX` via SUBSTR), JOIN `dt_products` 获取中文产品名, 排除空产品和 CP_99 条目, 按 total_amount DESC 排序

---

### 2.3 月度确权收入趋势

```
GET /api/dashboard/revenue-monthly
```

**返回**:

```json
[
  { "recognition_month": "2026年01月", "amount": 1200.5, "count": 45 },
  { "recognition_month": "2026年02月", "amount": 980.3, "count": 38 }
]
```

**数据来源**: `dt_revenue_details` GROUP BY recognition_month ORDER BY recognition_month

---

### 2.4 成果状态分布

```
GET /api/dashboard/achievement-status
```

**返回**:

```json
{
  "byForm": [
    { "achievement_form": "系统成果", "count": 120 },
    { "achievement_form": "文档成果", "count": 80 }
  ],
  "byOrg": [
    { "org_unit": "业务方案中心", "total": 60, "accepted": 15 },
    { "org_unit": "交付一分区", "total": 45, "accepted": 10 }
  ]
}
```

**数据来源**: `dt_achievements` GROUP BY achievement_form / org_unit

---

### 2.5 签约风险结构

```
GET /api/dashboard/signing-risk
```

**返回**:

```json
[
  { "signing_risk_level": "高", "amount": 3200.5, "count": 25 },
  { "signing_risk_level": "中", "amount": 5100.3, "count": 40 }
]
```

**数据来源**: `dt_signing_contracts` GROUP BY signing_risk_level

---

### 2.6 部门预算 vs 实际

```
GET /api/dashboard/department-budget
```

**返回**:

```json
[
  {
    "dept_name": "交付一分区",
    "dept_head": "张三",
    "budget": 5000000,
    "actual": 4800000,
    "diff": -200000,
    "execute_rate": 96.0
  }
]
```

**数据来源**: `dt_department_budgets` ORDER BY actual_cost_total DESC

---

### 2.7 产品概览

```
GET /api/dashboard/products
```

**返回**:

```json
[
  { "product_id": "CP_0001", "product_name": "个人金融服务平台", "product_department": "业务方案中心", "budget": 500, "cost": 450 }
]
```

**数据来源**: `dt_products`

---

### 2.8 表格数据端点（支持筛选）

所有表数据端点的 **列名** 为 snake_case 格式（对应 SQLite 列名）。

#### 签约合同数据

```
GET /api/dashboard/table/signing-contracts?department=交付一分区&riskLevel=高&quarter=一季度
```

| 列名 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR | PK |
| contract_id | VARCHAR | 合同编号 |
| customer_name | VARCHAR | 客户简称 |
| contract_name | VARCHAR | 合同名称 |
| signing_amount_wan | NUMERIC | 签约金额(万元) |
| accounting_type | VARCHAR | 合同核算类型 |
| signing_risk_level | VARCHAR | 签约风险等级 |
| recognition_risk_level | VARCHAR | 确权风险等级 |
| operator | VARCHAR | 经营岗 |
| department | VARCHAR | 所属部门 |
| signing_quarter | VARCHAR | 签约认定季度 |
| linked_package | VARCHAR | 关联套餐 |
| linked_opportunity | VARCHAR | 关联商机 |
| signing_date | VARCHAR | 签约日期(毫秒时间戳) |
| raw_json | TEXT | 原始JSON(调试用) |

**数据来源**: `dt_signing_contracts` (← 钉钉 O0DHU7u)

#### 签约派单数据

```
GET /api/dashboard/table/signing-orders?department=交付一分区&quarter=一季度
```

| 列名 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR | PK |
| order_id | VARCHAR | 系统订单编号 |
| contract_id | VARCHAR | 合同编号 |
| customer_name | VARCHAR | 所属客户 |
| order_amount_wan | NUMERIC | 派单金额(万元) |
| planned_recognition_amount | NUMERIC | 计划确权金额 |
| accounting_type | VARCHAR | 合同核算类型 |
| contract_status | VARCHAR | 合同状态 |
| operator | VARCHAR | 经营岗 |
| department | VARCHAR | 所属部门 |
| signing_quarter | VARCHAR | 签约认定季度 |
| linked_package | VARCHAR | 关联套餐 |
| order_create_date | VARCHAR | 订单创建日期(毫秒时间戳) |
| remark | TEXT | 备注 |

**数据来源**: `dt_signing_orders` (← 钉钉 LfxeQF7)

#### 确权明细数据

```
GET /api/dashboard/table/revenue-details?month=2026年03月&orgUnit=B1-凌辉
```

| 列名 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR | PK |
| order_id | VARCHAR | 订单编号 |
| contract_id | VARCHAR | 订单所属合同 |
| contract_name | VARCHAR | 合同名称 |
| customer_name | VARCHAR | 所属客户 |
| revenue_amount_wan | NUMERIC | 确权收入(万元) |
| confirmed_revenue | NUMERIC | 确认收入 |
| cost_carryover | NUMERIC | 结转成本 |
| delivery_margin | NUMERIC | 交付毛利 |
| recognition_month | VARCHAR | 确权认定时间 |
| profit_loss_subject | VARCHAR | 损益科目 |
| recognition_type | VARCHAR | 类型 |
| accounting_type | VARCHAR | 核算类型 |
| operator | VARCHAR | 姓名(经营岗) |
| org_unit | VARCHAR | 经营机构 |

**数据来源**: `dt_revenue_details` (← 钉钉 BRNEkOg)

#### 成果数据

```
GET /api/dashboard/table/achievements?orgUnit=业务方案中心&status=accepted
```

| 列名 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR | PK |
| achievement_name | VARCHAR | 成果名称 |
| linked_product | VARCHAR | 关联产品 |
| linked_packages | VARCHAR | 关联套餐 |
| linked_order_id | VARCHAR | 关联订单编号 |
| linked_order | VARCHAR | 关联订单 |
| linked_project | VARCHAR | 关联项目 |
| achievement_form | VARCHAR | 成果形态 |
| achievement_version | VARCHAR | 成果版本 |
| planned_accept_date | VARCHAR | 计划验收日期(毫秒时间戳) |
| actual_accept_date | VARCHAR | 实际验收日期(毫秒时间戳) |
| accept_method | VARCHAR | 验收方式 |
| acceptors | VARCHAR | 验收人 |
| department | VARCHAR | 部门 |
| org_unit | VARCHAR | 所属机构 |
| has_baseline | VARCHAR | 是否有基线 |
| app_scenario | VARCHAR | 应用场景 |
| requester | VARCHAR | 需求提出人 |
| goal_description | TEXT | 目标描述 |

**数据来源**: `dt_achievements` (← 钉钉 IjclEN7)

---

### 2.9 筛选选项

```
GET /api/dashboard/filter-options
```

**返回**:

```json
{
  "departments": [{ "department": "交付一分区" }, ...],
  "riskLevels": [{ "signing_risk_level": "高" }, ...],
  "quarters": [{ "signing_quarter": "一季度" }, ...],
  "orgUnits": [{ "org_unit": "业务方案中心" }, ...],
  "recognitionMonths": [{ "recognition_month": "2026年01月" }, ...]
}
```

**数据来源**: 所有 7 张缓存表的 DISTINCT 查询

---

## 3. 数据同步 API — `/api/data`

### 3.1 全量同步

```
POST /api/data/sync-all
```

从钉钉数据仓库拉取全部 7 张表数据到本地 SQLite 缓存。

**返回**:

```json
{
  "O0DHU7u_count": 74,
  "O0DHU7u_status": "ok",
  "LfxeQF7_count": 341,
  "LfxeQF7_status": "ok",
  "BRNEkOg_count": 650,
  "BRNEkOg_status": "ok",
  "IjclEN7_count": 251,
  "IjclEN7_status": "ok",
  "SXS7oOg_count": 12,
  "SXS7oOg_status": "ok",
  "Jwe8QNe_count": 105,
  "Jwe8QNe_status": "ok",
  "drgfZh4_count": 43,
  "drgfZh4_status": "ok",
  "total_count": 1476,
  "api_calls_used": 26,
  "api_calls_remaining": 474
}
```

**API消耗**: ~26 次/次 (7次 fields + ~19次 records 分页)
**月限制**: 500 次，超 85% 时返回 `warning`

### 3.2 同步状态

```
GET /api/data/sync-status
```

```json
{
  "signing_contract": 74,
  "signing_order": 341,
  "revenue_detail": 650,
  "achievement": 251,
  "product": 12,
  "product_package": 105,
  "department_budget": 43,
  "api_calls_used": 26,
  "api_calls_remaining": 474,
  "monthly_limit": 500
}
```

---

## 4. 成果管理 API — `/api/achievements`

### 4.1 成果列表

```
GET /api/achievements?keyword=意图&departmentName=业务方案中心&organizationNames=B1&status=RECORDED&productId=CP_0008&page=1&pageSize=20
```

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 模糊搜索 (名称/描述/ID) |
| departmentName | String | 否 | 部门筛选 |
| organizationNames | String | 否 | 机构筛选 (逗号分隔多机构) |
| status | String | 否 | 状态码: pre_register / register / recorded / offline / deleted |
| productId | String | 否 | 产品ID筛选 |
| page | int | 否 | 页码, 默认1 |
| pageSize | int | 否 | 每页大小, 默认20 |
| includeDeleted | Boolean | 否 | 是否包含已删除记录, 默认false |

**数据来源**: `achievements` 表

### 4.2 成果统计

```
GET /api/achievements/statistics?keyword=意图&departmentName=业务方案中心&organizationNames=B1&status=register&productId=CP_0008
```

**参数**: 同 4.1 成果列表的筛选参数（keyword, departmentName, organizationNames, status, productId, includeDeleted）。有筛选时返回筛选后数据的统计，无筛选时返回全局统计。

**返回**:

```json
{
  "totalCount": 250,
  "preRegisterCount": 121,
  "registerCount": 59,
  "recordCount": 70,
  "offlineCount": 0,
  "byStatus": {
    "pre_register": 121,
    "register": 59,
    "recorded": 70,
    "offline": 0
  },
  "byType": { "系统成果": 180, "文档成果": 70 },
  "byProduct": { "CP_0008": 98, "CP_0012": 48 }
}
```

**设计要点**: 统计接口接收与列表接口相同的筛选参数，确保页面顶部统计数字与表格数据一致。筛选条件变化时前端同步刷新。

### 4.3 成果详情

```
GET /api/achievements/{achievementId}
```

### 4.4 成果预注册

```
POST /api/achievements/pre-register
Content-Type: application/json

{
  "name": "新成果名称",
  "productId": "CP_0001",
  "productName": "产品名",
  "departmentId": "D001",
  ...
}
```

### 4.5 成果状态流转

| 端点 | 说明 |
|------|------|
| `POST /api/achievements/{id}/register` | 预注册 → 已注册 |
| `POST /api/achievements/{id}/record` | 已注册 → 已登记 |
| `POST /api/achievements/{id}/change` | 成果变更(更新字段) |
| `PUT /api/achievements/{id}/offline` | 下架 |
| `PUT /api/achievements/{id}/online` | 重新上架 |
| `PUT /api/achievements/{id}/delete` | 软删除 |

### 4.6 版本历史

```
GET /api/achievements/{achievementId}/history
```

**数据来源**: `achievement_version_records` 表

### 4.7 筛选选项

```
GET /api/achievements/organizations
GET /api/achievements/departments
GET /api/achievements/owners
GET /api/achievements/types
GET /api/achievements/filter-options
```

### 4.8 从钉钉同步成果

```
POST /api/achievements/sync-from-dingtalk
```

从钉钉 AI 表格 sheet `CiOtnAu`（产品成果明细登记薄，29字段）拉取全部成果记录，按 name+productId 执行全量 upsert 导入本地 `achievements` 表。

**返回**:

```json
{
  "imported": 250,      // 新增导入数
  "updated": 6,         // 字段差异更新数
  "skipped": 0,         // 无变化跳过数
  "total": 256          // 钉钉总记录数 (含去重/已变更)
}
```

**处理逻辑 (v4.0 全量 Upsert)**:

每条钉钉记录按 `成果名称 + 关联产品` 查找本地记录：
- **不存在** → 新增导入，状态取自钉钉的"成果状态"字段
- **存在** → 全字段 diff（含 status），有差异则更新

| 钉钉状态 | 目标状态 | 已存在时行为 | 不存在时行为 |
|---------|---------|------------|------------|
| 预注册 | PRE_REGISTER | 全字段 diff+更新 | import |
| 注册 | REGISTER | 全字段 diff+更新 | import |
| 登记 | RECORDED | 全字段 diff+更新 | import |
| **已变更** | **PRE_REGISTER (回退)** | **全字段 diff+更新 + 版本记录** | import |
| 下架 | OFFLINE | 全字段 diff+更新 | import |
| 删除 | DELETED | skip | skip |

**状态映射**:

| 钉钉状态值 | 枚举 |
|-----------|------|
| 预注册 | PRE_REGISTER |
| 已注册 / 注册 | REGISTER |
| 已登记 / 登记 | RECORDED |
| 已下架 / 下架 | OFFLINE |
| 已删除 / 删除 | DELETED |
| 已变更 / CHANGED | null (变更标记, 回退为 PRE_REGISTER) |

**status 变更附加操作**: 状态变更时自动设置对应时间戳 (registerTime / recordTime / preRegisterTime)，仅当原值为 null 时设置。

**已变更条目处理**:
1. 全字段 diff (含 status)
2. 有差异 → 更新字段、递增 `changeVersion`、保存 `AchievementVersionRecord`
3. 无差异 → skip

**钉钉未配置时**: 返回 HTTP 400: `{error: "钉钉未配置"}`

**数据来源**: 钉钉 sheet `CiOtnAu` → `achievements` + `achievement_version_records`

### 4.9 钉钉数据一致性对比

```
GET /api/achievements/sync-diff
```

对比钉钉源数据与本地数据，返回差异分析。

**返回**:

```json
{
  "onlyInDingTalk": [],
  "onlyInLocal": [],
  "onlyInDingTalkCount": 0,
  "onlyInLocalCount": 0,
  "dtNormalCount": 227,
  "localCount": 250
}
```

| 字段 | 说明 |
|------|------|
| onlyInDingTalk | 钉钉有但本地无的记录 |
| onlyInLocal | 本地有但钉钉正常记录中无的记录 |
| dtNormalCount | 钉钉正常记录去重后数量 |
| localCount | 本地去重后数量 |

> 注意: 对比时排除钉钉中"已变更"状态的记录，仅比较正常状态记录。

### 4.10 钉钉配置状态

```
GET /api/detail/dingtalk-status
```

```json
{ "configured": true }
```

---

## 5. 目标统计 API — `/api/targets`

### 5.1 目标统计

```
GET /api/targets/statistics?year=2026&organization=B1
```

**数据来源**: `targets` 表 + 明细表聚合

### 5.2 季度汇总

```
GET /api/targets/quarterly-summary?year=2026
```

### 5.3 目标 CRUD

| 端点 | 说明 |
|------|------|
| `POST /api/targets` | 新增目标 |
| `PUT /api/targets/{id}` | 更新目标 |
| `DELETE /api/targets/{id}` | 删除目标 |

### 5.4 实际数据

```
POST /api/targets/actual
Content-Type: application/json

{ "targetId": "...", "organization": "...", "dataType": "signing", "year": 2026, "month": 3, "actualValue": 500 }
```

**数据来源**: `actual_data` 表

### 5.5 导入目标

```
POST /api/targets/import
Content-Type: application/json

{ "items": [...] }
```

---

## 6. 明细管理 API — `/api/detail`

### 6.1 签约明细

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/detail/signings` | GET | 查询签约明细 (`?organization=&signMonth=`) |
| `/api/detail/signings` | POST | 新增签约记录 |
| `/api/detail/signings/{id}` | PUT | 更新签约记录 |
| `/api/detail/signings/{id}` | DELETE | 删除签约记录 |
| `/api/detail/signings/batch` | POST | 批量新增 |
| `/api/detail/signings/sync-from-dingtalk` | POST | 从钉钉同步签约明细 |
| `/api/detail/signings/migrate-subcategory` | POST | 迁移签约子分类 |

### 6.2 确权明细

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/detail/recognitions` | GET | 查询确权明细 (`?organization=&recognitionMonth=`) |
| `/api/detail/recognitions` | POST | 新增确权记录 |
| `/api/detail/recognitions/{id}` | PUT | 更新确权记录 |
| `/api/detail/recognitions/{id}` | DELETE | 删除确权记录 |
| `/api/detail/recognitions/batch` | POST | 批量新增 |
| `/api/detail/recognitions/sync-from-dingtalk` | POST | 从钉钉同步确权明细 |

---

## 7. 用户管理 API — `/api/users`

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/users` | GET | 启用用户列表 (含内置admin) |
| `/api/users/config` | GET | 全部用户配置 |
| `/api/users` | POST | 新增用户 |
| `/api/users/{id}` | PUT | 更新用户 |
| `/api/users/{id}` | DELETE | 删除用户 |

**数据来源**: `user_configs` 表

**角色枚举**:

| 角色 | 权限 |
|------|------|
| ADMIN | 全部权限 |
| DEPT_LEADER | 仅本部门数据 |
| ORG_LEADER | 仅本机构数据 |
| USER | 只读 |

---

## 8. 数据导入 API — `/api/data`

数据导入暂未实现独立的 Excel 文件导入接口。当前数据通过钉钉同步方式获取：

- **Dashboard 缓存数据**: `POST /api/data/sync-all` 从钉钉数据仓库拉取 7 张表
- **目标 (targets) 数据**: `DingTalkDataService` 在同步后自动从 `dt_signing_contracts`/`dt_signing_orders`/`dt_achievements`/`dt_department_budgets` 聚合生成，不再依赖 Excel 导入

---

## 9. 钉钉网关 API — FastAPI (port 8000, 可选)

辅助开发/调试用途的可选组件，生产环境 Java 直连钉钉 API，无需启动此服务。仅在需要直接查看钉钉原始数据时使用。

```
GET  /api/table/list                   # 列出配置的所有表格
GET  /api/table/{id}/data?max_results=100   # 读取表格数据
GET  /api/table/{id}/schema            # 读取字段结构
POST /api/table/{id}/query             # 带筛选条件查询
```

### 查询请求体

```json
{
  "conditions": [
    { "field": "所属机构", "operator": "contain", "value": "业务方案" }
  ],
  "max_results": 100
}
```

**支持的 operator**: equal / notEqual / contain / notContain / greater / less / empty / notEmpty

---

## 10. 数据来源汇总

### 10.1 数据仓库→缓存表映射（仪表盘用）

| 钉钉表格(sheetId) | 缓存表 | 同步字段数 | 记录数 |
|-------------------|--------|:---------:|------|
| O0DHU7u 签约明细表-合同(周同步) | `dt_signing_contracts` | 16 | 74 |
| LfxeQF7 签约明细表(派单中风险)(周同步) | `dt_signing_orders` | 14 | 341 |
| BRNEkOg 确权明细 | `dt_revenue_details` | 15 | 650 |
| IjclEN7 成果登记薄-2604版 | `dt_achievements` | 19 | 251 |
| SXS7oOg 产品基础信息 | `dt_products` | 9 | 12 |
| Jwe8QNe 2026年产品套餐明细 | `dt_product_packages` | 10 | 105 |
| drgfZh4 产品部门表 | `dt_department_budgets` | 16 | 43 |

### 10.2 钉钉→业务表直接同步映射

| 钉钉表格(sheetId) | 业务表 | 同步字段数 | 端点 |
|-------------------|--------|:---------:|------|
| **CiOtnAu 产品成果明细登记薄** | `achievements` + `achievement_version_records` | 29 | `POST /api/achievements/sync-from-dingtalk` |
| 签约明细 (O0DHU7u) | `contract_signings` | 16 | `POST /api/detail/signings/sync-from-dingtalk` |
| 确权明细 (BRNEkOg) | `revenue_recognitions` | 16 | `POST /api/detail/recognitions/sync-from-dingtalk` |

### 10.3 数据仓库其他未接入表 (32张)

```
1.部门产品预算进度周报表    6.产品销售进展视图表    11.附录1：月度订单费用同步表    ...
2.产品人员投入周报表        7.套餐签约情况表        12.附录2：周度订单费用明细      ...
3.研发项目执行进展月报表     8.2026年产品套餐登记表   13.附录3：当年存量周数据        ...
4.研发订单执行月度明细视图   9.2026年产品套餐明细     14.附录4：2026年月度财务成本明细表 ...
5.产品签约信息表(Rjz8jU5)  10.产品月度签约金额明细  15.附录5：2026周度报销明细表     ...
```

详见 [生产数据跟踪管理平台-技术方案](./生产数据跟踪管理平台-技术方案.md) 第1.3节。

---

## 11. 钉钉API调用控制

| 项目 | 值 |
|------|-----|
| 月上限 | 500 次 |
| 单次数据仓库全量同步 | ~26 次 (7 fields + ~19 分页) |
| 单次成果同步 (CiOtnAu) | ~5 次 (1 fields + ~4 分页, 256条) |
| 单次 sync-diff 对比 | ~5 次 (同成果同步) |
| 预警阈值 | 425 次 (85%) |
| 计数器 | `DingTalkClient.callCounter` (AtomicInteger) |
| 监控端点 | `GET /api/data/sync-status` |
| 当前部署配置 | `application-sqlite.yml` (dingtalk.*) |
| 数据仓库 baseId | `y20BglGWO2y56y7nC0aDvlb08A7depqY` |
| 成果表 baseId | `7dx2rn0JbY4y54XwI2gz73NMVMGjLRb3` (含 CiOtnAu) |
| 成果表 sheetId | `CiOtnAu` (产品成果明细登记薄, 29字段) |
