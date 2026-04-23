# 成果管理模块

成果管理模块是一个独立的企业成果资产全生命周期管理系统，支持成果的预注册、注册、登记、下架等完整流程管理。

## 功能特性

- **成果全生命周期管理**: 支持预注册→注册→登记→下架的完整流程
- **版本管理**: 自动生成版本号，记录版本变更历史
- **状态追踪**: 完整记录成果状态变更历史
- **出库管理**: 支持成果出库和使用追踪
- **统计分析**: 提供成果统计数据

## 目录结构

```
achievement-standalone/
├── frontend/                    # 前端代码
│   ├── pages/
│   │   ├── page.tsx            # 成果列表页
│   │   ├── [id]/page.tsx       # 成果详情页
│   │   ├── pre-register/page.tsx # 预注册页
│   │   └── change/page.tsx     # 变更页
│   └── types.ts                # 类型定义
├── backend/                     # 后端代码（Python原始实现，供参考）
│   ├── models/
│   │   ├── achievement.py
│   │   ├── achievement_status_record.py
│   │   └── achievement_version_record.py
│   ├── schemas/
│   │   ├── achievement.py
│   │   └── achievement_version_record.py
│   └── api/
│       ├── achievements.py
│       └── achievement_version_records.py
├── database/                    # 数据库相关
│   ├── schema.sql              # 表结构定义
│   └── sample_data.sql         # 示例数据
├── docs/                        # 文档
│   ├── API.md                  # API接口文档
│   ├── DATABASE.md             # 数据库设计文档
│   └── JAVA_IMPLEMENTATION.md  # Java实现指南
└── README.md                    # 本文件
```

## 快速开始

### 数据库初始化

1. 创建数据库
```sql
CREATE DATABASE achievement_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行建表脚本
```bash
mysql -u root -p achievement_db < database/schema.sql
```

3. 导入示例数据（可选）
```bash
mysql -u root -p achievement_db < database/sample_data.sql
```

### 前端部署

前端代码基于 Next.js 开发，需要配合完整的 Next.js 项目使用：

1. 将 `frontend/pages` 目录下的文件复制到 Next.js 项目的 `app/achievement` 目录
2. 将 `frontend/types.ts` 中的类型定义合并到项目的类型文件中
3. 确保安装了必要的依赖：
   - lucide-react
   - @radix-ui/react-tooltip
   - 其他 shadcn/ui 组件

### 后端实现

本模块提供了 Python (FastAPI) 的原始实现供参考，如需使用 Java 实现：

1. 参考 `docs/JAVA_IMPLEMENTATION.md` 了解 Java 实现指南
2. 参考 `docs/API.md` 了解完整的 API 接口规范
3. 参考 `docs/DATABASE.md` 了解数据库设计细节

## 成果状态流转

```
预注册 (pre_register)
    ├── 注册 (register)
    │       └── 登记 (record)
    │               ├── 下架 (offline)
    │               │       └── 上架 (record)
    │               └── ...
    └── 删除 (deleted)
```

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/achievements | 获取成果列表 |
| GET | /api/achievements/statistics | 获取成果统计 |
| GET | /api/achievements/{id} | 获取成果详情 |
| POST | /api/achievements/pre-register | 成果预注册 |
| POST | /api/achievements/{id}/register | 成果注册 |
| POST | /api/achievements/{id}/record | 成果登记 |
| POST | /api/achievements/{id}/change | 成果变更 |
| PUT | /api/achievements/{id}/offline | 成果下架 |
| PUT | /api/achievements/{id}/online | 成果上架 |
| PUT | /api/achievements/{id}/delete | 成果删除 |

详细 API 文档请参考 [docs/API.md](docs/API.md)

## 数据库表

| 表名 | 说明 |
|------|------|
| achievements | 成果主表 |
| achievement_status_records | 状态变更记录表 |
| achievement_version_records | 版本变更记录表 |
| achievement_attachments | 附件表 |
| achievement_checkout_history | 出库历史表 |

详细数据库设计请参考 [docs/DATABASE.md](docs/DATABASE.md)

## 技术栈

### 原始实现
- **前端**: Next.js 14, React, TypeScript, Tailwind CSS, shadcn/ui
- **后端**: Python, FastAPI, SQLAlchemy, PostgreSQL

### Java 实现推荐
- **框架**: Spring Boot 3.x
- **ORM**: Spring Data JPA 或 MyBatis-Plus
- **数据库**: MySQL 8.0+ 或 PostgreSQL 14+
- **认证**: Spring Security + JWT

## 许可证

MIT License
