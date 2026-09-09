"""AI 财富健康度诊断接口（Day 6）。

GET /api/ai/diagnosis?userId=...
调大模型生成个性化诊断建议卡片。
- 有 LLM API Key：走真模型（通义千问）
- 无 LLM API Key：规则兜底（从合成画像直接拼文案）
- Redis 缓存 diagnosis:{userId}，TTL 600s，避免重复 LLM 调用
"""
import json

from fastapi import APIRouter, Query

from config.settings import settings
from services.diagnosis_engine import generate_diagnosis
from services.redis_client import redis_client

router = APIRouter(prefix="/api/ai", tags=["diagnosis"])

_DIAGNOSIS_CACHE_TTL = 600  # 10 分钟


@router.get("/diagnosis")
async def diagnosis(user_id: int = Query(..., alias="userId", ge=1, description="用户 ID")):
    """AI 财富健康度诊断"""
    cache_key = f"diagnosis:{user_id}"

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

    # 2. 生成诊断
    try:
        result = await generate_diagnosis(user_id)
    except Exception as e:
        return {
            "code": settings.err_diagnosis,
            "msg": f"AI 诊断生成失败：{e}",
            "data": None,
        }

    # 3. 写入缓存
    try:
        client = await redis_client.get_client()
        await client.set(
            cache_key,
            json.dumps(result, ensure_ascii=False),
            ex=_DIAGNOSIS_CACHE_TTL,
        )
    except Exception:
        pass

    return {
        "code": 0,
        "msg": "success",
        "data": result,
    }
