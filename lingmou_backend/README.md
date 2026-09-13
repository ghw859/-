# ICBC · 灵枢 后端服务

## 项目简介
工行杯金融科技创新大赛参赛作品后端服务，为前端Vue3项目提供RESTful API。

## 技术栈
- Spring Boot 3.2 + Java 21
- MyBatis-Plus（ORM）
- MySQL 8.0（主数据库）
- Redis（缓存/排队/验证码）
- JWT（登录认证）
- WebSocket（叫号推送）
- SpringDoc OpenAPI（Swagger接口文档）
- Hutool（工具库）

## 快速开始

### 1. 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.8+（或用IDEA自带）

### 2. 配置数据库
```sql
CREATE DATABASE lingmou DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 3. 修改配置
编辑 `src/main/resources/application.yml`：
- 修改 `spring.datasource.password` 为你的MySQL密码
- 修改 `jwt.secret` 为自定义密钥

### 4. 启动项目
```bash
# 方式一：命令行
mvn spring-boot:run

# 方式二：IDEA中运行
打开 LingmouApplication.java → 右键 Run
```

### 5. 验证启动
- 浏览器访问 http://localhost:8080/api/test → 看到 `{"code":0,"msg":"服务启动成功"}`
- 接口文档 http://localhost:8080/swagger-ui.html

## 接口文档

### 在线文档与导出

| 方式 | 地址/命令 |
|------|-----------|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| 一键导出 openapi.json | 后端启动后执行 `powershell -ExecutionPolicy Bypass -File .\export-openapi.ps1` |
| 导入 Postman | Postman → Import → 选择 `docs/Lingmou_API.postman_collection.json`（已含全部端点与示例，登录请求自动保存 token） |
| 导出后导入 Postman | Postman → Import → File → 选择导出的 `openapi.json` |

### 统一约定

- **Base URL**：`http://localhost:8080`（AI 服务 `http://localhost:8000`，前端 Vite 代理 `/api`）
- **返回格式**：`{"code": 0, "msg": "success", "data": {...}}`，`code=0` 成功；业务失败也是 HTTP 200 + 错误码
- **鉴权**：除下表标注"公开"外，均需 `Authorization: Bearer <token>`（登录接口返回）
- **用户隔离**：所有"我的xx"类接口按 token 中的 userId 强制隔离

### 错误码总表（5位数）

| 码段 | 模块 |
|------|------|
| 0 | 成功 |
| 1xxxx | 通用/用户认证 |
| 2xxxx | 预约管理 |
| 3xxxx | 网点管理 |
| 4xxxx | 预填单 |
| 5xxxx | 审计 |
| 6xxxx | AI |
| 9xxxx | 系统 |

| 错误码 | 含义 | | 错误码 | 含义 |
|--------|------|-|--------|------|
| 10001 | 参数错误 | | 20007 | 凭证号不存在 |
| 10002 | 未登录或登录已过期 | | 20008 | 凭证号生成失败 |
| 10003 | 无权限 | | 30001 | 网点不存在 |
| 10004 | 资源不存在 | | 30002 | 网点已关门 |
| 11001 | 用户不存在 | | 40001 | AI解析失败 |
| 11002 | 密码错误 | | 40002 | 审计记录不存在 |
| 11003 | 用户名已存在 | | 40003 | 签名上传失败 |
| 11004 | 手机号已注册 | | 40004 | PDF生成失败 |
| 11005 | 身份证已注册 | | 50001 | 审计拦截 |
| 11006 | 验证码错误或已过期 | | 50002 | 审计链校验失败 |
| 11007 | 验证码发送失败 | | 60001 | AI服务异常 |
| 11008 | Token无效 | | 60002 | AI服务超时 |
| 11009 | 信用分过低 | | 60003 | AI预检未通过 |
| 20001 | 该时段已在其他网点预约 | | 60004 | 未知业务类型 |
| 20002 | 该时段已约满 | | 90000 | 系统繁忙 |
| 20003 | 预约不存在 | | 90001 | 远程服务调用失败 |
| 20004 | 预约已过期 | | | |
| 20005 | 预约已完成 | | | |
| 20006 | 预约已取消 | | | |

### 端点总表

