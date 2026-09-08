# API 接口文档 - Day 3

> 本文档为团队共用，Java-A 负责的接口已标注 👤

## 📍 基础信息

| 项 | 值 |
|---|---|
| 基础 URL | `http://localhost:8080` |
| 认证方式 | Bearer Token (JWT) |
| 编码 | UTF-8 |
| Content-Type | `application/json` |

---

## 📅 预约模块 👤

### 7. 创建预约

```
POST /api/appointments
```

**请求头：**
```
Authorization: Bearer <token>
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| branchId | long | ✅ | 网点ID | `1` |
| businessType | string | ✅ | 业务类型 | `开户` |
| appointmentDate | string | ✅ | 预约日期 | `2026-09-10` |
| timeSlot | string | ✅ | 时段 | `09:00-09:30` |

**请求示例：**
```json
{
  "branchId": 1,
  "businessType": "开户",
  "appointmentDate": "2026-09-10",
  "timeSlot": "09:00-09:30"
}
```

**响应示例：**
```json
{
  "code": 0,
  "msg": "预约创建成功",
  "data": {
    "id": 1,
    "userId": 1,
    "branchId": 1,
    "branchName": "工商银行北京西单支行",
    "businessType": "开户",
    "appointmentDate": "2026-09-10",
    "timeSlot": "09:00-09:30",
    "queueNumber": "1001",
    "status": "VIRTUAL",
    "voucherNum": "V1721234567890",
    "progressStep": 0,
    "createdAt": "2026-09-08T15:00:00"
  }
}
```

**预约规则：**
- 每时段最多 10 人
- 时段格式：`HH:00-HH:30`（半小时为一时段）
- 同日同时段不可跨网点重复预约

**错误码：**
| code | 说明 |
|------|------|
| 2001 | 该时段已在其他网点预约 |
| 2002 | 该时段已约满 |
| 3001 | 网点不存在 |

---

### 8. 我的预约

```
GET /api/appointments/my
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
      "branchId": 1,
      "branchName": "工商银行北京西单支行",
      "businessType": "开户",
      "appointmentDate": "2026-09-10",
      "timeSlot": "09:00-09:30",
      "queueNumber": "1001",
      "status": "VIRTUAL",
      "voucherNum": "V1721234567890",
      "progressStep": 0,
      "createdAt": "2026-09-08T15:00:00"
    }
  ]
}
```

---

### 9. 预约详情

```
GET /api/appointments/{id}
```

**请求头：**
```
Authorization: Bearer <token>
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 预约ID |

**响应示例：**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "userId": 1,
    "branchId": 1,
    "branchName": "工商银行北京西单支行",
    "businessType": "开户",
    "appointmentDate": "2026-09-10",
    "timeSlot": "09:00-09:30",
    "queueNumber": "1001",
    "status": "VIRTUAL",
    "voucherNum": "V1721234567890",
    "progressStep": 0,
    "createdAt": "2026-09-08T15:00:00"
  }
}
```

**错误码：**
| code | 说明 |
|------|------|
| 2003 | 预约不存在 |
| 1003 | 无权限（不是自己的预约） |

---

### 10. 取消预约

```
DELETE /api/appointments/{id}
```

**请求头：**
```
Authorization: Bearer <token>
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 预约ID |

**响应示例：**
```json
{
  "code": 0,
  "msg": "预约已取消",
  "data": null
}
```

**错误码：**
| code | 说明 |
|------|------|
| 2003 | 预约不存在 |
| 1003 | 无权限（不是自己的预约） |
| 2006 | 预约已取消/已完成/已过期，不能重复取消 |

---

## 📋 预约状态说明

| 状态 | 说明 |
|------|------|
| VIRTUAL | 虚拟（刚创建） |
| ACTIVE | 已激活 |
| CALLED | 已叫号 |
| PROCESSING | 办理中 |
| COMPLETED | 已完成 |
| EXPIRED | 已过期 |
| CANCELED | 已取消 |

---

## 📋 统一响应结构

```json
{
  "code": 0,      // 0=成功，其他=错误码
  "msg": "success", // 提示信息
  "data": {}       // 实际数据
}
```

**错误码对照表（完整版）：**

| 区间 | 模块 | 错误码 | 说明 |
|------|------|--------|------|
| 1xxxx | 用户/通用 | 1001 | 参数错误 |
| | | 1002 | 未登录 |
| | | 1003 | 无权限 |
| | | 1004 | 资源不存在 |
| | | 1101 | 用户不存在 |
| | | 1102 | 密码错误 |
| | | 1103 | 用户名已存在 |
| | | 1104 | 手机号已注册 |
| 2xxxx | 预约 | 2001 | 该时段已在其他网点预约 |
| | | 2002 | 该时段已约满 |
| | | 2003 | 预约不存在 |
| | | 2004 | 预约已过期 |
| | | 2005 | 预约已完成 |
| | | 2006 | 预约已取消 |
| | | 2007 | 凭证号不存在 |
| 3xxxx | 网点 | 3001 | 网点不存在 |
| | | 3002 | 网点已关门 |
| 4xxxx | 预填单/审计 | 4001 | AI解析失败 |
| | | 4002 | 审计记录不存在 |
| 5xxxx | 系统/AI | 5000 | 系统繁忙 |
| | | 5001 | AI服务异常 |
| | | 5002 | AI服务超时 |

---

## 🔑 认证说明

### 如何获取 Token

1. 调用 `/api/auth/login` 或 `/api/auth/register`
2. 响应中的 `data.token` 就是 JWT Token

### 如何携带 Token

在请求头中添加：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 无需登录的接口

- `/api/auth/*`（登录、注册）
- `/api/branches/*`（网点查询）
- `/api/test/*`（测试接口）
- `/swagger/**`（API 文档）

---

## ⚠️ 注意事项

1. **预约规则**：每时段 ≤10 人，半小时为一时段，同日同时段不可跨网点重复
2. **权限校验**：查询/取消预约只能操作自己的（后端强制校验）
3. **Redis**：登出功能依赖 Redis，预约功能不依赖
4. **状态推进**：进度状态由人工按钮推进，禁止自动定时推进

---

## 🐛 常见问题

**Q: 返回 2001 该时段已在其他网点预约？**
A: 同一用户同日同时段只能有一个预约，不管哪个网点

**Q: 返回 2002 该时段已约满？**
A: 该时段预约人数已达 10 人上限

**Q: 返回 1003 无权限？**
A: 尝试访问或取消他人预约，后端强制校验 userId

**Q: Redis 连接失败？**
A: 登出功能依赖 Redis，确保 Redis 运行（预约功能不依赖）
