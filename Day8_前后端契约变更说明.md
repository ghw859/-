# Day 8 前后端契约变更说明（前端冻结 · 后端适配版）

> **面向全体模块人员（前端 / Java / Python）**
> 日期：2026-09-11
> 模块：`lingmou_ai`（Python AI 服务，端口 8000）
> 原则：**前端代码冻结，禁止前端改任何数据结构；所有 AI 接口由后端适配前端现有形态。**

---

## 一、为什么改：发现的矛盾点

Day8 联调前以 `gonghang_vue3/src/views/OverviewView.vue`、`AIAssistantView.vue` 为唯一契约核对，发现 Python 后端实现与前端 mock 存在 4 处硬冲突：

| # | 接口 | 前端现有形态（冻结，不改） | 后端旧实现 | 矛盾性质 |
|---|---|---|---|---|
| 1 | `GET /api/ai/heatmap` | **6 个北京网点 × 8 时段**（9:00-16:00），单元格 10-100 负载百分比，行需要 `shortName`，格子需要 Tailwind 类名 `bg/textClass`，需要现成推荐语 `tip` | 4 个跨城市占位网点（北京/上海/深圳/杭州）× **24 全天时段**，输出客流**人数**（常 >100），状态为大写 `BUSY/MODERATE/IDLE`，无 tip/颜色字段 | 数据契约完全不匹配，前端直接赋值会白屏/无色 |
| 2 | `POST /api/ai/chat` | `getAIReply()` 本地知识库：**带【标题】的详细业务指引**（步骤/时长/推荐网点，50-130 字） | 10 意图 × **1-2 句短模板**；LLM key 当前 401 失效只能走规则版 | 切接口后演示质量明显倒退 |
| 3 | `GET /api/ai/diagnosis` | `aiDiagnosis` 是**单个字符串**，打字机逐字渲染到一个 `<p>` 里 | 返回结构化对象 `{diagnosis, highlights[], suggestions[], generatedAt, model}` | 前端不接收列表/对象，字段无法消费 |
| 4 | 全部接口 | 成功码期望 `code=0` | 成功码已是 0（此项无冲突，本次复核确认） | 无 |

**根因**：前端按演示稿先做 mock，后端按分工文档独立实现，双方此前未对过字段契约。本次由后端单方面适配解决，前端零改动。

---

## 二、怎么解决的（方案概要）

### 1. heatmap：以后端适配前端，同时保留线性回归算法

- 网点种子改为前端的 **6 个北京网点**，名称/顺序/状态（`busy/moderate/free`）逐字一致（`config/settings.py`）
- 线性回归（numpy 最小二乘 / sklearn 可选）**完整保留**：仍先预测 24 时段客流，再切营业 8 时段
- 输出尺度对齐：回归结果归一为小时形态比例，与前端 `basePatterns`（按 status）50/50 融合，再叠加**与前端完全相同的确定性扰动** `delta=((seed+k*7)%11)-5`（seed 同样用网点名字符编码累加），最后 clamp 到 10-100
- 颜色类名、`shortName` 截断、`tip` 推荐语全部在后端按前端 JS 逻辑逐字生成，**前端拿到即可直接渲染，无需任何转换**
- 顺带修复 Day3 遗留 bug：预测日期以前内部误用 `date.today()`，现在正确透传入参日期

### 2. chat：前端知识库 17 条话术原样移植

- `AIAssistantView.vue` 中 `getAIReply` 的 if-else 规则链，按**原顺序、原文案**移植为 `KB_RULES`（正则 `re.search`，大小写敏感，语义等价 JS `regex.test`）
- 规则版独立保证演示质量，**不依赖大模型可用**（DASHSCOPE key 401 期间自动走规则长文；WebSocket Day7 的情感识别、LLM 双路径均保留）
- 前端末尾分支中"开户/挂失/外汇"3 条在 JS 里本就不可达（主链提前命中），已在代码注释标明，未照搬死代码

### 3. diagnosis：对外只暴露 text

