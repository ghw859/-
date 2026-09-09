# API 接口文档 - Day 7

> 本文档为团队共用，Java-A 负责的接口已标注 👤

## 📍 基础信息

| 项 | 值 |
|---|---|
| 基础 URL | `http://localhost:8080` |
| 认证方式 | Bearer Token (JWT) |
| 编码 | UTF-8 |
| Content-Type | `application/json` |

---

## 💳 信用分模块 👤

### 23. 查询信用分

```
GET /api/credit
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
  "data": {
    "userId": 1,
    "username": "zhangsan",
    "creditScore": 95,
    "creditLevel": "EXCELLENT",
    "customerLevel": "GOLD",
    "changeHistory": [
      {
        "changeType": "ADD",
        "amount": 3,
        "reason": "预约办理完成",
        "createdAt": "2026-09-08T10:30:00"
      },
      {
        "changeType": "DEDUCT",
        "amount": -5,
        "reason": "预约取消",
        "createdAt": "2026-09-07T15:20:00"
      }
    ]
  }
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | long | 用户ID |
| username | string | 用户名 |
| creditScore | int | 当前信用分（0-100） |
| creditLevel | string | 信用等级 |
| customerLevel | string | 客户级别 |
| changeHistory | List | 变更记录（最近10条） |

**信用等级说明：**

| 等级 | 分值范围 | 说明 |
|------|----------|------|
| EXCELLENT | >= 90 | 极好 |
| GOOD | >= 75 | 良好 |
| FAIR | >= 60 | 一般 |
| POOR | < 60 | 较差 |

---

### 24. 调整信用分

```
POST /api/credit/adjust
```

**请求头：**
```
Authorization: Bearer <token>
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| targetUserId | long | ✅ | 目标用户ID | `1` |
| amount | int | ✅ | 调整分数（正数加/负数减） | `5` 或 `-10` |
| reason | string | ❌ | 调整原因 | `按时完成办理` |

**响应示例：**
```json
{
  "code": 0,
  "msg": "信用分调整成功",
  "data": {
    "userId": 1,
    "username": "zhangsan",
    "creditScore": 98,
    "creditLevel": "EXCELLENT",
    "customerLevel": "GOLD",
    "changeHistory": [...]
  }
}
```

**错误码：**
| code | 说明 |
|------|------|
| 1001 | 参数错误（缺少必填参数）|
| 1003 | 无权限（仅 AUDITOR/RISK/ADMIN 可调用）|
| 1101 | 用户不存在 |

**重要约束：**
- 仅 `AUDITOR`、`RISK`、`ADMIN` 角色可调用
- 信用分范围限制在 `[0, 100]`
- 系统自动记录变更人和变更原因

---

## 📋 信用分变更规则

| 触发场景 | 变更值 | 规则 |
|----------|--------|------|
| 预约取消 | -5 | 用户主动取消预约 |
| 预约到期未到 | -10 | 预约过期未使用 |
| 办理完成 | +3 | 预约状态变为 COMPLETED |
| 按时到达 | +1 | 取号后按时办理 |
| 迟到 | -3 | 超过预约时段30分钟以上 |

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

1. **信用分查询**：任何登录用户可查询自己的信用分
2. **信用分调整**：仅管理员角色可调整，且需提供原因
3. **分值限制**：信用分最低0分，最高100分，超出范围自动截断

---

## 🐛 常见问题

**Q: 信用分调整后多久生效？**
A: 实时生效，调用成功即可查到最新分数

**Q: 可以把信用分调到100以上吗？**
A: 不可以，信用分上限为100，超出部分会自动截断

**Q: 普通客户可以查询别人的信用分吗？**
A: 不可以，只能查询自己的信用分

**Q: 信用等级会影响预约吗？**
A: 信用分低于60分（POOR）的用户可能会被限制预约功能
