# Day 10 上午 E2E 走查报告（2026-09-13，Java-A）

> 全链路真实 HTTP 走查（非 mock），发现问题 2 个（均已修）、误报 1 个（已澄清）。
> 走查账号：`13800138000` / `123456`。终端为 Git Bash 时发中文请求体必须写 UTF-8 文件 + `--data-binary`，直接 `-d '中文'` 会以 GBK 编码导致后端 90000（见"误报"节）。

## 一、走查结果：主链路 16 步全通 ✅

| # | 链路 | 结果 |
|---|------|------|
| 1 | 登录 → token | ✅ |
| 2 | `GET /api/overview` | ✅ totalPreForms=2（与库一致，前端卡片接线后可显示真值） |
| 3 | 创建预约 | ✅ id=40，VIRTUAL，排队号 1001 |
| 4 | `GET /api/vouchers/{V号}` | ✅ validFormat=true，**kind=APPOINTMENT**（Java-B 双格式互认+V凭证关联行 best-effort 落库均生效） |
| 5 | `GET /api/qrcode/{V号}/image` | ✅ 200，公开放行 |
| 6 | 推进 4 步 | ✅ VIRTUAL→ACTIVE→CALLED→PROCESSING→COMPLETED |
| 7 | 终态守卫 | ✅ 已完成的再推进 → 20005 拒绝 |
| 8 | 创建预填单 | ✅ id=3 |
| 9 | T3：`POST /api/qrcode/generate` 缺材料 | ✅ HTTP 200 / code=0 / passed=false，missing=['date']，missingLabels 中文标签（契约正确） |
| 10 | T3：材料齐全 | ✅ passed=true，T 号 `TB1002026091300011FD6`（21 位格式） |
| 11 | T 凭证查询 | ✅ validFormat=true，kind=DIRECT_CODE |
| 12 | T 码图片 / 审计链 / 链校验 | ✅ 图片 200；audit/my 1 条 hashValid=true；verify intact=true |
| 13 | AI 热力图 | ✅ 6 网点 × 8 时段 |
| 14 | AI 数字人 | ✅ intent=branch_recommend，190 字长文 |
| 15 | AI 诊断 | ✅ text 336 字 |
| 16 | AI 宕机降级 | ✅ 后端返回 60001「AI预检服务调用失败: Connection refused」，网点等其余接口正常 |

## 二、修复的问题（2 个）

### 1. `preform_parser._extract_amount` 缺"取现"类关键词（Day8 遗留，非 Day9 回归）
`cash_reserve`（大额取现）是前端 5 枚举之一，但中文数字金额的关键词表只有 存/转账/汇款/投资/贷款/借款，**"我要取现五万元"解析不出金额**。
修复：`services/preform_parser.py` 关键词表追加 `取现/取钱/提现/支取`。

### 2. 中文金额后紧跟其他词时整串校验失败（Day8 遗留）
正则 `_AMOUNT_CN_RE` 贪婪匹配最长 7 个汉字，"转账**五万元到张三**"的 group 含尾部人名，`cn2num` 整串校验返回 None → 金额为 None。
修复：剥掉货币后缀后，截断到首个非中文数字字符为止。
单测 8 例全过：转账/取现/存款/金额/借款/取现三千五/一亿/无金额，均符合期望。

## 三、误报澄清（1 个）

`POST /api/appointments` 一度返回 90000，实为 **curl 测试 harness 把中文请求体按 GBK 发送**（Invalid UTF-8 start byte 0xbf），非应用 bug。前端 axios 发 UTF-8 无此问题。用 UTF-8 文件 + `--data-binary` 后通过。

## 四、Day 10 上午任务完成情况

| 任务 | 状态 |
|------|------|
| 9 演示数据重置 | ✅ 信用分 90（库内 0 条记录，清掉联调扣分的 4 条 -5）；演示预约 1 条 ACTIVE（id=41，branch 2，9-14 10:00）供进度页演示；历史 1 COMPLETED + 4 CANCELED + 1 EXPIRED 保留展示多状态 |
| 1 主链路走查 | ✅ 16 步全过（本报告） |
| 7 前端构建 | ✅ `npm run build`（type-check + vite）exit 0 |
| 8 后端打包 | ✅ `mvn package` → `target/lingmou-1.0.0.jar` |
| 3 降级链路（AI 宕机） | ✅ T3 报 60001 中文提示可重试；其余接口正常 |

## 五、遗留（下午）

- LLM key 401 待用户从百炼控制台换专属 Base URL + 新 key（当前数字人/诊断走规则降级，演示可接受但会被问）
- request.ts toast 英文、NavigationView 徽章 → 前端负责人 gao
- 压测 `POST /api/qrcode/generate`（ZXing + 审计写库，最重的新链路）
