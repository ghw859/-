# API 接口文档 - Day 6

> 本文档为团队共用，Java-A 负责的接口已标注 👤

## 📍 基础信息

| 项 | 值 |
|---|---|
| 基础 URL | `http://localhost:8080` |
| 认证方式 | Bearer Token (JWT) |
| 编码 | UTF-8 |
| Content-Type | `application/json` |

---

## 📊 数据总览 👤

### 19. 卡片统计数据

```
GET /api/overview
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
    "totalUsers": 156,
    "todayAppointments": 23,
    "totalPreForms": 89,
    "monthlyCompleted": 67,
    "usersChange": 5,
    "appointmentsChange": -2,
    "preFormsChange": 3,
    "completedChange": 8
  }
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| totalUsers | long | 总用户数 |
| todayAppointments | long | 今日预约数 |
| totalPreForms | long | 总预填单数 |
| monthlyCompleted | long | 本月办理完成数 |
| usersChange | int | 用户数较昨日变化 |
| appointmentsChange | int | 预约数较昨日变化 |
| preFormsChange | int | 预填单数较昨日变化 |
| completedChange | int | 完成数较昨日变化 |

---

### 20. 资产趋势图

```
GET /api/overview/assets-chart
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
    "dates": ["09-02", "09-03", "09-04", "09-05", "09-06", "09-07", "09-08"],
    "accounts": [12, 18, 15, 22, 19, 25, 23],
    "deposits": [600000, 900000, 750000, 1100000, 950000, 1250000, 1150000],
    "finances": [360000, 540000, 450000, 660000, 570000, 750000, 690000]
  }
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| dates | List\<String\> | 近7天日期（MM-dd格式） |
| accounts | List\<Integer\> | 每日新增用户数（开户） |
| deposits | List\<Long\> | 每日模拟充值金额（元） |
| finances | List\<Long\> | 每日模拟理财金额（元） |

---

### 21. 信用分趋势

```
GET /api/overview/credit-trend
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
    "dates": ["09-02", "09-03", "09-04", "09-05", "09-06", "09-07", "09-08"],
    "avgScores": [95.2, 95.5, 94.8, 95.0, 95.3, 95.6, 95.4],
    "highScoreUsers": [120, 125, 118, 122, 128, 130, 132],
    "lowScoreUsers": [5, 4, 6, 5, 4, 3, 4]
  }
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| dates | List\<String\> | 近7天日期（MM-dd格式） |
| avgScores | List\<Double\> | 平均信用分（保留1位小数） |
| highScoreUsers | List\<Integer\> | 高分用户数（>=90分） |
| lowScoreUsers | List\<Integer\> | 低分用户数（<70分） |

---

### 22. 业务类型饼图

```
GET /api/overview/business-pie
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
    "items": [
      { "businessType": "开户", "count": 45, "percentage": 35.2 },
      { "businessType": "挂失", "count": 28, "percentage": 21.9 },
      { "businessType": "理财", "count": 22, "percentage": 17.2 },
      { "businessType": "转账", "count": 18, "percentage": 14.1 },
      { "businessType": "其他", "count": 15, "percentage": 11.7 }
    ]
  }
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List\<BusinessItem\> | 业务类型列表 |
| businessType | string | 业务类型名称 |
| count | int | 该类型预约数 |
| percentage | double | 占比（%，保留1位小数） |

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

1. **数据总览**：需要登录后才能访问，未登录返回 1002
2. **图表数据**：资产趋势和信用趋势均为近7天数据
3. **变化值**：表示与昨日相比的增减，正数为涨，负数为跌

---

## 🐛 常见问题

**Q: 卡片数据的变化值是怎么计算的？**
A: 用今日统计值减去昨日统计值，正数表示增长，负数表示下降

**Q: 资产趋势里的金额是怎么算的？**
A: 基于每日预约数模拟估算，开口座均约5万，理财约3万

**Q: 信用趋势里的用户数是怎么统计的？**
A: highScoreUsers 指信用分>=90的用户，lowScoreUsers 指信用分<70的用户