#### 1. 用户认证 `/api/auth`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/login` | 公开 | 登录，返回 `data.token`（JWT） |
| POST | `/register` | 公开 | 注册；body：username/password/realName/phone 必填，idCard 可选 |
| POST | `/logout` | 登录 | 登出，token 失效 |

#### 2. 预约管理 `/api/appointments`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 创建预约；body：`{branchId, businessType, appointmentDate, timeSlot}`；返回 `id/voucherNum(V凭证)/queueNumber` |
| GET | `/my` | 我的预约列表；`?simple=true` 返回简化版 |
| GET | `/{id}` | 预约详情 |
| DELETE | `/{id}` | 取消预约 |
| GET | `/{id}/progress` | 查询办理进度 |
| PUT | `/{id}/progress` | 手动推进到下一步（无请求体） |
| GET | `/history` | 历史分页查询；`?pageNum&pageSize&branchId&businessType&status&startDate&endDate&simple` |

#### 3. 预填单 `/api/preforms`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 创建；body：`{businessType, rawText, parsedJson?, imageUrls?, signatureUrl?}`；businessType 用前端小写枚举（cash_reserve/open_card/corp_transfer/cash_deposit/fx_exchange） |
| GET | `/my` | 我的预填单列表 |
| GET | `/{id}` | 详情 |
| PUT | `/{id}` | 更新 |
| DELETE | `/{id}` | 删除 |

#### 4. 网点 `/api/branches`（全部公开）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 网点列表（id 为字符串 b1-b6，status 小写 free/moderate/busy） |
| GET | `/{id}` | 网点详情 |
| GET | `/recommend` | AI 推荐网点；`?businessType=...` |

#### 5. 业务直通码 `/api/qrcode`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/generate` | 登录 | **T3 主链路**：材料归一化 → Python `/api/ai/precheck` 预检 → 签发 T 码 → 写 vouchers + 审计哈希链。详见下方示例 |
| GET | `/{voucherNum}/image` | 公开 | ZXing 生成 PNG 二维码，`?size=300` 可调；T/V 均支持，可直接 `<img src>` |
| PUT | `/{voucherNum}/bindAppointment` | 登录 | **Day9 #4 新增**：预约创建后回填 `appointment_id`，`?appointmentId={预约id}`。详见下方说明 |

`POST /generate` 请求/响应示例：
```json
// 请求
{
  "businessType": "corp_transfer",
  "materials": {
    "userName": "张三", "idCard": "110101...", "phone": "138...",
    "payerName": "北京灵枢科技有限公司", "payerAccount": "6222...",
    "payeeName": "李四", "payeeAccount": "6222...", "amount": "50000"
  },
  "preFormId": 1,
  "appointmentId": null,
  "branchId": null
}

// 预检不通过（不算错误，HTTP 200）
{ "code": 0, "msg": "材料预检未通过，请按提示补齐材料",
  "data": { "passed": false, "missing": ["payeeName"],
            "missingLabels": ["收款单位名称"], "required": ["idCard","payerName","payerAccount","payeeName","payeeAccount","amount"] } }

// 预检通过
{ "code": 0, "msg": "业务直通码生成成功",
  "data": { "passed": true, "voucherNum": "TBK002026091300013C2C", "sn": "SN1789...",
            "qrcodeImageUrl": "/api/qrcode/TBK002026091300013C2C/image",
            "auditSynced": true, "appointmentId": null, "preFormId": 1, ... } }
```

`PUT /{voucherNum}/bindAppointment?appointmentId=1`（Day9 #4）：
- 背景：前端流程是"预填单 → 直通码页（调 generate）→ 去预约"，generate 时预约尚不存在，`appointmentId` 传不进去，vouchers 表 `appointment_id` 为 null
- T 码已有行 → 直接更新 `appointment_id`；V 号不在 vouchers 表（历史数据）→ 幂等补落关联行；T 码不存在 → `20007`
- 预约必须存在（`20003`）且属于当前登录用户（否则 `10003`）
- 前端握手：预约创建成功后，从 `sessionStorage["lingmou_last_qrcode"]` 取 `voucherNum`（QRCodeView 签发成功时已写入），调本接口一次即可；幂等可重试

