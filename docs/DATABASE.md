# 成果管理模块数据库设计文档

## 概述

成果管理模块的数据库设计遵循以下原则：
- 支持成果的全生命周期管理（预注册→注册→登记→下架）
- 完整记录成果的版本变更历史
- 支持成果的状态变更追踪
- 支持成果出库管理

---

## 核心表结构

### 1. 成果主表 (achievements)

成果主表是整个模块的核心，存储成果的基本信息和状态。

#### 字段说明

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | VARCHAR(50) | 是 | 主键，格式：成果名称_版本号 |
| name | VARCHAR(200) | 是 | 成果名称 |
| version | VARCHAR(50) | 否 | 成果版本，格式：V+版本号 |
| product_id | VARCHAR(50) | 是 | 所属产品ID |
| product_name | VARCHAR(200) | 否 | 所属产品名称 |
| module_id | VARCHAR(50) | 否 | 所属模块ID |
| module_name | VARCHAR(200) | 否 | 所属模块名称 |
| type | VARCHAR(50) | 否 | 成果类型（模块/功能/资产/文档/其他） |
| description | TEXT | 否 | 成果描述 |
| status | VARCHAR(50) | 否 | 成果状态，默认pre_register |
| owner | VARCHAR(100) | 否 | 负责人 |
| pre_register_time | TIMESTAMP | 否 | 预注册时间 |
| register_time | TIMESTAMP | 否 | 注册时间 |
| record_time | TIMESTAMP | 否 | 登记时间 |
| created_at | TIMESTAMP | 是 | 创建时间 |
| updated_at | TIMESTAMP | 是 | 更新时间 |

#### 完整字段分类

**基础数据**
- product_id, product_name - 产品关联
- organization_id, organization_name - 机构关联
- department_id, department_name - 部门关联
- name, version - 成果标识
- product_external_version - 产品对外版本
- has_baseline - 是否有基线
- requirement_proposer - 需求提出人
- achievement_form - 成果形态
- sale_type - 可售类型
- function_list_file - 功能清单文件
- package_ids - 所属套餐ID列表（JSON）
- application_scenario - 应用场景
- module_id, module_name - 模块关联
- type - 成果类型
- description - 描述
- owner - 负责人

**计划数据**
- achievement_target - 成果目标
- planned_acceptance_date - 计划验收日期
- acceptance_method - 验收方式
- acceptor - 验收人
- related_project_id, related_project_name - 关联项目
- related_order_id, related_order_name - 关联订单
- acceptance_requirements - 验收要求
- acceptance_organization - 验收机构

**变更数据**
- change_reason - 变更原因

**实际数据**
- deliverables - 提交物
- code_repository_url - 代码仓库地址
- demo_url - DEMO地址
- actual_acceptance_date - 实际验收日期

**系统审计数据**
- status - 状态
- estimated_acceptance_month - 预估验收年月
- pre_register_time - 预注册时间
- register_time - 注册时间
- record_time - 登记时间
- created_by, updated_by - 创建人/更新人
- created_at, updated_at - 创建时间/更新时间

**其他**
- risk_tags - 风险标签（JSON数组）

#### 索引

```sql
CREATE INDEX idx_achievements_product_id ON achievements(product_id);
CREATE INDEX idx_achievements_module_id ON achievements(module_id);
CREATE INDEX idx_achievements_status ON achievements(status);
CREATE INDEX idx_achievements_type ON achievements(type);
CREATE INDEX idx_achievements_owner ON achievements(owner);
CREATE INDEX idx_achievements_pre_register_time ON achievements(pre_register_time);
CREATE INDEX idx_achievements_product_external_version ON achievements(product_external_version);
```

---

### 2. 成果状态变更记录表 (achievement_status_records)

记录成果状态变更的历史，用于追踪上架、下架、删除等操作。

#### 字段说明

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | VARCHAR(50) | 是 | 主键 |
| achievement_id | VARCHAR(50) | 是 | 成果ID（外键） |
| achievement_name | VARCHAR(200) | 是 | 成果名称 |
| from_status | VARCHAR(50) | 是 | 变更前状态 |
| to_status | VARCHAR(50) | 是 | 变更后状态 |
| change_type | VARCHAR(50) | 是 | 变更类型（上架/下架/删除） |
| change_reason | TEXT | 否 | 变更原因 |
| operator | VARCHAR(100) | 否 | 操作人 |
| change_time | TIMESTAMP | 是 | 变更时间 |
| created_at | TIMESTAMP | 是 | 创建时间 |

---

### 3. 成果版本变更记录表 (achievement_version_records)

记录成果版本变更的详细历史，包括变更的字段和内容。

