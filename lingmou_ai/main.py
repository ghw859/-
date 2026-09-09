from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# 启动时加载 .env 文件（必须在 import services 之前）
load_dotenv()

from routers.health import router as health_router
from routers.chat import router as chat_router
from routers.heatmap import router as heatmap_router
from routers.precheck import router as precheck_router
from routers.parse_preform import router as parse_preform_router
from routers.diagnosis import router as diagnosis_router
from services.llm_client import llm_client
from services.redis_client import redis_client

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
app.include_router(chat_router)
app.include_router(heatmap_router)
app.include_router(precheck_router)
app.include_router(parse_preform_router)
app.include_router(diagnosis_router)


@app.on_event("startup")
async def on_startup():
    """启动时打印 LLM + Redis 配置状态"""
    if llm_client.is_configured:
        print(f"[LLM] 已配置密钥，使用模型: {llm_client.model}")
    else:
        print("[LLM] ⚠ 未配置 DASHSCOPE_API_KEY，AI 对话功能不可用")
    ok = await redis_client.ping()
    if ok:
        print("[Redis] 连接正常")
    else:
        print("[Redis] ⚠ 连接失败，对话上下文功能不可用")
