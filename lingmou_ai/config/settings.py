"""AI 服务配置，统一从环境变量读取。"""
import os
from dataclasses import dataclass, field


# 4 个网点种子数据（Day 3 硬编码占位，Day 8 联调时改由 Java /api/branches 拉取）
BRANCHES_SEED = [
    {"id": 1, "name": "工行北京分行营业部", "busy_factor": 1.2},
    {"id": 2, "name": "工行上海陆家嘴支行", "busy_factor": 1.0},
    {"id": 3, "name": "工行深圳福田支行",   "busy_factor": 0.9},
    {"id": 4, "name": "工行杭州西湖支行",   "busy_factor": 0.7},
]

# 24 时段基线客流（银行典型日形态）
# 索引 0-23 对应 00:00 - 23:00
HOURLY_BASELINE = [
    0, 0, 0, 0, 0, 2,           # 00-05 夜间
    5, 15,                         # 06-07 开门前聚集
    80, 110, 120, 90,              # 08-11 上午高峰
    45, 55,                        # 12-13 午休
    95, 125, 130, 100,             # 14-17 下午高峰
    50, 30,                        # 18-19 临近下班
    5, 2, 1, 0                     # 20-23 非营业
]

# 6 类业务所需材料清单（Day 4）
# key = businessType，value = 必填字段名列表
BUSINESS_MATERIALS = {
    "OPEN_ACCOUNT":     ["idCard", "phone", "address"],
    "CARD_LOSS":         ["idCard", "cardNumber"],
    "LARGE_TRANSFER":    ["idCard", "payeeName", "payeeAccount", "amount"],
    "DEPOSIT":           ["amount", "depositMethod"],
    "LOAN_APPLICATION":  ["idCard", "incomeProof", "employer", "amount", "loanTerm"],
    "WEALTH_MGMT":       ["idCard", "riskAssessment", "investAmount"],
}

# 业务类型关键词映射（Day 5 预填单兜底解析）
# 用于从自然语言文本推断 businessType
BUSINESS_KEYWORDS = {
    "OPEN_ACCOUNT":     ["开户", "办卡", "新卡", "开卡"],
    "CARD_LOSS":         ["挂失", "补卡", "丢卡", "卡丢"],
    "LARGE_TRANSFER":    ["转账", "汇款", "转钱", "打款"],
    "DEPOSIT":           ["存款", "存钱", "存入", "存"],
    "LOAN_APPLICATION":  ["贷款", "借款", "贷一下"],
    "WEALTH_MGMT":       ["理财", "投资", "买基金", "买产品"],
}

# 各业务期望解析出的字段（Day 5 置信度计算用）
BUSINESS_EXPECTED_FIELDS = {
    "OPEN_ACCOUNT":     ["name", "idCard", "phone", "address"],
    "CARD_LOSS":         ["name", "idCard", "cardNumber"],
    "LARGE_TRANSFER":    ["name", "idCard", "payeeName", "payeeAccount", "amount"],
    "DEPOSIT":           ["amount"],
    "LOAN_APPLICATION":  ["name", "idCard", "amount", "loanTerm"],
    "WEALTH_MGMT":       ["name", "idCard", "investAmount"],
}


@dataclass
class Settings:
    # Redis
    redis_url: str = os.getenv("REDIS_URL", "redis://localhost:6379/0")
    chat_history_ttl: int = int(os.getenv("CHAT_HISTORY_TTL", "1800"))  # 30 分钟
    chat_history_max: int = int(os.getenv("CHAT_HISTORY_MAX", "20"))   # 最多 20 轮（40 条）

    # LLM（Day 1 已有，保留；Day 5 预填单解析严禁使用）
    dashscope_api_key: str = os.getenv("DASHSCOPE_API_KEY", "")
    qwen_model: str = os.getenv("QWEN_MODEL", "qwen-turbo")

    # 热力图
    branches_seed: list = field(default_factory=lambda: BRANCHES_SEED)
    hourly_baseline: list = field(default_factory=lambda: HOURLY_BASELINE)
    heatmap_history_days: int = int(os.getenv("HEATMAP_HISTORY_DAYS", "14"))
    heatmap_cache_ttl: int = int(os.getenv("HEATMAP_CACHE_TTL", "86400"))  # 1 天
    heatmap_seed_base: int = int(os.getenv("HEATMAP_SEED_BASE", "42"))

    # 材料预检
    business_materials: dict = field(default_factory=lambda: BUSINESS_MATERIALS)

    # 预填单兜底解析
    business_keywords: dict = field(default_factory=lambda: BUSINESS_KEYWORDS)
    business_expected_fields: dict = field(default_factory=lambda: BUSINESS_EXPECTED_FIELDS)
    parse_confidence_threshold: float = float(os.getenv("PARSE_CONFIDENCE_THRESHOLD", "0.3"))

    # AI 错误码段位 6xxxx
    err_param = 60001
    err_redis = 60002
    err_predict = 60003
    err_unknown_business = 60004
    err_parse_failed = 60005
    err_diagnosis = 60006
    err_internal = 60099

    # Java 后端地址（Day 6 起可选调用；无鉴权时不调用）
    java_backend_url: str = os.getenv("JAVA_BACKEND_URL", "http://localhost:8080")


settings = Settings()
