# 成果管理系统

企业成果资产全生命周期管理 + 生产数据跟踪平台。

## 功能特性

- **成果管理**: 预注册→注册→登记→下架的完整生命周期管理，版本变更追踪
- **生产总览 / Dashboard**: KPI 总览、产品签约排名、确权收入趋势、成果状态分布、签约风险结构、部门预算 vs 实际、产品概览，支持部门/机构/年份/季度筛选
- **目标统计**: 签约/成果/费用三类目标的季度汇总与明细管理，数据从钉钉自动聚合同步
- **钉钉数据同步**: 7 张钉钉数据仓库表自动同步到本地 SQLite 缓存，支持全量刷新和增量对比
- **钉钉登录**: 生产环境基于钉钉 JSAPI 授权码换取 JWT Token 的身份认证

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3 + Vite + Element Plus |
| 后端 | Spring Boot 3 + Spring Data JPA |
| 数据库 | SQLite |
| 数据源 | DingTalk 数据仓库 API |
| 可选辅助 | FastAPI (dingtalk-fastapi, 仅开发调试) |

## 快速启动

```bash
./start.sh
```

一键启动 Java 后端 (8080)、Vue 前端 (3000) 和可选的钉钉同步 API (8000)。

## 服务地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:3000 |
| 后端 API | http://localhost:8080 |
| Swagger 文档 | http://localhost:8080/swagger-ui.html |
| 钉钉同步 API (可选) | http://localhost:8000 |
| 钉钉同步文档 (可选) | http://localhost:8000/docs |

## 环境要求

- Java 17+ / Maven 3.8+
- Node.js 18+ / npm 9+
- Python 3.9+ (仅 dingtalk-fastapi 可选组件需要)

## 关键 API 端点

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/dingtalk/login` | 钉钉授权码换取 JWT (生产环境) |
| GET | `/api/auth/me` | 获取当前用户信息 |

### Dashboard
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard/kpi` | KPI 总览 |
| GET | `/api/dashboard/product-signing` | 产品签约排名 |
| GET | `/api/dashboard/revenue-monthly` | 月度确权收入趋势 |
| GET | `/api/dashboard/achievement-status` | 成果状态分布 |
| GET | `/api/dashboard/signing-risk` | 签约风险结构 |
| GET | `/api/dashboard/department-budget` | 部门预算 vs 实际 |
| GET | `/api/dashboard/products` | 产品概览 |
| GET | `/api/dashboard/filter-options` | 筛选选项 |

所有 Dashboard 端点支持可选筛选参数: `department`, `organization`, `year`, `quarter`。

### 成果管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/achievements` | 成果列表 (分页/搜索/筛选) |
| GET | `/api/achievements/statistics` | 成果统计 |
| GET | `/api/achievements/{id}` | 成果详情 |
| POST | `/api/achievements/pre-register` | 预注册 |
| POST | `/api/achievements/{id}/register` | 注册 |
| POST | `/api/achievements/{id}/record` | 登记 |
| POST | `/api/achievements/{id}/change` | 变更 |
| PUT | `/api/achievements/{id}/offline` | 下架 |
| PUT | `/api/achievements/{id}/online` | 上架 |
| PUT | `/api/achievements/{id}/delete` | 软删除 |
| POST | `/api/achievements/sync-from-dingtalk` | 从钉钉同步成果 |

### 数据同步
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/data/sync-all` | 全量同步钉钉 7 张表到本地 |
| GET | `/api/data/sync-status` | 同步状态与 API 配额 |

### 目标统计
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/targets/statistics` | 目标统计 |
| GET | `/api/targets/quarterly-summary` | 季度汇总 |
| POST | `/api/targets` | 新增目标 |
| PUT | `/api/targets/{id}` | 更新目标 |
| DELETE | `/api/targets/{id}` | 删除目标 |
| POST | `/api/targets/actual` | 录入实际数据 |

## 目录结构

```
achievement-standalone/
├── start.sh                  # 一键启动脚本
├── start-prod.sh             # 生产模式启动
├── deploy.sh                 # 部署打包脚本
├── docker-compose.yml        # Docker 编排
├── frontend-vue/             # Vue 3 前端
│   ├── src/
│   │   ├── api/              # API 请求封装
│   │   ├── router/           # 路由配置
│   │   └── views/            # 页面组件 (Dashboard, 成果管理, 目标统计, 用户管理等)
│   ├── package.json
│   └── vite.config.js
├── java-backend/             # Spring Boot 后端
│   ├── src/main/java/com/example/achievement/
│   │   ├── config/           # 配置 (Security, JWT, CORS, Swagger, SQLite方言)
│   │   ├── controller/       # REST 控制器
│   │   ├── dto/              # 请求/响应 DTO
│   │   ├── entity/           # JPA 实体
│   │   ├── enums/            # 枚举
│   │   ├── repository/       # JPA Repository
│   │   ├── service/          # 业务服务 (含钉钉同步/登录/数据服务)
│   │   └── util/             # 工具类 (JWT, 版本生成, 数据转换)
│   ├── src/main/resources/
│   │   ├── application-sqlite.yml   # SQLite 配置 (开发/生产共用)
│   │   ├── application-prod.yml     # 生产环境配置
│   │   └── db/migration/            # 数据库迁移脚本
│   └── pom.xml
├── dingtalk-fastapi/         # 钉钉网关 API (可选, 辅助调试)
│   ├── app/
│   │   ├── main.py
│   │   ├── client.py
│   │   └── config.py
│   ├── config.yaml.example
│   └── requirements.txt
├── deploy/                   # 部署包输出目录
│   ├── achievement-service-1.0.0.jar
│   ├── static/               # 前端构建产物
│   └── start.sh              # 部署启动脚本
└── data/
    └── achievement.db        # SQLite 数据库文件
```

## 数据流

```
钉钉数据仓库 (7张表)
  │
  ├── POST /api/data/sync-all → 本地 SQLite dt_* 缓存表
  │
  ├── dt_signing_contracts + dt_signing_orders → Dashboard KPI / 产品签约排名
  ├── dt_revenue_details → 月度收入趋势 / 确权明细
  ├── dt_achievements → 成果状态分布
  ├── dt_department_budgets → 部门预算 vs 实际
  ├── dt_products → 产品概览
  │
  ├── dt_signing_contracts/orders → DingTalkDataService → targets 签约实际值
  ├── dt_achievements → DingTalkDataService → targets 研发成果
  └── dt_department_budgets (drgfZh4) → DingTalkDataService → targets 费用/预算
```

## 部署

```bash
# 构建部署包 (JAR + 前端静态文件 + 数据库)
./deploy.sh

# Docker 部署
docker-compose up -d
```

## 许可证

MIT License
