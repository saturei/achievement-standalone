# 成果管理模块 API 接口文档

## 基础信息

- **Base URL**: `/api/achievements`
- **认证方式**: Bearer Token (JWT)
- **数据格式**: JSON

---

## 成果管理接口

### 1. 获取成果列表

**GET** `/api/achievements`

获取成果列表，支持分页和筛选。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| page_size | int | 否 | 每页数量，默认20，最大100 |
| status | string | 否 | 状态筛选：pre_register, register, recorded, offline, deleted |
| type | string | 否 | 类型筛选：模块, 功能, 资产, 文档, 其他 |
| product_id | string | 否 | 产品ID筛选 |
| keyword | string | 否 | 搜索关键词（名称、描述、ID） |
| include_deleted | bool | 否 | 是否包含已删除记录，默认false |

#### 响应示例

```json
{
  "total": 100,
  "page": 1,
  "page_size": 20,
  "items": [
    {
      "id": "成果名称_V1.0.0",
      "name": "成果名称",
      "version": "V1.0.0",
      "product_id": "P001",
      "product_name": "产品名称",
      "module_id": "M001",
      "module_name": "模块名称",
      "type": "模块",
      "description": "成果描述",
      "status": "pre_register",
      "owner": "负责人",
      "pre_register_time": "2024-01-01T00:00:00",
      "register_time": null,
      "record_time": null,
      "created_at": "2024-01-01T00:00:00",
      "updated_at": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### 2. 获取成果统计

**GET** `/api/achievements/statistics`

获取成果统计数据。

#### 响应示例

```json
{
  "total_count": 100,
  "pre_register_count": 20,
  "register_count": 30,
  "record_count": 40,
  "offline_count": 10,
  "by_status": {
    "pre_register": 20,
    "register": 30,
    "recorded": 40,
    "offline": 10
  },
  "by_type": {
    "模块": 50,
    "功能": 30,
    "资产": 20
  },
  "by_product": {
    "P001": 30,
    "P002": 25
  },
  "checkout_count": 0
}
```

---

### 3. 获取成果详情

**GET** `/api/achievements/{achievement_id}`

获取单个成果的详细信息。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 响应示例

```json
{
  "id": "成果名称_V1.0.0",
  "name": "成果名称",
  "version": "V1.0.0",
  "product_id": "P001",
  "product_name": "产品名称",
  "module_id": "M001",
  "module_name": "模块名称",
  "type": "模块",
  "description": "成果描述",
  "status": "pre_register",
  "owner": "负责人",
  "pre_register_time": "2024-01-01T00:00:00",
  "register_time": null,
  "record_time": null,
  "related_order_id": null,
  "related_project_id": null,
  "acceptance_requirements": "验收要求",
  "created_at": "2024-01-01T00:00:00",
  "updated_at": "2024-01-01T00:00:00"
}
```

---

### 4. 成果预注册

**POST** `/api/achievements/pre-register`

创建新的成果预注册记录。

#### 请求体

```json
{
  "name": "成果名称",
  "version": "V1.0.0",
  "product_id": "P001",
  "product_external_version": "V1.0.0",
  "organization_id": "ORG001",
  "organization_name": "机构名称",
  "department_id": "DEPT001",
  "department_name": "部门名称",
  "has_baseline": "无基线",
  "requirement_proposer": "需求提出人",
  "achievement_form": "成果形态",
  "sale_type": "可售类型",
  "application_scenario": "应用场景",
  "type": "模块",
  "description": "成果描述",
  "achievement_target": "成果目标",
  "planned_acceptance_date": "2024-06-30",
  "acceptance_method": "验收方式",
  "acceptor": "验收人",
  "related_project_id": "PROJ001",
  "related_project_name": "项目名称",
  "related_order_id": "ORD001",
  "related_order_name": "订单名称",
  "acceptance_requirements": "验收要求",
  "owner": "负责人",
  "risk_tags": ["风险标签1", "风险标签2"]
}
```

#### 响应示例

返回创建的成果对象。

---

### 5. 成果注册

**POST** `/api/achievements/{achievement_id}/register`

将预注册成果转为注册状态。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 请求体

```json
{
  "register_time": "2024-03-01T00:00:00",
  "register_notes": "注册备注"
}
```

---

### 6. 成果登记

**POST** `/api/achievements/{achievement_id}/record`

将注册成果转为登记状态（验收确权）。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 请求体

```json
{
  "record_time": "2024-06-01T00:00:00",
  "record_notes": "登记备注",
  "related_order_id": "ORD001",
  "related_project_id": "PROJ001"
}
```

---

### 7. 成果变更

**POST** `/api/achievements/{achievement_id}/change`

对成果进行变更，生成新版本。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 请求体

```json
{
  "change_description": "变更说明",
  "description": "新的成果描述",
  "owner": "新的负责人",
  "related_project_id": "PROJ002",
  "related_order_id": "ORD002",
  "acceptance_requirements": "新的验收要求",
  "register_time": "2024-04-01T00:00:00",
  "risk_tags": ["延期"],
  "product_external_version": "V1.1.0",
  "module_id": "M002",
  "module_name": "新模块名称"
}
```

---

### 8. 成果下架

**PUT** `/api/achievements/{achievement_id}/offline`

将成果下架，不再允许出库。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 请求体

```json
{
  "reason": "下架原因"
}
```

---

### 9. 成果上架

**PUT** `/api/achievements/{achievement_id}/online`

将下架的成果重新上架。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 请求体

```json
{
  "reason": "上架原因"
}
```

---

### 10. 成果删除

**PUT** `/api/achievements/{achievement_id}/delete`

逻辑删除预注册状态的成果。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

**注意**: 只有预注册状态的成果才能删除。

---

### 11. 成果出库

**POST** `/api/achievements/{achievement_id}/checkout`

成果出库操作。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 请求体

```json
{
  "purpose": "使用目的",
  "user_id": "U001"
}
```

---

### 12. 获取成果版本历史

**GET** `/api/achievements/{achievement_id}/history`

获取成果的版本历史记录。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| page_size | int | 否 | 每页数量，默认20 |

---

### 13. 获取成果状态变更记录

**GET** `/api/achievements/{achievement_id}/status-records`

获取成果的状态变更历史记录。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| page_size | int | 否 | 每页数量，默认20 |

---

### 14. 更新成果

**PUT** `/api/achievements/{achievement_id}`

更新成果信息。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| achievement_id | string | 是 | 成果ID |

#### 请求体

支持部分更新，只需提供要修改的字段。

---

## 版本记录接口

### 1. 获取版本变更记录列表

**GET** `/api/achievement-version-records`

获取所有成果的版本变更记录。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| page_size | int | 否 | 每页数量，默认20 |
| achievement_name | string | 否 | 成果名称筛选 |
| start_time | datetime | 否 | 开始时间 |
| end_time | datetime | 否 | 结束时间 |
| risk_tag | string | 否 | 风险标签筛选 |

---

### 2. 获取指定成果的版本记录

**GET** `/api/achievement-version-records/{achievement_id}`

获取指定成果的所有版本变更记录。

---

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 成果状态流转

```
预注册 (pre_register)
    ├── 注册 (register) - POST /api/achievements/{id}/register
    │       └── 登记 (record) - POST /api/achievements/{id}/record
    │               ├── 下架 (offline) - PUT /api/achievements/{id}/offline
    │               │       └── 上架 (record) - PUT /api/achievements/{id}/online
    │               └── ...
    └── 删除 (deleted) - PUT /api/achievements/{id}/delete
```