- 内部仍生成结构化结果（画像 → LLM/规则 → 正文+亮点+建议），新增 `compose_text()` 拼成**一段无换行纯文本**（前端单 `<p>` 不识别 `\n`），对外 `data` 只返回 `{"text": "..."}`

### 4. Redis 缓存键全部升 v2（避免旧结构脏数据）

| 旧键 | 新键 | 原因 |
|---|---|---|
| `heatmap:{date}:{branchId}`（24 元素组，按网点 4 键） | `heatmap:v2:{date}`（整图 1 键，新结构） | 新旧数组长度/结构不同，id 1-4 撞键会导致反序列化后组装失败；整图单键也更利于 Day9 的 <200ms 目标 |
| `diagnosis:{userId}`（旧结构化对象） | `diagnosis:v2:{userId}`（`{"text":...}`） | 旧缓存无 `text` 字段，命中会导致前端打字机空白 |

> 部署/拉取最新代码后无需手动清 Redis（键名已隔离）；如要彻底清理旧键可删 `heatmap:*`、`diagnosis:*`（不带 v2 的）。

---

## 三、修改文件清单（仅 lingmou_ai 目录，共 5 个文件）

| 文件 | 改动 |
|---|---|
| `config/settings.py` | `BRANCHES_SEED`：4 跨城市占位 → 6 个北京网点（新增 `status` 字段，名称/顺序对齐前端） |
| `services/heatmap_engine.py` | 重写输出层：保留线性回归三层算法，新增营业 8 时段负载百分比输出（10-100）；修复 date 透传 bug |
| `routers/heatmap.py` | 返回结构改为前端契约（branches/hours/rows/tip）；颜色类名、shortName、tip 后端生成；缓存升 v2 整图键 |
| `services/chat_engine.py` | 短模板 → 前端 17 条长文知识库（有序规则）；Day7 情感识别/LLM 双路径保留，降级也走长文 |
| `routers/diagnosis.py` + `services/diagnosis_engine.py` | 对外仅 `data.text`；画像网点同步为北京 6 网点；缓存升 v2 |

**未改动**：`parse-preform`（前端正则为主解析、Python 只兜底的红线不变）、`precheck`（Java 直通码流程在调，契约不动）、健康检查、WebSocket 路由、前端模板与 mock 数据结构、Java 全部文件。

### 前端（2 个文件，仅新增请求动作）

| 文件 | 改动 |
|---|---|
| `gonghang_vue3/src/views/OverviewView.vue` | 新增 `loadHeatmap()` 调真实接口 + 失败回退 `buildHeatmap()`；onMounted 切换调用；import 统一请求层 |
| `gonghang_vue3/src/views/AIAssistantView.vue` | 新增 `getSessionId()`/`fetchReply()`（POST chat + 失败回退本地话术）；打字回调改 async；思维链/打字机/快捷指令未动 |

---

## 四、最新接口契约（前端/Java 对接以此为准）

所有接口：响应统一 `{ "code": 0, "msg": "success", "data": ... }`；成功 **code 恒为 0**，错误码 6xxxx。

### 1. `GET /api/ai/heatmap?date=today`（date 也支持 yyyy-mm-dd）

```json
{
  "code": 0, "msg": "success",
  "data": {
    "date": "2026-09-11",
    "branches": [
      {"id": 1, "name": "北京分行营业部", "status": "busy"},
      {"id": 2, "name": "长安街智慧示范支行", "status": "moderate"},
      {"id": 3, "name": "金融街私人银行旗舰支行", "status": "free"},
      {"id": 4, "name": "中关村科技创新特色支行", "status": "moderate"},
      {"id": 5, "name": "望京SOHO社区支行", "status": "free"},
      {"id": 6, "name": "国贸CBD中心支行", "status": "busy"}
    ],
    "hours": ["9:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00"],
    "rows": [
      {
        "branch": "北京分行营业部",
        "shortName": "北京分行营业部",
        "cells": [
          {"val": 93, "bg": "bg-rose-400", "textClass": "text-white"}
        ]
      }
    ],
    "tip": "AI推荐：望京SOHO社区支行 12:00-13:00 全网客流最低（仅10%负载），建议此时段到店办理"
  }
}
```

