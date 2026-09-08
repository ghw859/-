# API 接口文档 - Day 4

> 本文档为团队共用，Java-A 负责的接口已标注 👤

## 📍 基础信息

| 项 | 值 |
|---|---|
| 基础 URL | `http://localhost:8080` |
| 认证方式 | Bearer Token (JWT) |
| 编码 | UTF-8 |
| Content-Type | `application/json` |

---

## 📅 预约进度模块 👤

### 11. 查询办理进度

```
GET /api/appointments/{id}/progress
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
    "status": "ACTIVE",
    "voucherNum": "V1721234567890",
    "progressStep": 2,
    "createdAt": "2026-09-08T15:00:00"
  }
}
```

**进度步骤说明：**

| progressStep | status | 说明 |
|-------------|--------|------|
| 0 | VIRTUAL | 取号 |
| 1 | ACTIVE | 排队 |
| 2 | CALLED | 叫号 |
| 3 | PROCESSING | 办理中 |
| 4 | COMPLETED | 已完成 |

**错误码：**
| code | 说明 |
|------|------|
| 2003 | 预约不存在 |
| 1003 | 无权限（不是自己的预约） |

---

### 12. 推进办理进度

```
PUT /api/appointments/{id}/progress
```

**请求头：**
```
Authorization: Bearer <token>
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 预约ID |

**业务规则：**
- 每次调用推进一步（0→1→2→3→4）
- 已完成的预约（step=4）不能再推进
- 已取消/已过期的不能推进

**响应示例：**
```json
{
  "code": 0,
  "msg": "进度已推进",
  "data": {
    "id": 1,
    "userId": 1,
    "branchId": 1,
    "branchName": "工商银行北京西单支行",
    "businessType": "开户",
    "appointmentDate": "2026-09-10",
    "timeSlot": "09:00-09:30",
    "queueNumber": "1001",
    "status": "PROCESSING",
    "voucherNum": "V1721234567890",
    "progressStep": 3,
    "createdAt": "2026-09-08T15:00:00"
  }
}
```

**错误码：**
| code | 说明 |
|------|------|
| 2003 | 预约不存在 |
| 1003 | 无权限（不是自己的预约） |
| 2005 | 预约已完成，不能继续推进 |

---

## 📋 预约完整生命周期

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌────────────┐    ┌───────────┐
│ VIRTUAL │───▶│ ACTIVE  │───▶│ CALLED  │───▶│ PROCESSING │───▶│ COMPLETED │
│  取号   │    │  排队   │    │  叫号   │    │   办理中    │    │   已完成   │
└─────────┘    └─────────┘    └─────────┘    └────────────┘    └───────────┘
     │              │               │              │
     ▼              ▼               ▼              ▼
┌─────────┐    ┌─────────┐
│ CANCELED│    │ EXPIRED │
│  已取消  │    │  已过期  │
└─────────┘    └─────────┘
```

**状态流转规则：**
- `VIRTUAL → ACTIVE`：用户到店签到
- `ACTIVE → CALLED`：工作人员叫号
- `CALLED → PROCESSING`：开始办理
- `PROCESSING → COMPLETED`：办理完成
- 任何状态可手动推进，禁止自动定时推进

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

## 🔑 认证说明

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

1. **进度推进**：手动按钮推进，禁止自动定时
2. **权限校验**：查询/推进进度只能操作自己的预约
3. **状态机**：严格按步骤流转，不能跳步

---

## 🐛 常见问题

**Q: 返回 2005 预约已完成？**
A: 已到 step=4，无法继续推进

**Q: 进度推进没反应？**
A: 检查当前 progressStep 是否已达 4

**Q: 状态跳步了？**
A: 正常，每次调用只推进一步

---

## 📝 Java-B 待实现（Day 4）

| 功能 | 说明 |
|------|------|
| 凭证查询 | `GET /api/vouchers/{voucherNum}` |
| 网点繁忙度更新 | 定时更新网点的 busy_level |
