# 成果管理系统 — 架构文档

> 版本: 2.0.0 | 最后更新: 2026-05-09

---

## 目录

1. [项目概览](#1-项目概览)
2. [数据库结构](#2-数据库结构)
3. [后端 API 总览](#3-后端-api-总览)
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
- **内网访问**: 前端 `http://{IP}:3001/`，后端 `http://{IP}:8080/`（需配置 `server.address=0.0.0.0`）
- **代理转发**: 前端 Vite proxy 将 `/api/*` 请求转发到 `localhost:8080`

### 1.3 项目结构

```
achievement-standalone/
├── frontend-vue/              # 主前端 (Vue 3)
│   ├── src/
│   │   ├── api/               # API 封装 (request.js, achievement.js, target.js)
│   │   ├── views/             # 页面组件
│   │   │   ├── AchievementList.vue     # 成果列表
│   │   │   ├── TargetStatistics.vue    # 目标统计
│   │   │   ├── PreRegister.vue         # 预注册
│   │   │   ├── RegisterAchievement.vue # 注册
│   │   │   ├── RecordAchievement.vue   # 登记
│   │   │   └── ChangeAchievement.vue   # 变更
│   │   └── router/            # 路由配置
│   └── vite.config.js         # Vite 配置 (proxy, host)
├── java-backend/              # 主后端 (Spring Boot)
│   └── src/main/java/com/example/achievement/
│       ├── controller/        # AchievementController, TargetController, DataController
│       ├── service/           # Service 接口 + impl 实现
│       ├── repository/        # JPA Repository + 原生 SQL 查询
│       ├── entity/            # Achievement, Target, 审计记录实体
│       ├── dto/               # Request/Response DTO
│       └── enums/             # AchievementStatus 枚举
├── data/                      # SQLite 数据库文件
└── docs/                      # 设计文档
    ├── ARCHITECTURE.md        # 本文档 — 架构总览
    ├── API.md                 # API 接口详细文档
    ├── DATABASE.md            # 数据库设计文档
    └── JAVA_IMPLEMENTATION.md # Java 实现指南
```

---

## 2. 数据库结构

> 共 6 张表。**详细字段说明、状态流转图、索引请参见 [DATABASE.md](./DATABASE.md)**。

### 2.1 表关系概览

```
achievements (成果主表)
  ├── achievement_status_records (状态变更记录) — achievement_id -> id
  ├── achievement_version_records (版本变更记录) — achievement_id -> id
  └── products (产品) — product_id -> id

targets (目标表)
  └── actual_data (实际数据) — target_id -> id
```

### 2.2 核心表摘要

| 表名 | 用途 | 主文档章节 |
|---|---|---|
| `achievements` | 成果主表，存储全生命周期数据 | [DATABASE.md §1](./DATABASE.md#1-成果主表-achievements) |
| `achievement_status_records` | 状态变更审计日志 | [DATABASE.md §2](./DATABASE.md#2-成果状态变更记录表-achievement_status_records) |
| `achievement_version_records` | 版本变更审计日志 | [DATABASE.md §3](./DATABASE.md#3-成果版本变更记录表-achievement_version_records) |
| `targets` | 目标数据，含季度目标/实际值 | [DATABASE.md §6](./DATABASE.md#6-目标表-targets) |
| `actual_data` | 实际数据明细 | — |
| `products` | 产品信息 | — |

---

## 3. 后端 API 总览

> 共 5 个 Controller。**完整请求参数、响应示例详见 [API.md](./API.md)**。

### 3.1 目标统计 — `/api/targets`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/targets/statistics` | 获取目标统计（卡片 + 表格数据），参数 `year`(必填), `product`, `organization`, `owner`, `subCategory` |
| GET | `/api/targets/quarterly-summary` | 获取签约/确权/预算季度汇总（柱状图数据） |
| GET | `/api/targets/distribution` | 获取研发成果月度分布（堆叠柱状图数据） |
| GET | `/api/targets/products` | 获取部门列表 |
| GET | `/api/targets/organizations` | 获取机构列表 |
| GET | `/api/targets/owners` | 获取负责人列表 |
| POST | `/api/targets` | 创建目标 |
| PUT | `/api/targets/{id}` | 更新目标 |
| DELETE | `/api/targets/{id}` | 删除目标 |
| POST | `/api/targets/import` | Excel 导入目标数据 |

**统计核心计算逻辑**:
- `签约目标` → subCategory 含"签约"，**实际值优先**从 `contract_signings.amount` 按机构+年度+季度+细分目标聚合，无数据回退到 targets.q1~q4_actual
- `确权目标` → subCategory 含"确权"，**实际值优先**从 `revenue_recognitions.revenue_amount`（含税收入）按机构+年度+季度+细分目标聚合
- `研发成果` → 从 achievements 表实时统计，target = 已验收 + 计划中，actual = 已登记且已验收
- `费用` → subCategory = "费用"，实际值取自 targets.q1~q4_actual（手工编辑）

### 3.2 成果管理 — `/api/achievements`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/achievements` | 获取成果列表（分页 + 多条件筛选） |
| GET | `/api/achievements/statistics` | 获取成果统计 |
| GET | `/api/achievements/{id}` | 获取成果详情 |
| GET | `/api/achievements/{id}/history` | 获取版本变更历史 |
| GET | `/api/achievements/filter-options` | **级联筛选**：根据当前条件返回可选部门/机构 |
| GET | `/api/achievements/departments` | 获取所有部门 |
| GET | `/api/achievements/organizations` | 获取所有机构 |
| GET | `/api/achievements/owners` | 获取所有负责人 |
| POST | `/api/achievements/pre-register` | 预注册 |
| POST | `/api/achievements/{id}/register` | 注册 (PRE_REGISTER → REGISTER) |
| POST | `/api/achievements/{id}/record` | 登记 (REGISTER → RECORDED) |
| POST | `/api/achievements/{id}/change` | 变更 |
| PUT | `/api/achievements/{id}/offline` | 下架 (RECORDED → OFFLINE) |
| PUT | `/api/achievements/{id}/online` | 上架 (OFFLINE → RECORDED) |
| DELETE | `/api/achievements/{id}` | 逻辑删除 (仅 PRE_REGISTER) |

**成果列表筛选参数**: `keyword`, `departmentName`, `organizationNames`(逗号分隔多选), `status`, `productId`, `includeDeleted`

**级联筛选** (`/filter-options`): 传入当前已选条件，返回缩小范围后的部门和机构下拉选项。例如选 `productId=CP_0007` → 部门只返回 FM平台中心，机构只返回生产工具机构。

### 3.3 数据导入 — `/api/data`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/data/import` | 上传 Excel 导入数据 |
| POST | `/api/data/import-file` | 从文件路径导入 |
| POST | `/api/data/reinitialize` | 清空并重新导入 |

### 3.4 签约明细 — `/api/detail`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/detail/signings` | 签约明细列表（筛选 `organization`, `signMonth`） |
| POST | `/api/detail/signings` | 新增签约记录 |
| PUT | `/api/detail/signings/{id}` | 编辑签约记录 |
| DELETE | `/api/detail/signings/{id}` | 删除签约记录 |
| POST | `/api/detail/signings/batch` | 批量导入（从 Excel 粘贴），按合同ID+套餐ID去重 |

**细分目标推导**：保存时根据 `signingRiskLevel` 自动填充 `subCategory`：高→签约收入（高）、中→签约收入（中）。

### 3.5 确权/收入明细 — `/api/detail`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/detail/recognitions` | 确权明细列表（筛选 `organization`, `recognitionMonth`） |
| POST | `/api/detail/recognitions` | 新增确权记录 |
| PUT | `/api/detail/recognitions/{id}` | 编辑确权记录 |
| DELETE | `/api/detail/recognitions/{id}` | 删除确权记录 |
| POST | `/api/detail/recognitions/batch` | 批量导入，按合同ID+订单ID去重 |

**细分目标推导**：保存时根据 `recognitionRiskLevel` 自动填充 `subCategory`：高→确权收入（高）、中→确权收入（中）。

### 3.6 用户管理 — `/api/users`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/users` | 获取启用用户列表（前端用户切换用） |
| GET | `/api/users/config` | 获取全部用户配置（管理页用） |
| POST | `/api/users` | 新增用户 |
| PUT | `/api/users/{id}` | 编辑用户 |
| DELETE | `/api/users/{id}` | 删除用户 |

**权限机制**：前端通过 `X-Current-User` 请求头传递当前用户名，后端 `UserContextFilter` 解析并存入 `ThreadLocal`。Service 层根据用户角色过滤数据（管理员全量、部门负责人看本部门、机构负责人看本机构、普通用户只看自己）。

---

## 4. 前端-后端通讯

### 4.1 请求配置

**Vue 3 前端使用 axios**:
- baseURL: `/api` (相对路径)
- timeout: 10000ms
- Content-Type: `application/json` (默认)
- 响应拦截器: 自动解包 `response.data`

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
```

### 4.3 前端 API 文件

```
frontend-vue/src/api/
  ├── request.js          # axios 实例 (baseURL=/api)
  ├── achievement.js      # 成果管理 API
  └── target.js           # 目标统计 API
```

### 4.4 数据流 — 成果列表 (AchievementList.vue)

```
AchievementList.vue
  │
  ├── onMounted()
  │   ├── loadStatistics()
  │   │   └── GET /api/achievements/statistics → 四个统计卡片
  │   ├── loadAchievements()
  │   │   └── GET /api/achievements?keyword=&departmentName=&organizationNames=&status=&productId=
  │   └── loadFilterOptions()
  │       └── GET /api/achievements/filter-options?keyword=&departmentName=&...&productId=
  │           └── 返回 { departments: [...], organizations: [...] }
  │
  ├── 筛选变更 (@change) → handleFilterChange()
  │   ├── loadAchievements()   # 刷新列表
  │   └── loadFilterOptions()  # 级联更新部门/机构下拉
  │
  └── 部门变更 → handleDepartmentChange()
      ├── 清空 organizationNames
      └── handleFilterChange()
```

### 4.5 数据流 — 目标统计 (TargetStatistics.vue)

```
TargetStatistics.vue
  │
  ├── onMounted()
  │   ├── loadFilterOptions()
  │   │   └── GET /api/targets/products + organizations + owners
  │   ├── loadStatistics()
  │   │   └── GET /api/targets/statistics?year=2026&...
  │   │       ├── summary → 四个统计卡片
  │   │       └── statistics[] → 机构目标达成进度表
  │   ├── loadQuarterlySummary()
  │   │   └── GET /api/targets/quarterly-summary → 季度柱状图
  │   ├── loadMonthlyDistribution()
  │   │   └── GET /api/targets/distribution → 月度分布堆叠图
  │   └── loadTableData()
  │       └── GET /api/targets/statistics (同上)
  │
  └── 筛选变更 → handleFilterChange() → 重新加载全部
```

---

## 5. 前端参数表

### 5.1 目标统计筛选 (filterForm)

| 字段 | 类型 | 说明 | 数据来源 |
|---|---|---|---|
| year | Number | 年度 (2024/2025/2026) | 硬编码 |
| department | String | 部门，选择后重置机构 | GET /api/targets/products |
| organizations | String[] | 机构（多选，collapse-tags） | GET /api/targets/organizations |
| owner | String | 负责人 | GET /api/targets/owners |
| status | String | 成果状态（仅前端图表过滤用） | 硬编码 |
| subCategory | String | 细分目标 | 硬编码枚举 |

### 5.2 成果列表筛选 (searchForm)

| 字段 | 类型 | 说明 | 数据来源 |
|---|---|---|---|
| keyword | String | 关键词搜索（名称/描述/ID） | 用户输入 |
| departmentName | String | 部门，选择后清空机构 | 级联接口 |
| organizationNames | String[] | 机构（多选，collapse-tags） | 级联接口 |
| productId | String | 产品ID | 硬编码 productMap |
| status | String | 状态 | 硬编码枚举 |

**级联逻辑**: 选择任一套件后，自动调用 `/filter-options` 根据当前所有已选条件缩小部门和机构下拉范围。

### 5.3 机构-部门映射

定义列表参数弹框中机构选择时自动填充部门：

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

### 5.4 细分目标-子类目映射

| 类别 | 子类目 |
|---|---|
| 价值 | 签约收入（高）, 签约收入（中）, 确权收入（高）, 确权收入（中）, 研发成果 |
| 费用 | 费用 |

### 5.5 金额单位规则

| 数据类型 | 存储单位 | 展示单位 | 前端转换 |
|---|---|---|---|
| 签约/确权/预算 | 元 (BigDecimal) | 万元 | 值 / 10000, `formatMoney()` |
| 研发成果 | 个 (Integer) | 个 | 原值, `formatInteger()` |

### 5.6 表格视觉规范 (v2.0 新增)

目标统计进度表的视觉规则：

- **组间分隔**: 年度/Q1/Q2/Q3/Q4 列组之间 3px 粗竖线
- **目标列**: 浅灰底色 `#f5f7fa`
- **实际列**: 白底
- **未达标标记**: 实际值 < 目标值时背景浅红 `#fff0f0`，数字红色 `#e04848` 加粗

### 5.7 图表可视化布局

| 位置 | 图表 | 类型 | 配色 |
|---|---|---|---|
| 左上 | 签约收入 季度对比 | 分组柱状图 | 浅蓝 `#a0cfff` / 深蓝 `#409eff` |
| 右上 | 确权收入 季度对比 | 分组柱状图 | 浅橙 `#f4cfa0` / 深橙 `#e6a23c` |
| 左下 | 研发成果 月度分布 | 堆叠柱状图 | 预注册橙/已注册蓝/已登记绿 |
| 右下 | 预算控制 季度对比 | 分组柱状图 | 浅红 `#f0a0a0` / 深红 `#f56c6c` |

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

**状态流转**:

```
PRE_REGISTER → REGISTER → RECORDED ⇄ OFFLINE
     ↓
  DELETED
```

### 6.2 环境配置

| Profile | 数据库 | DDL | 启动参数 |
|---|---|---|---|
| 默认 | MySQL `achievement_db` | validate | `mvn spring-boot:run` |
| dev | MySQL `achievement_db_dev` | validate | `-Dspring-boot.run.profiles=dev` |
| h2 | H2 内存库 | create-drop | `-Dspring-boot.run.profiles=h2` |
| sqlite | SQLite `data/achievement.db` | update | `-Dspring-boot.run.profiles=sqlite` |
| prod | MySQL `achievement_db_prod` | validate | `-Dspring-boot.run.profiles=prod` |

### 6.3 产品代码映射

| 产品代码 | 产品名称 |
|---|---|
| CP_0001 | 数字化智能营销平台 |
| CP_0003 | 对公金融服务平台 |
| CP_0004 | 个人金融服务平台 |
| CP_0007 | Finmall平台 |
| CP_0008 | Finmall资产底座 |
| CP_0012 | DPRO平台 |
| CP_0014 | 信创产品 |
| CP_0018 | 企业服务生态云平台 |
| CP_0019 | AI 手机银行 |

---
*本文档为架构总览，详细接口参数与响应示例见 [API.md](./API.md)，表结构见 [DATABASE.md](./DATABASE.md)，Java 实现见 [JAVA_IMPLEMENTATION.md](./JAVA_IMPLEMENTATION.md)。*