- `val`：10-100 整数；颜色阈值与前端一致：`<35 bg-emerald-400`、`<60 bg-amber-400`、其余 `bg-rose-400`
- `shortName`：网点名超 8 字符截前 7 字 + `…`；tip 内超 10 字符截前 9 字 + `…`（与 JS 完全一致）
- 同一 date 结果确定性一致；非法日期返回 `code=60001`
- Redis 不可用时自动降级实时计算，不报错

### 2. `POST /api/ai/chat`

请求：`{"sessionId": "xxx", "message": "附近哪个网点人少？"}`
响应 `data`：`{"sessionId", "reply", "intent"}`
- `reply`：带【标题】的长文指引（50-130 字，与前端原本地话术同文案）
- `intent`：17 类标签（branch_recommend/forex/material/queue/appointment/open_account/withdraw/card_loss/password_reset/greeting/help/thanks/transfer/loan/large_cash/business_hours/location/unknown）
- 多轮上下文存 Redis（`chat:{sessionId}`，TTL 30 分钟，最多 20 轮）

### 3. `GET /api/ai/diagnosis?userId=1`

```json
{"code": 0, "msg": "success", "data": {"text": "经灵枢 AI 业务调度与风控模型扫描……（一段约 200 字纯文本，无换行）"}}
```

### 4. `GET /api/ai/health` → `code=0, data.status="ok"`（不变）

### 不变的接口（Java 仍在调用，勿改调用方式）

- `POST /api/ai/precheck`：材料预检，Java 直通码流程（`AiPrecheckServiceImpl`）依赖，契约不变
- `POST /api/ai/parse-preform`：预填单兜底解析，红线保持（不调大模型）
- `WS /api/ai/chat/ws`：Day7 数字人长连接，现已同步输出长文话术 + 情感识别

---

## 五、验证结果（真实 HTTP 实测）

启动 uvicorn 后用真实 HTTP 请求逐接口验证，**全部通过**：

- health：`code=0`
- heatmap：6 网点 × 8 时段；名称/顺序/小写 status 全一致；val 全部 10-100 整数；颜色三种类名正确；shortName 截断与 JS 一致；tip 格式正确；同日期确定性一致；非法日期 60001；显式日期可用
- chat：7 组典型提问意图与长文前缀全部正确，旧短模板已消失
- diagnosis：`data` 仅含 `text`，字符串、约 229 字、无换行；缓存命中一致；缺 userId 返回 422
- 回归：Day7 WebSocket（长文+消极安抚前缀+ping/pong）、precheck（60004 等）、parse-preform（五万元=50000）均通过

---

## 六、前端接线（已完成 ✅，模板与数据结构零改动）

接线于 2026-09-11 完成并通过浏览器端到端验证。**未改任何模板结构/mock 数据结构**，仅新增请求动作，原本地算法/知识库全部保留为降级兜底。

| 前端文件 | 改动 |
|---|---|
| `gonghang_vue3/src/views/OverviewView.vue` | 新增 `loadHeatmap()`：`onMounted` 改调 `GET /api/ai/heatmap?date=today`，`data.rows` 直接赋给 `heatmapData`、`data.tip` 赋给 `heatmapTip`（字段同名同形）；请求失败或结构异常时回退原 `buildHeatmap()` 本地算法 |
| `gonghang_vue3/src/views/AIAssistantView.vue` | 新增 `getSessionId()`（sessionId 存 localStorage 键 `lingmou_ai_session`，刷新续接 Redis 上下文）+ `fetchReply()`：思维链动画后调 `POST /api/ai/chat`，`data.reply` 喂原打字机定时器；失败回退原 `getAIReply()` 本地知识库。思维链/打字机动画、快捷指令、欢迎语全部未动 |

两处均复用队友已建的统一请求层 `src/utils/request.ts`（axios 拦截器自动注入 token、解包 `{code,msg,data}`），未裸写 fetch。

**端到端验证证据（真实浏览器）**：

