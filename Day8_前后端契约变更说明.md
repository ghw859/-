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

## 六、环境注意：本机 8000 端口被 Windows 系统预留

开发机上 `uvicorn --port 8000` 报 `[Errno 10013]`：8000 被系统 **http.sys / Hyper-V 动态端口排除**占用（`netsh interface ipv4 show excludedportrange protocol=tcp` 可见 8000-8000，监听者为 PID 4 System），普通权限无法释放（重启 WinNAT 需管理员）。

- 代码与端口无关，本次功能验证在 **8010** 端口完成，行为与 8000 完全一致
- 其他机器 / Day10 的 Docker Compose 环境内无此限制，仍按标准 `uvicorn main:app --port 8000` 启动
- 本机临时联调可用 `--port 8010`，并把 `vite.config.ts` 的 proxy target 临时改为 8010（或用管理员权限执行 `net stop winnat && net start winnat` 后重试 8000）

---

## 七、前端接线（已完成 ✅，模板与数据结构零改动）

接线于 2026-09-11 完成并通过浏览器端到端验证。**未改任何模板结构/mock 数据结构**，仅新增请求动作，原本地算法/知识库全部保留为降级兜底。

| 前端文件 | 改动 |
|---|---|
| `gonghang_vue3/src/views/OverviewView.vue` | 新增 `loadHeatmap()`：`onMounted` 改调 `GET /api/ai/heatmap?date=today`，`data.rows` 直接赋给 `heatmapData`、`data.tip` 赋给 `heatmapTip`（字段同名同形）；请求失败或结构异常时回退原 `buildHeatmap()` 本地算法 |
| `gonghang_vue3/src/views/AIAssistantView.vue` | 新增 `getSessionId()`（sessionId 存 localStorage 键 `lingmou_ai_session`，刷新续接 Redis 上下文）+ `fetchReply()`：思维链动画后调 `POST /api/ai/chat`，`data.reply` 喂原打字机定时器；失败回退原 `getAIReply()` 本地知识库。思维链/打字机动画、快捷指令、欢迎语全部未动 |

两处均复用队友已建的统一请求层 `src/utils/request.ts`（axios 拦截器自动注入 token、解包 `{code,msg,data}`），未裸写 fetch。

**端到端验证证据（真实浏览器）**：

- 真实链路（vite 5173 → proxy → Python 8010）：
  - 热力图 `GET /api/ai/heatmap` 200，DOM 实测 **6 行 × 8 列 = 48 格**，行名/首行数值（93,100,75,43,48,66,70,44）与后端逐字一致，tip 正常显示
  - 数字人 `POST /api/ai/chat` 200："附近哪个网点人少"→【AI网点推荐】127 字；"我想办卡"→【开户办卡指引】117 字；sessionId 自动生成（`web_<时间戳>_<随机>`）
  - 控制台无 /api/ai/ 相关错误
- 降级链路（故意停掉 Python 后端）：
  - 热力图自动回退本地算法，仍渲染 48 格 + tip，**无白屏、无 alert**
  - 数字人自动回退本地知识库，仍返回【AI网点推荐】长文，**无 alert、对话不中断**
- 前端工程检查：`vue-tsc` 类型检查零错误，`vite build` 成功

**剩余 Day9 接线（本次未做）**：诊断卡片取 `GET /api/ai/diagnosis` 的 `data.text` 赋给打字机源文本即可（后端已就绪）。

> 本机联调备注：因 8000 被系统预留，验证时 Python 跑在 8010，用临时 vite 配置 `npx vite --config vite.tmp.config.ts` 覆盖 proxy target；该临时文件已删除，仓库中的 `vite.config.ts` 仍指向标准 8000，Docker/其他机器不受影响。
