from fastapi import APIRouter

router = APIRouter(prefix="/api/ai", tags=["health"])


@router.get("/health")
def health_check():
    """AI 服务健康检查"""
    return {
        "code": 200,
        "msg": "AI 服务运行正常",
        "data": {
            "status": "ok",
            "service": "lingmou_ai",
            "version": "0.0.1",
        },
    }