#### 字段说明

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | VARCHAR(50) | 是 | 主键 |
| achievement_name | VARCHAR(100) | 是 | 成果名称 |
| achievement_id | VARCHAR(50) | 是 | 变更前的成果ID |
| product_external_version | VARCHAR(20) | 是 | 产品对外版本号 |
| from_version | VARCHAR(20) | 是 | 变更前版本号 |
| to_version | VARCHAR(20) | 是 | 变更后版本号 |
| changed_fields | JSON | 是 | 变更字段详情 |
| change_description | TEXT | 否 | 变更说明 |
| risk_tags | JSON | 否 | 风险标签 |
| operator | VARCHAR(50) | 否 | 操作人 |
| change_time | TIMESTAMP | 是 | 变更时间 |
| created_at | TIMESTAMP | 是 | 创建时间 |

#### changed_fields JSON格式示例

```json
{
  "description": {
    "old": "旧描述",
    "new": "新描述"
  },
  "owner": {
    "old": "旧负责人",
    "new": "新负责人"
  }
}
```

---

### 4. 成果附件表 (achievement_attachments)

存储成果相关的附件信息。

#### 字段说明

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | VARCHAR(20) | 是 | 主键 |
| achievement_id | VARCHAR(50) | 是 | 成果ID（外键） |
| achievement_version | VARCHAR(20) | 否 | 成果版本 |
| name | VARCHAR(255) | 是 | 文件名 |
| type | VARCHAR(100) | 否 | 文件类型 |
| size | INTEGER | 否 | 文件大小（字节） |
| upload_time | TIMESTAMP | 是 | 上传时间 |
| uploader | VARCHAR(50) | 否 | 上传人 |
| file_path | VARCHAR(500) | 否 | 文件存储路径 |
| created_at | TIMESTAMP | 是 | 创建时间 |

---

### 5. 成果出库历史表 (achievement_checkout_history)

记录成果的出库历史。

#### 字段说明

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | VARCHAR(20) | 是 | 主键 |
| achievement_id | VARCHAR(50) | 是 | 成果ID（外键） |
| achievement_version | VARCHAR(20) | 否 | 成果版本 |
| user_id | VARCHAR(20) | 否 | 用户ID（外键） |
| user_name | VARCHAR(50) | 否 | 用户名 |
| checkout_time | TIMESTAMP | 是 | 出库时间 |
| purpose | TEXT | 否 | 使用目的 |
| download_count | INTEGER | 否 | 下载次数，默认0 |
| return_time | TIMESTAMP | 否 | 归还时间 |
| created_at | TIMESTAMP | 是 | 创建时间 |

---

## 表关系图

```
┌─────────────────┐
│     users       │
└────────┬────────┘
         │
         │ 1:N
         ▼
┌─────────────────┐      ┌─────────────────────────┐
│    products     │──────│      achievements       │
└─────────────────┘      └───────────┬─────────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         │                           │                           │
         ▼                           ▼                           ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│ achievement_status  │   │ achievement_version │   │ achievement_        │
│ _records            │   │ _records            │   │ attachments         │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘

         │                           │
         ▼                           ▼
┌─────────────────────┐   ┌─────────────────────┐
│ achievement_        │   │ achievement_        │
│ checkout_history    │   │ version_history     │
└─────────────────────┘   └─────────────────────┘
```

---

## 状态流转

```
┌──────────────┐
│ pre_register │ (预注册)
└──────┬───────┘
       │
       ├─── register() ──────────────►┌──────────┐
       │                              │ register │ (注册)
       │                              └────┬─────┘
       │                                   │
       │                                   ├─── record() ────────►┌────────┐
       │                                   │                      │ record │ (登记)
       │                                   │                      └───┬────┘
       │                                   │                          │
       │                                   │                          ├─── offline() ───►┌─────────┐
       │                                   │                          │                  │ offline │ (下架)
       │                                   │                          │                  └────┬────┘
       │                                   │                          │                       │
       │                                   │                          │                       └─── online() ──► record
       │                                   │                          │
       └─── delete() ──────────────────────┴──────────────────────────┴───────────────────►┌─────────┐
                                                                                          │ deleted │ (已删除)
                                                                                          └─────────┘
```

---

## 数据迁移注意事项

1. **ID生成规则**: 成果ID格式为 `{成果名称}_{版本号}`，需要确保唯一性
2. **版本号格式**: 建议使用 `V{主版本}.{次版本}.{修订号}` 格式
3. **JSON字段**: `package_ids`、`risk_tags`、`changed_fields` 使用JSON存储数组或对象
4. **时间字段**: 所有时间字段使用带时区的TIMESTAMP类型
5. **外键约束**: 删除成果时会级联删除相关的状态记录、版本记录等
