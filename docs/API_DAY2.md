# API 接口文档 - Day 2

> 本文档为团队共用，Java-A 负责的接口已标注 👤

## 📍 基础信息

| 项 | 值 |
|---|---|
| 基础 URL | `http://localhost:8080` |
| 认证方式 | Bearer Token (JWT) |
| 编码 | UTF-8 |
| Content-Type | `application/json` |

---

## 🔐 认证模块 👤

### 1. 用户注册

```
POST /api/auth/register
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| username | string | ✅ | 用户名(4-20位) | `zhangsan` |
| password | string | ✅ | 密码(6-20位) | `123456` |
| realName | string | ✅ | 真实姓名 | `张三` |
| phone | string | ✅ | 手机号 | `13800138000` |
| idCard | string | ❌ | 身份证号 | `110101199001011234` |

**请求示例：**
```json
{
  "username": "zhangsan",
  "password": "123456",
  "realName": "张三",
  "phone": "13800138000",
  "idCard": "110101199001011234"
}
```

**响应示例：**
```json
{
  "code": 0,
  "msg": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "userInfo": {
      "id": 1,
      "username": "zhangsan",
      "realName": "张三",
      "phone": "13800138000",
      "role": "CUSTOMER",
      "customerLevel": "NORMAL",
      "creditScore": 100,
      "elderlyMode": 0
    }
  }
}
```

---

### 2. 用户登录

```
POST /api/auth/login
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| username | string | ✅ | 用户名或手机号 | `zhangsan` |
| password | string | ✅ | 密码 | `123456` |

**请求示例：**
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**响应示例：**
```json
{
  "code": 0,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "userInfo": {
      "id": 1,
      "username": "zhangsan",
      "realName": "张三",
      "phone": "13800138000",
      "role": "CUSTOMER",
      "customerLevel": "NORMAL",
      "creditScore": 100,
      "elderlyMode": 0
    }
  }
}
```

**错误码：**
| code | 说明 |
|------|------|
| 1101 | 用户不存在 |
| 1102 | 密码错误 |

---

### 3. 用户登出

```
POST /api/auth/logout
```

**请求头：**
```
Authorization: Bearer <登录返回的token>
```

**响应示例：**
```json
{
  "code": 0,
  "msg": "登出成功",
  "data": null
}
```

---

## 🏦 网点模块 👤

### 4. 查询所有网点

```
GET /api/branches
```

**无需登录**

**响应示例：**
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "工商银行北京西单支行",
      "address": "北京市西城区西单北大街120号",
      "businessHours": "09:00-17:00",
      "currentQueue": 0,
      "busyLevel": "IDLE"
    },
    {
      "id": 2,
      "name": "工商银行上海浦东支行",
      "address": "上海市浦东新区世纪大道100号",
      "businessHours": "09:00-17:00",
      "currentQueue": 0,
      "busyLevel": "IDLE"
    }
    // ... 共4个网点
  ]
}
```

---

### 5. 查询单个网点

```
GET /api/branches/{id}
```

**无需登录**

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 网点ID |

**响应示例：**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "工商银行北京西单支行",
    "address": "北京市西城区西单北大街120号",
    "businessHours": "09:00-17:00",
    "currentQueue": 0,
    "busyLevel": "IDLE"
  }
}
```

**错误码：**
| code | 说明 |
|------|------|
| 3001 | 网点不存在 |

---

### 6. 推荐网点（按繁忙程度排序）

```
GET /api/branches/recommend
```

**无需登录**

**响应示例：**
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "工商银行北京西单支行",
      "busyLevel": "IDLE"
    },
    {
      "id": 2,
      "name": "工商银行上海浦东支行",
      "busyLevel": "MODERATE"
    },
    {
      "id": 3,
      "name": "工商银行深圳南山支行",
      "busyLevel": "BUSY"
    }
    // 按 IDLE -> MODERATE -> BUSY 排序
  ]
}
```

---

## 📋 统一响应结构

```json
{
  "code": 0,      // 0=成功，其他=错误码
  "msg": "success", // 提示信息
  "data": {}       // 实际数据
}
```

**错误码对照表：**

| 区间 | 模块 |
|------|------|
| 1xxxx | 用户/通用 |
| 2xxxx | 预约 |
| 3xxxx | 网点 |
| 4xxxx | 预填单/审计 |
| 5xxxx | 系统/AI |

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

### Token 有效期

默认 24 小时（86400 秒），可在 `application-local.yml` 中配置 `jwt.expiration`

---

## ⚠️ 注意事项

1. **密码加密**：用户密码使用 BCrypt 加密存储，登录时自动校验
2. **无需登录的接口**：登录、注册、网点查询、swagger 文档
3. **Redis**：Token 黑名单存储在 Redis，登出后原 Token 失效
4. **错误处理**：所有异常由 `GlobalExceptionHandler` 统一处理

---

## 🐛 常见问题

**Q: 返回 401 未授权？**
A: 检查请求头是否正确携带 `Authorization: Bearer <token>`

**Q: 返回 1101 用户不存在？**
A: 先调用注册接口，用户名不能重复

**Q: Spring Security 阻止请求？**
A: 已配置所有请求放行，检查是否引入了其他安全配置

**Q: Redis 连接失败？**
A: 登出功能依赖 Redis，确保 Redis 服务运行中