- 真实链路（vite 5173 → proxy → Python 8000）：
  - 热力图 `GET /api/ai/heatmap` 200，DOM 实测 **6 行 × 8 列 = 48 格**，行名/首行数值（93,100,75,43,48,66,70,44）与后端逐字一致，tip 正常显示
  - 数字人 `POST /api/ai/chat` 200："附近哪个网点人少"→【AI网点推荐】127 字；"我想办卡"→【开户办卡指引】117 字；sessionId 自动生成（`web_<时间戳>_<随机>`）
  - 控制台无 /api/ai/ 相关错误
- 降级链路（故意停掉 Python 后端）：
  - 热力图自动回退本地算法，仍渲染 48 格 + tip，**无白屏、无 alert**
  - 数字人自动回退本地知识库，仍返回【AI网点推荐】长文，**无 alert、对话不中断**
- 前端工程检查：`vue-tsc` 类型检查零错误，`vite build` 成功

---

## 七、Day 9 联调通知（2026-09-12，Python 模块 → 全员）

### 1. 【待办·前端】AI 材料预检接线（T3，Day9 唯一剩余开发项）

**现状**：Java `POST /api/qrcode/generate` 已完整接通 Python `POST /api/ai/precheck`（QRCodeController → AiPrecheckServiceImpl，60004 有专门分支），但**前端从未调用过该接口**——目前 PreFormView 的「生成直通码」按钮只走本地流程，AI 预检链路端到端从未跑通。

**接线点（前端 1 处改动 + 1 个弹窗）**：

1. `gonghang_vue3/src/views/PreFormView.vue` **L1165**「材料已备齐，生成直通码」按钮：点击时改为（或先）调用 Java `POST /api/qrcode/generate`
   - 请求体：`{ businessType, materials, ... }`（沿用现有表单字段，businessType 用 6 个标准枚举或简写均可，见下）
   - `code=0 && passed=true` → 按现有流程跳转 `/qrcode`
   - `passed=false` → 弹窗展示 `data.missing`（缺失材料清单），不跳转
   - `code=60004`（未知业务）→ 提示业务类型错误
2. Python 侧已做**枚举别名兼容**（本次新增）：`TRANSFER`→`LARGE_TRANSFER`、`LOAN`→`LOAN_APPLICATION`，大小写不敏感；前端/Java 传哪种写法都不会再误报 60004

**Python/Java 两侧接口均已就绪，联调时只需前端接上即可跑通。**

### 2. 【已完成·无需动作】AI 诊断卡片接线验证

OverviewView 的诊断卡片接线已由前端侧完成（`loadDiagnosis()`，带未登录守卫与失败回退），Python 侧运行时验证通过：`GET /api/ai/diagnosis?userId=1` → `data.text` 单段 228 字、无换行。Day9 联调时直接走查即可。

### 3. 【验收证据】热力图压测 < 200ms **PASS**（Day9 分工目标）

三场景真实 HTTP 实测（6 网点 × 8 时段矩阵，Redis 缓存开启）：

| 场景 | 次数 | 平均 | 最大 |
|---|---|---|---|
| 缓存命中 | 50 | 19.5 ms | 32.1 ms |
| 缓存未命中（随机日期重新预测） | 30 | 18.2 ms | 30.0 ms |
| 并发 20 线程 | 20 | 23.3 ms | 34.2 ms（总墙钟 61.6 ms） |

全部远低于 200ms 目标，**验收通过**。注：若手动用浏览器工具测出 ~2000ms，是 `localhost` IPv6 解析回落所致，非服务问题（走 `127.0.0.1` / vite proxy 即正常）。

### 4. 【联调环境】启动注意

1. AI 服务按标准 `uvicorn main:app --port 8000` 启动（需 Redis 在 6379 运行）
2. precheck 传 `TRANSFER` / `LOAN` 亦可（已兼容），但建议统一用 6 个标准枚举

---

## 八、Day 9 联调通知（2026-09-12，Java-A → 全员）

