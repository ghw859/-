# Day 3-4 总结

## ✅ 已完成

### Day 3 - 智能预约排队

**接口：**
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/appointments` | POST | 创建预约 |
| `/api/appointments/my` | GET | 我的预约 |
| `/api/appointments/{id}` | GET | 预约详情 |
| `/api/appointments/{id}` | DELETE | 取消预约 |

**规则实现：**
- 每时段最多 10 人
- 半小时为一时段（如 `09:00-09:30`）
- 同日同时段不可跨网点重复预约
- 强制校验 userId，只能操作自己的预约

**新增文件：**
- `AppointmentRequest.java` / `AppointmentResponse.java`
- `AppointmentService.java` + `AppointmentServiceImpl.java`
- `AppointmentController.java`
- `API_DAY3.md`

---

### Day 4 - 办理进度

**接口：**
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/appointments/{id}/progress` | GET | 查询办理进度 |
| `/api/appointments/{id}/progress` | PUT | 推进办理进度 |

**进度状态机（手动推进）：**
```
0 VIRTUAL → 1 ACTIVE → 2 CALLED → 3 PROCESSING → 4 COMPLETED
```

**注意：** 业务直通码是 Java-B 负责的

---

## 📁 项目结构

```
lingmou_backend/src/main/java/com/icbc/lingmou/
├── config/
│   ├── SecurityConfig.java      # BCrypt密码加密
│   └── WebConfig.java           # 拦截器配置
├── controller/
│   ├── AuthController.java      # 登录/注册/登出
│   ├── BranchController.java    # 网点查询
│   └── AppointmentController.java # 预约/进度
├── service/
│   ├── UserService.java
│   ├── AuthService.java
│   ├── BranchService.java
│   └── AppointmentService.java
└── service/impl/
    ├── UserServiceImpl.java
    ├── AuthServiceImpl.java
    ├── BranchServiceImpl.java
    └── AppointmentServiceImpl.java
```

## 🔌 待完成（Java-B 负责）

| 功能 | 接口 |
|------|------|
| 凭证查询 | `GET /api/vouchers/{voucherNum}` |
| 凭证号生成 | 规则实现 |
| 业务直通码 | `POST /api/qrcode/generate` |
| 网点推荐 | 已完成（按繁忙度排序）|

## 📝 待完成（后续天数）

| Day | 功能 | 负责人 |
|-----|------|--------|
| Day 5 | 预填单存储、历史查询 | Java-A + Java-B |
| Day 6 | 数据总览、区块链审计 | Java-A + Java-B |
| Day 7 | 信用分、老年模式 | Java-A + Java-B |
| Day 8-9 | 前后端联调 | 前端 + Java |
| Day 10 | 测试、部署 | 全部 |
