"""LLM 客户端封装，统一调用通义千问大模型。

密钥通过环境变量 DASHSCOPE_API_KEY 读取，不硬编码。
默认模型 qwen-turbo（性价比高），可通过 QWEN_MODEL 环境变量切换。
"""

import os
from typing import List, Dict, Optional

import httpx

DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY", "")
QWEN_MODEL = os.getenv("QWEN_MODEL", "qwen-turbo")
DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"


class LLMClient:
    """通义千问 LLM 客户端"""

    def __init__(self, api_key: str = None, model: str = None):
        self.api_key = api_key or DASHSCOPE_API_KEY
        self.model = model or QWEN_MODEL
        self.base_url = DASHSCOPE_BASE_URL

    @property
    def is_configured(self) -> bool:
        """是否已配置 API 密钥"""
        return bool(self.api_key)

    async def chat(
        self,
        messages: List[Dict[str, str]],
        temperature: float = 0.7,
        max_tokens: int = 1024,
    ) -> str:
        """多轮对话

        Args:
            messages: 消息列表，格式 [{"role": "user", "content": "..."}]
            temperature: 采样温度
            max_tokens: 最大输出 token 数

        Returns:
            模型回复内容
        """
        if not self.is_configured:
            return "AI 服务未配置密钥，请设置环境变量 DASHSCOPE_API_KEY"

        url = f"{self.base_url}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": self.model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }

        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            data = response.json()
            return data["choices"][0]["message"]["content"]

    async def chat_once(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: int = 1024,
    ) -> str:
        """单轮对话（便捷方法）

        Args:
            prompt: 用户输入
            system_prompt: 系统提示词（可选）
            temperature: 采样温度
            max_tokens: 最大输出 token 数

        Returns:
            模型回复内容
        """
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})
        return await self.chat(messages, temperature, max_tokens)


# 全局单例，供各 router 复用
llm_client = LLMClient()
