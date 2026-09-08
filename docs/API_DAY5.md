# API 接口文档 - Day 5

> 本文档为团队共用，Java-A 负责的接口已标注 👤

## 📍 基础信息

| 项 | 值 |
|---|---|
| 基础 URL | `http://localhost:8080` |
| 认证方式 | Bearer Token (JWT) |
| 编码 | UTF-8 |
| Content-Type | `application/json` |

---

## 📄 预填单模块 👤

### 13. 创建预填单

```
POST /api/preforms
```

**请求头：**
```
Authorization: Bearer <token>
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| businessType | string | ✅ | 业务类型 | `开户` |
| rawText | string | ✅ | 原始文本内容 | `姓名：张三\n...` |
| parsedJson | string | ❌ | 解析后的JSON | `{}` |

**请求示例：**
```json
{
  "businessType": "开户",
  "rawText": "姓名：张三\n身份证号：110101199001011234\n手机号：13800138000",
  "parsedJson": "{\"name\":\"张三\",\"idCard\":\"...\"}"
}
```

**响应示例：**
```json
{
  "code": 0,
  "msg": "预填单创建成功",
  "data": {
    "id": 1,
    "userId": 1,
    "businessType": "开户",
    "rawText": "姓名：张三\n身份证号：...",
    "parsedJson": "{}",
    "status": "DRAFT",
    "createdAt": "2026-09-08T15:00:00",
    "updatedAt": "2026-09-08T15:00:00"
  }
}
```

---

### 14. 我的预填单

```
GET /api/preforms/my
```

**请求头：**
```
Authorization: Bearer <token>
```

**响应示例：**
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "businessType": "开户",
      "rawText": "姓名：张三\n...",
      "parsedJson": "{}",
      "status": "DRAFT",
      "createdAt": "2026-09-08T15:00:00",
      "updatedAt": "2026-09-08T15:00:00"
    }
  ]
}
```

---

### 15. 预填单详情

```
GET /api/preforms/{id}
```

**请求头：**
```
Authorization: Bearer <token>
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 预填单ID |

**响应示例：**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "userId": 1,
    "businessType": "开户",
    "rawText": "姓名：张三\n身份证号：...",
    "parsedJson": "{}",
    "status": "DRAFT",
    "createdAt": "2026-09-08T15:00:00",
    "updatedAt": "2026-09-08T15:00:00"
  }
}
```

**错误码：**
| code | 说明 |
|------|------|
| 1004 | 资源不存在 |
| 1003 | 无权限（不是自己的） |

---

### 16. 更新预填单

```
PUT /api/preforms/{id}
```

**请求头：**
```
Authorization: Bearer <token>
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 预填单ID |

**请求示例：**
```json
{
  "businessType": "开户",
  "rawText": "姓名：李四\n...",
  "parsedJson": "{}"
}
```

**响应示例：**
```json
{
  "code": 0,
  "msg": "预填单更新成功",
  "data": { ... }
}
```

---

### 17. 删除预填单

```
DELETE /api/preforms/{id}
```

**请求头：**
```
Authorization: Bearer <token>
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 预填单ID |

**响应示例：**
```json
{
  "code": 0,
  "msg": "预填单已删除",
  "data": null
}
```

---

## 📅 历史预约查询 👤

### 18. 历史预约查询

```
GET /api/appointments/history
```

**请求头：**
```
Authorization: Bearer <token>
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| branchId | long | ❌ | 网点ID | `1` |
| businessType | string | ❌ | 业务类型 | `开户` |
| status | string | ❌ | 状态 | `COMPLETED` |
| startDate | string | ❌ | 开始日期 | `2026-09-01` |
| endDate | string | ❌ | 结束日期 | `2026-09-30` |
| pageNum | int | ❌ | 页码（默认1） | `1` |
| pageSize | int | ❌ | 每页大小（默认10） | `10` |

**响应示例：**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 25,
    "totalPages": 3,
    "records": [
      {
        "id": 1,
        "userId": 1,
        "branchId": 1,
        "branchName": "工商银行北京西单支行",
        "businessType": "开户",
        "appointmentDate": "2026-09-10",
        "timeSlot": "09:00-09:30",
        "queueNumber": "1001",
        "status": "COMPLETED",
        "voucherNum": "V1721234567890",
        "progressStep": 4,
        "createdAt": "2026-09-08T15:00:00"
      }
    ]
  }
}
```

**重要：强制按 userId 过滤，只返回当前用户的预约**

---

## 📋 预填单状态说明

| 状态 | 说明 |
|------|------|
| DRAFT | 草稿 |
| SUBMITTED | 已提交 |
| USED | 已使用 |

---

## 📋 统一响应结构

```json
{
  "code": 0,      // 0=成功，其他=错误码
  "msg": "success", // 提示信息
  "data": {}       // 实际数据
}
```

---

## ⚠️ 注意事项

1. **预填单解析**：前端用正则解析，后端 Python 只做规则兜底（不调大模型）
2. **历史查询**：强制按 userId 过滤，只能查自己的
3. **分页**：pageNum 从 1 开始，pageSize 默认 10

---

## 🐛 常见问题

**Q: 预填单状态有哪些？**
A: DRAFT（草稿）、SUBMITTED（已提交）、USED（已使用）

**Q: 历史查询能查别人的预约吗？**
A: 不能，后端强制按 userId 过滤