#### 6. 凭证查询 `/api/vouchers`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/{voucherNum}` | 登录 | 凭证详情；Day9 起双格式互认（T 直通码 CRC 校验 / V 预约凭证格式校验），返回 `data.kind = DIRECT_CODE / APPOINTMENT`；V 号缺 vouchers 行时回查预约 |
| GET | `/{voucherNum}/validate` | 公开 | 凭证校验 |
| GET | `/rule` | 公开 | 凭证号规则说明 |

#### 7. 区块链审计 `/api/audit`
Hash 公式：`SHA256(prevHash + operatorId + operatorName + action + content)`，链首 prevHash 为 64 个 0。

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/my` | 登录 | 我的存证（data 直接为数组）：`id/sn/bizType/bizTypeName/userName/idCardMasked/phoneMasked/extraData/timestamp/hash/status/voucherNum/hashValid`，PII 脱敏 |
| GET | `/my/verify` | 登录 | 我的存证链校验，返回 `{intact}` |
| GET | `/logs` | AUDITOR/RISK/ADMIN | 全量日志分页；`?action=VOUCHER_GENERATE&pageNum=1&pageSize=20` |
| GET | `/logs/verify` | AUDITOR/RISK/ADMIN | 全链完整性校验，返回 `{intact}` |

#### 8. 数据总览 `/api/overview`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 总览卡片 |
| GET | `/assets-chart` | 资产走势 |
| GET | `/credit-trend` | 信用分趋势 |
| GET | `/business-pie` | 业务分布饼图 |

#### 9. 文件上传 `/api/files`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/upload` | form-data `file` 单文件 |
| POST | `/upload/batch` | form-data `files` 批量 |
| DELETE | `/delete?fileName=` | 删除文件 |

#### 10. 信用分 `/api/credit`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/` | 登录 | 我的信用分及变更历史 |
| POST | `/adjust` | AUDITOR/RISK/ADMIN | `?targetUserId&amount&reason` |

#### 11. AI 服务（Python FastAPI，:8000，全部公开，Vite 代理 `/api/ai`）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/health` | 健康检查 |
| POST | `/api/ai/precheck` | 材料预检（纯规则）；body：`{businessType, materials}`；大写枚举 + TRANSFER/LOAN 别名；未知业务 `60004` |
| POST | `/api/ai/chat` | 数字人对话（规则版）；body：`{sessionId, message}` |
| WS | `/api/ai/chat/ws` | 数字人对话（WebSocket） |
| POST | `/api/ai/parse-preform` | 预填单解析 |
| GET | `/api/ai/heatmap` | 网点热力图 |
| GET | `/api/ai/diagnosis` | 网点诊断 |

### Day9 关键业务链路

```
预填单(POST /api/preforms)
  → 直通码页(POST /api/qrcode/generate：AI预检 → T码 + 审计上链)
  → 预约(POST /api/appointments → 返回 V 凭证)
  → 绑定(PUT /api/qrcode/{voucherNum}/bindAppointment?appointmentId=xxx)   ← Day9 #4
  → 审计页(GET /api/audit/my、/my/verify)
  → 扫码(GET /api/qrcode/{num}/image，T/V 通吃)
```

## 项目结构
```
src/main/java/com/icbc/lingmou/
├── controller/        ← 接口层（接收HTTP请求）
├── service/           ← 业务逻辑层
│   └── impl/
│   └── support/       ← 适配层（PrecheckBizSupport：前端枚举/材料字段归一化）
├── mapper/            ← 数据库操作层（MyBatis-Plus接口）
├── entity/            ← 数据库实体类
├── dto/               ← 数据传输对象
│   ├── request/       ← 请求参数
│   └── response/      ← 返回结果
├── common/            ← 公共类（统一返回/异常处理/错误码）
├── config/            ← 配置类（跨域/Redis/JWT/MyBatis-Plus/Web）
├── utils/             ← 工具类（JWT/SM3/脱敏）
├── interceptor/       ← 拦截器（登录校验）
└── LingmouApplication.java  ← 启动类
```

## 三人分工

| 人员 | 负责模块 | 接口数量 |
|------|---------|---------|
| A | 用户认证 + 预约管理 + 排队叫号 | 19个 |
| B | 网点管理 + 数据总览看板 + 直通码/凭证/审计客户侧 | 14个 |
| C | 预填单/审计存证 + AI服务 | 15个 |

## Git分支策略
```
main        ← 受保护，只合并通过的代码
dev         ← 日常开发合并
feature/xxx ← 各自功能分支
```
