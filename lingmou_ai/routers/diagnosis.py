"""AI 财富健康度诊断接口（Day 6 初版 / Day 8 前端契约对齐版）。

GET /api/ai/diagnosis?userId=...
Day 8：前端 aiDiagnosis 是单个字符串（打字机逐字渲染），不接收结构化列表，
故对外 data 只暴露 {"text": "..."}；内部仍由画像 → LLM/规则 → 结构化结果拼接。
- 有 LLM API Key：走真模型（通义千问）
- 无 Key / Key 失效：规则兜底（当前 DASHSCOPE key 401，自动走此路径）
- Redis 缓存 diagnosis:v2:{userId}，TTL 600s（v2 与旧结构化缓存键隔离）
"""
import json

from fastapi import APIRouter, Query

from config.settings import settings
from services.diagnosis_engine import generate_diagnosis, compose_text
from services.redis_client import redis_client

router = APIRouter(prefix="/api/ai", tags=["diagnosis"])

_DIAGNOSIS_CACHE_TTL = 600  # 10 分钟


@router.get("/diagnosis")
async def diagnosis(user_id: int = Query(..., alias="userId", ge=1, description="用户 ID")):
    """AI 财富健康度诊断（对外仅返回单段文本 data.text）"""
    cache_key = f"diagnosis:v2:{user_id}"

    # 1. 尝试 Redis 缓存
    try:
        client = await redis_client.get_client()
        cached = await client.get(cache_key)
        if cached:
            return {
                "code": 0,
                "msg": "success (cached)",
                "data": json.loads(cached),
            }
    except Exception:
        # Redis 不可用降级为每次生成
        pass

    # 2. 生成诊断并拼接为单段文本
    try:
        result = await generate_diagnosis(user_id)
        text = compose_text(result)
    except Exception as e:
        return {
            "code": settings.err_diagnosis,
            "msg": f"AI 诊断生成失败：{e}",
            "data": None,
        }

    payload = {"text": text}

    # 3. 写入缓存
    try:
        client = await redis_client.get_client()
        await client.set(
            cache_key,
            json.dumps(payload, ensure_ascii=False),
            ex=_DIAGNOSIS_CACHE_TTL,
        )
    except Exception:
        pass

    return {
        "code": 0,
        "msg": "success",
        "data": payload,
    }
