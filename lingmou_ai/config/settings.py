"""AI 服务配置，统一从环境变量读取。"""
import os
from dataclasses import dataclass


@dataclass
class Settings:
    # Redis
    redis_url: str = os.getenv("REDIS_URL", "redis://localhost:6379/0")
    chat_history_ttl: int = int(os.getenv("CHAT_HISTORY_TTL", "1800"))  # 30 分钟
    chat_history_max: int = int(os.getenv("CHAT_HISTORY_MAX", "20"))   # 最多 20 轮（40 条）

    # LLM（Day 1 已有，保留）
    dashscope_api_key: str = os.getenv("DASHSCOPE_API_KEY", "")
    qwen_model: str = os.getenv("QWEN_MODEL", "qwen-turbo")

    # AI 错误码段位 6xxxx
    err_param = 60001
    err_redis = 60002
    err_internal = 60099


settings = Settings()
