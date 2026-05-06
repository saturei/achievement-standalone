# 成果管理系统 — 架构文档

> 版本: 1.0.0 | 最后更新: 2026-04-30

---

## 目录

1. [项目概览](#1-项目概览)
2. [数据库结构](#2-数据库结构)
3. [后端 API 文档](#3-后端-api-文档)
4. [前端-后端通讯](#4-前端-后端通讯)
5. [前端参数表](#5-前端参数表)
6. [枚举与常量](#6-枚举与常量)

---

## 1. 项目概览

### 1.1 技术栈

| 层级 | 技术 |
|---|---|
| **前端** | Vue 3 (Composition API) + Vite 5 + Element Plus |
| **后端** | Spring Boot 2.7.18 + JPA + JDBC Template |
| **数据库** | 开发: SQLite / 测试: H2 / 生产: MySQL 8 |
| **构建工具** | Maven (后端) / npm (前端) |

### 1.2 启动方式

```bash
# 后端 (sqlite 本地模式)
cd java-backend && mvn spring-boot:run -Dspring-boot.run.profiles=sqlite

# 前端
cd frontend-vue && npm run dev
```

- **前端地址**: http://localhost:3000
- **后端地址**: http://localhost:8080
- **代理转发**: 前端 Vite proxy 将 `/api/*` 请求转发到 `localhost:8080`

---

## 2. 数据库结构

> 共 6 张表，所有实体主键均为手动赋值的 String UUID，无自增主键。

### 2.1 表关系概览

```
achievements (成果主表)
  ├── achievement_status_records (状态变更记录) — achievement_id -> id
  ├── achievement_version_records (版本变更记录) — achievement_id -> id
  └── products (产品) — product_id -> id

targets (目标表)
  └── actual_data (实际数据) — target_id -> id
```

### 2.2 achievements — 成果主表

| 列名 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(200) PK | UUID 主键 |
| product_id | VARCHAR(50) NOT NULL | 产品ID |
| product_name | VARCHAR(200) | 产品名称 |
| organization_id | VARCHAR(50) | 机构ID |
| organization_name | VARCHAR(200) | 机构名称 |
| department_id | VARCHAR(50) | 部门ID |
| department_name | VARCHAR(200) | 部门名称 |
| name | VARCHAR(200) NOT NULL | 成果名称 |
| version | VARCHAR(50) | 版本号 |
| change_version | VARCHAR(50) | 变更版本 |
| product_external_version | VARCHAR(50) | 产品对外版本 |
| has_baseline | VARCHAR(20) | 默认"无基线" |
| requirement_proposer | VARCHAR(100) | 需求提出人 |
| achievement_form | VARCHAR(100) | 成果形式 |
| sale_type | VARCHAR(100) | 销售类型 |
| function_list_file | VARCHAR(500) | 功能列表文件 |
| package_ids | TEXT | 包ID列表 |
| application_scenario | TEXT | 应用场景 |
| module_id | VARCHAR(50) | 模块ID |
| module_name | VARCHAR(200) | 模块名称 |
| type | VARCHAR(50) | 类型 |
| description | TEXT | 描述 |
| owner | VARCHAR(100) | 责任人 |
| achievement_target | TEXT | 成果目标 |
| planned_acceptance_date | DATE | 计划验收日期 |
| acceptance_method | TEXT | 验收方式 |
| acceptor | VARCHAR(200) | 验收人 |
| related_project_id | VARCHAR(50) | 关联项目ID |
| related_project_name | VARCHAR(200) | 关联项目名称 |
| related_order_id | VARCHAR(50) | 关联订单ID |
| related_order_name | VARCHAR(200) | 关联订单名称 |
| acceptance_requirements | TEXT | 验收要求 |
| acceptance_organization | VARCHAR(200) | 验收组织 |
| change_reason | TEXT | 变更原因 |
| deliverables | TEXT | 交付物 |
| code_repository_url | VARCHAR(500) | 代码仓库地址 |
| demo_url | VARCHAR(500) | Demo地址 |
| actual_acceptance_date | DATE | 实际验收日期 |
| status | VARCHAR(50) | 状态枚举 [PRE_REGISTER/REGISTER/RECORDED/OFFLINE/DELETED] |
| estimated_acceptance_month | VARCHAR(20) | 预计验收月份 |
| pre_register_time | DATETIME | 预注册时间 |
| register_time | DATETIME | 注册时间 |
| record_time | DATETIME | 登记时间 |
| created_by | VARCHAR(100) | 创建人 |
| updated_by | VARCHAR(100) | 更新人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| risk_tags | TEXT | 风险标签 |

### 2.3 targets — 目标表

| 列名 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(50) PK | UUID 主键 |
| department | VARCHAR(100) | 部门 |
| organization | VARCHAR(100) | 机构 |
| category | VARCHAR(50) | 类别 (价值/费用) |
| sub_category | VARCHAR(50) | 细分目标 |
| target_type | VARCHAR(50) | 目标类型 (冗余字段,当前用 sub_category 判断) |
| year | INT | 年份 |
| annual_target | DECIMAL(15,2) | 年度目标(元) |
| q1_target | DECIMAL(15,2) | Q1目标(元) |
| q2_target | DECIMAL(15,2) | Q2目标(元) |
| q3_target | DECIMAL(15,2) | Q3目标(元) |
| q4_target | DECIMAL(15,2) | Q4目标(元) |
| q1_actual | DECIMAL(15,2) | Q1实际(元) |
| q2_actual | DECIMAL(15,2) | Q2实际(元) |
| q3_actual | DECIMAL(15,2) | Q3实际(元) |
| q4_actual | DECIMAL(15,2) | Q4实际(元) |
| owner | VARCHAR(100) | 责任人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**细分目标枚举**:
- `签约收入（高）` / `签约收入（中）` — 类别=价值, 归入"签约目标"
- `确权收入（高）` / `确权收入（中）` — 类别=价值, 归入"确权目标"
- `研发成果` — 类别=价值, 数据从 achievements 表统计
- `费用` — 类别=费用, 归入"预算控制"

### 2.4 achievement_status_records — 状态变更记录

| 列名 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(50) PK | UUID 主键 |
| achievement_id | VARCHAR(50) NOT NULL | 成果ID |
| achievement_name | VARCHAR(200) NOT NULL | 成果名称 |
| from_status | VARCHAR(50) NOT NULL | 变更前状态 |
| to_status | VARCHAR(50) NOT NULL | 变更后状态 |
| change_type | VARCHAR(50) NOT NULL | 变更类型 |
| change_reason | TEXT | 变更原因 |
| operator | VARCHAR(100) | 操作人 |
| change_time | DATETIME | 变更时间 |
| created_at | DATETIME | 创建时间 |

### 2.5 achievement_version_records — 版本变更记录

| 列名 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(50) PK | UUID 主键 |
| achievement_id | VARCHAR(50) NOT NULL | 成果ID |
| achievement_name | VARCHAR(100) NOT NULL | 成果名称 |
| product_external_version | VARCHAR(20) NOT NULL | 产品对外版本 |
| from_version | VARCHAR(20) NOT NULL | 变更前版本 |
| to_version | VARCHAR(20) NOT NULL | 变更后版本 |
| changed_fields | TEXT NOT NULL | 变更字段 |
| change_description | TEXT | 变更说明 |
| risk_tags | TEXT | 风险标签 |
| operator | VARCHAR(50) | 操作人 |
| change_time | DATETIME NOT NULL | 变更时间 |
| created_at | DATETIME NOT NULL | 创建时间 |

### 2.6 actual_data — 实际数据

| 列名 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(50) PK | UUID 主键 |
| target_id | VARCHAR(50) | 目标ID |
| organization | VARCHAR(100) | 组织 |
| data_type | VARCHAR(50) | 数据类型 |
| year | INT | 年份 |
| month | INT | 月份 |
| actual_value | DECIMAL(15,2) | 实际值 |
| remark | TEXT | 备注 |
| created_by | VARCHAR(100) | 创建人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.7 products — 产品表

| 列名 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(50) PK | UUID 主键 |
| product_name | VARCHAR(200) | 产品名称 |
| product_code | VARCHAR(50) | 产品编码 |
| organization_id | VARCHAR(50) | 组织ID |
| organization_name | VARCHAR(200) | 组织名称 |
| description | TEXT | 描述 |
| created_by | VARCHAR(100) | 创建人 |
| updated_by | VARCHAR(100) | 更新人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

## 3. 后端 API 文档

> 共 3 个 Controller，26 个端点。

### 3.1 目标统计管理 — `/api/targets`

| 方法 | 路径 | 说明 | 请求参数 |
|---|---|---|---|
| GET | `/api/targets/statistics` | 获取目标统计数据 | `year`(必填), `dimension`(默认organization), `product`(部门筛选用), `organization`(支持逗号分隔多选), `owner`, `subCategory`(选填) |
| GET | `/api/targets/quarterly-summary` | 获取季度汇总数据 | `year`(必填), `organization`(支持逗号分隔,选填) |
| GET | `/api/targets/distribution` | 获取月度成果分布 | `year`(必填) |
| GET | `/api/targets/products` | 获取所有产品列表 | — |
| GET | `/api/targets/organizations` | 获取所有机构列表 | — |
| GET | `/api/targets/owners` | 获取所有负责人列表 | — |
| GET | `/api/targets/test-count` | 测试数据库记录数 | — |
| POST | `/api/targets` | 创建新目标 | Body: CreateTargetRequest |
| POST | `/api/targets/actual` | 保存实际数据 | Body: ActualDataRequest |
| POST | `/api/targets/import` | 导入目标数据(multipart) | `file`(MultipartFile) |
| PUT | `/api/targets/{id}` | 更新目标 | Path: id; Body: CreateTargetRequest |
| DELETE | `/api/targets/{id}` | 删除目标 | Path: id |

#### 3.1.1 GET /api/targets/statistics 响应结构

```json
{
  "dimension": "organization",
  "year": 2026,
  "statistics": [
    {
      "id": "uuid",
      "organizationName": "业务方案高风险机构",
      "subCategory": "签约收入（高）",
      "targetType": "价值",
      "owner": "马跃",
      "annualTarget": 50000000,
      "actualValue": 0,
      "signingTarget": 50000000,
      "signingActual": 23470000,
      "confirmationTarget": 0,
      "confirmationActual": 0,
      "rdTarget": 0,
      "rdActual": 0,
      "rdPlanned": 0,
      "budgetTarget": 0,
      "budgetActual": 0,
      "q1Target": 5000000,
      "q1Actual": 20580000,
      "q2Target": 0,
      "q2Actual": 0,
      ...
    }
  ],
  "summary": {
    "signingTarget": 146280000,
    "signingActual": 23470000,
    "signingRate": 16.04,
    "confirmationTarget": 72390000,
    "confirmationActual": 0,
    "confirmationRate": 0,
    "budgetTarget": 16319412.67,
    "budgetActual": 760000,
    "budgetRate": 4.66
  },
  "rdActual": 39,
  "rdPlanned": 210,
  "rdTarget": 249
}
```

**核心计算逻辑**:
- `签约目标` = subCategory 含"签约"的记录, target=annualTarget, actual=q1+q2+q3+q4 actual
- `确权目标` = subCategory 含"确权"的记录, target=annualTarget, actual=q1+q2+q3+q4 actual
- `研发成果` = subCategory="研发成果"的记录, target=rdActual+rdPlanned (从 achievements 表统计), actual=有实际验收日期+已登记的数量
- `预算控制` = subCategory="费用"的记录, target=annualTarget, actual=q1+q2+q3+q4 actual

#### 3.1.2 GET /api/targets/quarterly-summary 响应结构

```json
{
  "signing": [
    {"quarter": "Q1", "target": 23090000, "actual": 23470000, "completionRate": 101.65},
    {"quarter": "Q2", "target": 48030000, "actual": 0, "completionRate": 0},
    {"quarter": "Q3", "target": 40300000, "actual": 0, "completionRate": 0},
    {"quarter": "Q4", "target": 34860000, "actual": 0, "completionRate": 0}
  ],
  "confirmation": [
    {"quarter": "Q1", "target": 0, "actual": 0, "completionRate": 0},
    {"quarter": "Q2", "target": 2000000, "actual": 0, "completionRate": 0},
    {"quarter": "Q3", "target": 15000000, "actual": 0, "completionRate": 0},
    {"quarter": "Q4", "target": 55390000, "actual": 0, "completionRate": 0}
  ],
  "budget": [
    {"quarter": "Q1", "target": 5168580.02, "actual": 5930000, "completionRate": 114.73},
    {"quarter": "Q2", "target": 4005401.82, "actual": 0, "completionRate": 0},
    {"quarter": "Q3", "target": 3669620.42, "actual": 0, "completionRate": 0},
    {"quarter": "Q4", "target": 3475970.42, "actual": 0, "completionRate": 0}
  ]
}
```

**计算逻辑**:
- 按 subCategory 分组：`signing`(含"签约") / `confirmation`(含"确权") / `budget`(="费用")
- 每组分别累加所有机构的 Q1-Q4 target/actual
- organization 参数支持逗号分隔的多机构筛选

---

### 3.2 成果管理 — `/api/achievements`

| 方法 | 路径 | 说明 | 请求参数 |
|---|---|---|---|
| GET | `/api/achievements` | 获取成果列表(分页) | `page`(默认1), `pageSize`(默认20), `status`, `achievementForm`, `productId`, `keyword`, `includeDeleted`, `plannedAcceptanceMonth`, `organizationName` |
| GET | `/api/achievements/statistics` | 获取成果统计 | — |
| GET | `/api/achievements/{achievementId}` | 获取成果详情 | Path: achievementId |
| GET | `/api/achievements/{achievementId}/history` | 获取变更历史 | Path: achievementId |
| GET | `/api/achievements/organizations` | 获取机构列表 | — |
| POST | `/api/achievements/pre-register` | 成果预注册 | Body: AchievementPreRegisterRequest |
| POST | `/api/achievements/{achievementId}/register` | 成果注册 | Path: achievementId |
| POST | `/api/achievements/{achievementId}/record` | 成果登记 | Path: achievementId |
| POST | `/api/achievements/{achievementId}/change` | 成果变更 | Path: achievementId |
| PUT | `/api/achievements/{achievementId}/offline` | 成果下架 | Path: achievementId |
| PUT | `/api/achievements/{achievementId}/online` | 成果上架 | Path: achievementId |
| PUT | `/api/achievements/{achievementId}/delete` | 成果删除 | Path: achievementId |

### 3.3 数据导入 — `/api/data`

| 方法 | 路径 | 说明 | 请求参数 |
|---|---|---|---|
| POST | `/api/data/import` | 上传Excel导入数据 | `file`(MultipartFile) |
| POST | `/api/data/import-file` | 从文件路径导入数据 | Body: FilePathRequest |
| POST | `/api/data/reinitialize` | 清空并重新导入 | `file`(MultipartFile), `confirm`(boolean) |

---

## 4. 前端-后端通讯

### 4.1 请求配置

**Vue 3 前端使用 axios**:
- baseURL: `/api` (相对路径)
- timeout: 10000ms
- Content-Type: `application/json` (默认)
- 响应拦截器: 自动解包 `response.data`
- 无认证头 (无 Bearer token 注入)

**代理配置** (vite.config.js):
```js
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

### 4.2 API 调用方式

**Vue 3 前端** (通过 request.js):

```js
import request from './request'

// GET 请求
request.get('/targets/statistics', { params: { year: 2026 } })

// POST 请求
request.post('/targets', { organization: 'xxx', ... })

// PUT 请求
request.put('/targets/{id}', { ... })

// DELETE 请求
request.delete('/targets/{id}')

// 文件上传 (multipart)
request.post('/targets/import', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
```

### 4.3 前端 API 文件结构

```
frontend-vue/src/api/
  ├── request.js          # axios 实例配置 (baseURL=/api, timeout=10s)
  ├── achievement.js      # 成果管理 API (12个方法)
  └── target.js           # 目标统计 API (12个方法, 含 getQuarterlySummary)
```

### 4.4 数据流示例 (目标统计页)

```
TargetStatistics.vue
  │
  ├── loadFilterOptions()
  │   └── GET /api/targets/products → departments
  │   └── GET /api/targets/organizations
  │   └── GET /api/targets/owners
  │
  ├── loadStatistics()
  │   └── GET /api/targets/statistics?year=2026&dimension=organization
  │       ├── 响应顶层 rdActual, rdPlanned, rdTarget → 研发成果卡片
  │       └── summary.signingTarget/Actual/Rate → 签约目标卡片
  │           summary.confirmationTarget/Actual/Rate → 确权目标卡片
  │           summary.budgetTarget/Actual/Rate → 预算控制卡片
  │
  ├── loadQuarterlySummary()
  │   └── GET /api/targets/quarterly-summary?year=2026
  │       ├── signing → 签约收入季度对比图 (Q1-Q4)
  │       ├── confirmation → 确权收入季度对比图 (Q1-Q4)
  │       └── budget → 预算控制季度对比图 (Q1-Q4)
  │
  ├── loadMonthlyDistribution()
  │   └── GET /api/targets/distribution?year=2026 → 研发成果月度分布图
  │
  └── loadTableData()
      └── GET /api/targets/statistics?year=2026 → 机构目标达成进度表

  └── 编辑/新增
      └── POST /api/targets (createTarget)
      └── PUT /api/targets/{id} (updateTarget)

handleDeleteRow()
  └── DELETE /api/targets/{id}
```

---

## 5. 前端参数表

### 5.1 筛选条件参数 (filterForm)

| 字段 | 类型 | 说明 | 数据来源 |
|---|---|---|---|
| year | Number | 年度 (2024/2025/2026) | 硬编码 |
| department | String | 部门（原"产品"修改） | GET /api/targets/products |
| organizations | String[] | 机构（多选） | 部门联动筛选: organizationDepartmentOptions |
| owner | String | 负责人 | GET /api/targets/owners |
| status | String | 成果状态 (PRE_REGISTER/REGISTER/RECORDED) | 硬编码 |
| subCategory | String | 细分目标 | 硬编码枚举 |

### 5.2 机构-部门映射 (organizationDepartmentOptions)

定义列表参数弹框中机构选择时的部门自动填充:

| 机构 | 部门 |
|---|---|
| AI方案研发机构 | AI方案中心 |
| AI场景研发机构 | AI方案中心 |
| AI方案中心本级 | AI方案中心 |
| 端技术底座机构 | 能力中心 |
| AI技术底座机构 | 能力中心 |
| POC方案验证机构 | 能力中心 |
| 服务技术底座机构 | 能力中心 |
| 能力中心本级 | 能力中心 |
| 数据中心本级 | 数据中心 |
| 业务方案高风险机构 | 业务方案中心 |
| 业务方案中风险机构 | 业务方案中心 |
| 业务方案中心本级 | 业务方案中心 |
| 中台运营机构 | FM平台中心 |
| 生产工具机构 | FM平台中心 |
| FM平台中心本级 | FM平台中心 |

### 5.3 细分目标-子类目映射 (subCategoryMap)

| 类别 | 子类目 |
|---|---|
| 价值 | 签约收入（高）, 签约收入（中）, 确权收入（高）, 确权收入（中）, 研发成果 |
| 费用 | 费用 |

### 5.4 研发成果统计逻辑 (前端展示)

| 卡片字段 | 数据来源 | 计算方式 |
|---|---|---|
| 年度目标 | `response.rdTarget` | Achievements 表实际完成 + 计划中总数 |
| 计划成果 | `response.rdPlanned` | Achievements 表 actual_acceptance_date IS NULL 数量 |
| 实际完成 | `response.rdActual` | Achievements 表 actual_acceptance_date IS NOT NULL AND status='RECORDED' 数量 |
| 达成率 | 前端计算 | `Math.round(rdActual / rdTarget * 100)` |

### 5.5 金额单位规则

| 数据类型 | 存储单位 | 展示单位 | 前端转换 |
|---|---|---|---|
| 签约目标/确权目标/预算控制 | 元 (BigDecimal) | 万元 | 值 / 10000, `formatMoney()` (千分位+两位小数) |
| 研发成果 | 个 (Integer) | 个 | 原值展示, `formatInteger()` (千分位整数) |

### 5.6 图表可视化布局

目标统计页底部为 **2×2 四图表看板**：

| 位置 | 图表标题 | 图表类型 | 数据来源 |
|---|---|---|---|
| 左上 | 签约收入 — 季度目标 vs 实际 | 分组柱状图 | `GET /api/targets/quarterly-summary` → signing |
| 右上 | 确权收入 — 季度目标 vs 实际 | 分组柱状图 | `GET /api/targets/quarterly-summary` → confirmation |
| 左下 | 研发成果 — 月度分布 | 堆叠柱状图 | `GET /api/targets/distribution` |
| 右下 | 预算控制 — 季度预算 vs 实际支出 | 分组柱状图 | `GET /api/targets/quarterly-summary` → budget |

**季度对比图规格**:
- X 轴: Q1 / Q2 / Q3 / Q4
- 两个系列: 目标(浅色) vs 实际(深色)
- 签约: 浅蓝 `#a0cfff` / 深蓝 `#409eff`
- 确权: 浅橙 `#f4cfa0` / 深橙 `#e6a23c`
- 预算: 浅红 `#f0a0a0` / 深红 `#f56c6c`
- Y 轴单位: 万元
- Tooltip 显示: 季度 / 目标(万元) / 实际(万元) / 达成率(%)

---

## 6. 枚举与常量

### 6.1 AchievementStatus 成果状态

| 枚举值 | code | 中文名 |
|---|---|---|
| PRE_REGISTER | pre_register | 预注册 |
| REGISTER | register | 注册 |
| RECORDED | recorded | 登记 |
| OFFLINE | offline | 下架 |
| DELETED | deleted | 已删除 |

### 6.2 统计维度 (dimension)

| 值 | 说明 |
|---|---|
| organization | 按机构维度统计 (当前默认/唯一使用) |
| owner | 按负责人维度统计 |
| product | 按产品维度统计 |

### 6.3 环境配置

| Profile | 数据库 | DDL策略 | 启用方式 |
|---|---|---|---|
| 默认 | MySQL `achievement_db` | validate | mvn spring-boot:run |
| dev | MySQL `achievement_db_dev` | validate | -Dspring-boot.run.profiles=dev |
| h2 | H2 内存库 | create-drop | -Dspring-boot.run.profiles=h2 |
| sqlite | SQLite 文件 `data/achievement.db` | update | -Dspring-boot.run.profiles=sqlite |
| prod | MySQL `achievement_db_prod` | validate | -Dspring-boot.run.profiles=prod |

---

*本文档由系统自动生成，对应当前代码库的实际状态。*
