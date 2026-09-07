"""异步 Redis 客户端封装。

用于存储 AI 对话上下文（按 sessionId 隔离）。
"""
import json
from typing import List, Dict, Optional

import redis.asyncio as redis

from config.settings import settings


class RedisClient:
    """异步 Redis 客户端"""

    def __init__(self):
        self._client: Optional[redis.Redis] = None

    async def get_client(self) -> redis.Redis:
        """懒加载连接"""
        if self._client is None:
            self._client = redis.from_url(
                settings.redis_url,
                decode_responses=True,
            )
        return self._client

    async def get_chat_history(self, session_id: str) -> List[Dict[str, str]]:
        """读取对话历史，无记录返回空列表"""
        client = await self.get_client()
        raw = await client.get(f"chat:{session_id}")
        if not raw:
            return []
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return []

    async def append_chat_history(
        self,
        session_id: str,
        role: str,
        content: str,
    ) -> None:
        """追加一轮对话并刷新 TTL；超出上限丢最早一条"""
        client = await self.get_client()
        history = await self.get_chat_history(session_id)
        history.append({"role": role, "content": content})
        max_items = settings.chat_history_max * 2
        if len(history) > max_items:
            history = history[-max_items:]
        await client.set(
            f"chat:{session_id}",
            json.dumps(history, ensure_ascii=False),
            ex=settings.chat_history_ttl,
        )

    async def ping(self) -> bool:
        """探活，失败返回 False 不抛异常"""
        try:
            client = await self.get_client()
            return await client.ping()
        except Exception:
            return False


redis_client = RedisClient()
