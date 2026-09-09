"""AI 数字人 WebSocket 对话接口（Day 7 升级版）。

WebSocket /api/ai/chat/ws?sessionId=xxx
多轮上下文 + 简单情感识别 + LLM 双路径升级。

消息协议（JSON）：
  client → server: {"type": "message", "message": "..."}
  client → server: {"type": "ping"}
  server → client: {"type": "reply", "reply": "...", "intent": "...", "emotion": "...", "model": "..."}
  server → client: {"type": "error", "code": 60xxx, "msg": "..."}
  server → client: {"type": "pong"}
  server → client: {"type": "system", "msg": "..."}

上下文存储：Redis（跨连接持久化）+ 活跃连接内内存副本（降延迟）。
"""
import asyncio
import json
from typing import Dict, List, Set

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from config.settings import settings
from services.chat_engine import chat_with_emotion
from services.redis_client import redis_client

router = APIRouter(tags=["chat-ws"])


# ============ 活跃连接管理 ============

class ConnectionManager:
    """管理 WebSocket 活跃连接 + 内存上下文"""

    def __init__(self):
        self._active: Dict[str, WebSocket] = {}   # sessionId → WebSocket
        self._mem_history: Dict[str, List[Dict[str, str]]] = {}  # sessionId → 内存历史

    async def connect(self, ws: WebSocket, session_id: str) -> None:
        await ws.accept()
        self._active[session_id] = ws
        # 从 Redis 加载历史到内存（Redis 挂了也能降级）
        try:
            self._mem_history[session_id] = await redis_client.get_chat_history(session_id)
        except Exception:
            self._mem_history[session_id] = []

    def disconnect(self, session_id: str) -> None:
        self._active.pop(session_id, None)
        self._mem_history.pop(session_id, None)

    def get_history(self, session_id: str) -> List[Dict[str, str]]:
        return self._mem_history.get(session_id, [])

    async def save_turn(
        self, session_id: str, role: str, content: str
    ) -> None:
        """追加一轮对话到内存 + Redis"""
        hist = self._mem_history.setdefault(session_id, [])
        hist.append({"role": role, "content": content})
        # 裁剪（与 Redis 配置一致）
        max_items = settings.chat_history_max * 2
        if len(hist) > max_items:
            self._mem_history[session_id] = hist[-max_items:]
        # Redis 备份
        try:
            await redis_client.append_chat_history(session_id, role, content)
        except Exception:
            pass  # Redis 不可用时仅内存存储


manager = ConnectionManager()


# ============ WebSocket Handler ============

@router.websocket("/api/ai/chat/ws")
async def chat_ws(ws: WebSocket, sessionId: str = ""):
    """AI 数字人 WebSocket 对话入口"""
    # 生成 sessionId：前端没传就用时间戳
    sid = sessionId or f"ws_{id(ws)}_{int(asyncio.get_event_loop().time() * 1000)}"

    await manager.connect(ws, sid)
    # 连接成功通知
    await ws.send_json({
        "type": "system",
        "msg": f"灵枢 AI 数字人已连接（session={sid}）。"
               f"有问题可随时问我：预约、网点、材料、进度等。",
    })

    try:
        while True:
            raw = await ws.receive_text()
            try:
                data = json.loads(raw)
            except json.JSONDecodeError:
                await ws.send_json({
                    "type": "error",
                    "code": settings.err_param,
                    "msg": "消息格式错误，请发送 JSON",
                })
                continue

            msg_type = data.get("type", "message")

            if msg_type == "ping":
                await ws.send_json({"type": "pong"})
                continue

            if msg_type == "message":
                user_msg = (data.get("message") or "").strip()
                if not user_msg:
                    await ws.send_json({
                        "type": "error",
                        "code": settings.err_param,
                        "msg": "message 不能为空",
                    })
                    continue

                # 记录用户输入
                await manager.save_turn(sid, "user", user_msg)

                # 生成回复
                try:
                    result = await chat_with_emotion(
                        user_msg, manager.get_history(sid)
                    )
                except Exception as e:
                    await ws.send_json({
                        "type": "error",
                        "code": settings.err_internal,
                        "msg": f"AI 服务内部错误：{e}",
                    })
                    continue

                # 记录 AI 回复
                await manager.save_turn(sid, "assistant", result["reply"])

                # 推送给客户端
                await ws.send_json({
                    "type": "reply",
                    "reply": result["reply"],
                    "intent": result["intent"],
                    "emotion": result["emotion"],
                    "model": result["model"],
                })

            else:
                await ws.send_json({
                    "type": "error",
                    "code": settings.err_param,
                    "msg": f"未知消息类型: {msg_type}",
                })

    except WebSocketDisconnect:
        manager.disconnect(sid)
    except Exception as e:
        manager.disconnect(sid)
        # 异常时尝试通知客户端（连接可能已断）
        try:
            await ws.send_json({
                "type": "error",
                "code": settings.err_internal,
                "msg": f"连接异常: {e}",
            })
        except Exception:
            pass
