from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# 启动时加载 .env 文件（必须在 import services 之前）
load_dotenv()

from routers.health import router as health_router
from services.llm_client import llm_client

app = FastAPI(
    title="ICBC · 灵枢 AI 服务",
    description="工行杯项目 AI 能力服务",
    version="0.0.1",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(health_router)


@app.on_event("startup")
def on_startup():
    """启动时打印 LLM 配置状态，方便排查"""
    if llm_client.is_configured:
        print(f"[LLM] 已配置密钥，使用模型: {llm_client.model}")
    else:
        print("[LLM] ⚠ 未配置 DASHSCOPE_API_KEY，AI 对话功能不可用")

