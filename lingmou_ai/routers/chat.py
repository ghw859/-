"""AI 数字人对话接口（Day 2 规则版）。

- POST /api/ai/chat：单轮对话，上下文存 Redis
- Day 7 升级为 WebSocket /api/ai/chat/ws（多轮 + 情感识别）
"""
from fastapi import APIRouter
from pydantic import BaseModel, Field

from config.settings import settings
from services.redis_client import redis_client
from services.chat_engine import chat as rule_chat

router = APIRouter(prefix="/api/ai", tags=["chat"])


class ChatRequest(BaseModel):
    sessionId: str = Field(..., description="会话 ID，用于追踪多轮上下文")
    message: str = Field(..., min_length=1, max_length=500, description="用户输入")


@router.post("/chat")
async def chat_endpoint(req: ChatRequest):
    """规则版 AI 数字人对话

    Redis 降级策略（与 heatmap/diagnosis/WS 保持一致）：
    - 读历史失败：history 置空继续（多轮退单轮，不报错）
    - 写回失败：回复照常返回（不丢弃已生成的 reply）
    """
    # 1. 读取历史上下文（Redis 不可用 → 空历史降级）
    try:
        history = await redis_client.get_chat_history(req.sessionId)
    except Exception:
        history = []

    # 2. 规则匹配 + 话术生成
    try:
        reply, intent = rule_chat(req.message, history)
    except Exception as e:
        return {
            "code": settings.err_internal,
            "msg": f"AI 服务内部错误：{e}",
            "data": None,
        }

    # 3. 写回上下文（Redis 不可用 → 静默忽略，回复不丢）
    try:
        await redis_client.append_chat_history(req.sessionId, "user", req.message)
        await redis_client.append_chat_history(req.sessionId, "assistant", reply)
    except Exception:
        pass

    return {
        "code": 0,
        "msg": "success",
        "data": {
            "sessionId": req.sessionId,
            "reply": reply,
            "intent": intent,
        },
    }