Java-A 提交 `d5e7eb8`：预填单页/凭证详情/进度时间轴联调完成（真实浏览器 e2e 验证：进度页 16 断言、预填单页 18 断言、失败路径 17 断言全过）。以下事项需各角色知悉/决策。

### 1.【需 Java-B 决策】预约凭证 V ↔ 直通码 T 格式断裂（本次有意未修）

- `AppointmentServiceImpl.generateVoucherNum()` 产出 `V`+毫秒+4位随机（18 字符）；`VoucherServiceImpl.isValidVoucherNum()` 要求 21 字符、`T` 开头、CRC16 通过
- 后果：拿 `appointment.voucherNum` 调 `GET /api/vouchers/{num}` 一律 `validFormat:false`；`GET /api/qrcode/{num}/image` 抛 `20007`
- `vouchers` 表行只由 `POST /api/qrcode/generate` 创建、`appointment_id` 恒 NULL，**两套凭证无任何数据通路**。直通码页在 Java-B 范围，由 Java-B 定夺是否统一。

### 2.【T3 归属确认】`POST /api/qrcode/generate` 前端仍无人调用

- 现状：PreFormView「材料已备齐，生成直通码」按钮目前只做 `POST /api/preforms`（保存预填单）+ 跳 `/qrcode`；`QRCodeView.vue` 也未调 generate
- Java-A 侧有意不接（分工红线：预填单只保存+列表）。建议 **Java-B 在 QRCodeView 内接通 generate + AI 预检弹窗**，或明确由 Java-A 在按钮上加一次预检调用，二选一即可跑通 T3 全链路

### 3.【共享 store 变更】`stores/appointment.ts`（QRCodeView 是消费方）

本次改动：`ApptStatus` 新增 `'canceled'`；`Appointment` 新增 `queueNumber`；新增 `refreshOne(id)`；`cancel()` 缺后端 ID 时改为 **throw**（不再静默 splice）。均为增量，但 QRCodeView 若 catch 不到会直接冒泡，请知悉。

### 4.【已知限制·非 bug，待拍板】

- **终态预约永远删不掉**：已取消/已过期/已完成后端拒绝删除（20006/20004），但进度页仍给复选框 → 批量删除必然部分失败，toast 如实提示「删除失败 N 条」且卡片不动（已实测）。建议二选一：终态卡片隐藏复选框，或后端放开终态软删除
- **后端不可达时 toast 显示 axios 英文原文** `Request failed with status code 502`（拦截器 `error.response.data.msg` 取不到时回落 `error.message`），中文界面突兀，待统一文案
- `NavigationView.vue:1075-1079` 状态徽章 `v-else` 写死「待激活」，已取消预约在此误显为「待激活」（既有问题、非本次回归），归属待确认

### 5.【需前端负责人一行修】OverviewView 「AI 预填单自动解析」是假数据

`OverviewView.vue:41` 值硬编码 `'9'`；后端 `GET /api/overview` 已返回真实 `totalPreForms`（当前 2），但全前端 0 处消费。一行 `value: String(...)` 即可变真。

### 6.【需 Python 知悉】签名与预填单落库现状

- `pre_forms.signature_url` 是 `VARCHAR(255)`，装不下 canvas dataURL（实测 400 字符即触发 MySQL 1406 → 兜成 90000）。签名目前只记在 `parsedJson.signatureKeys`（键名清单），**未持久化图像**。要真落签名需新增上传接口 + 改列 TEXT；`schema.sql` 是 DROP TABLE 重建脚本不能重跑
- `pre_forms.raw_text` 可能出现 `[手动录入] {业务名}` 前缀（手动路径无粘贴文本时的兜底），若 AI 侧解析 rawText 需兼容该标记

### 7.【联调环境】测试数据现状（演示前知悉）

- 账号 `13800138000`：信用分 **70**（联调取消/删除扣分痕迹，4 条 credit_records）；`appointments` 5 条（1 虚拟号 / 3 已取消 / 1 已失效，正好演示进度页多状态）；`pre_forms` 2 条
- 进度页演示可直接用；要恢复 90 分或清数据找 Java-A
