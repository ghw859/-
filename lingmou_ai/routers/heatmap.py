"""AI 客流预测热力图接口（Day 3）。

GET /api/ai/heatmap?date=today
返回 4 网点 × 24 时段的预测客流。
硬约束：不调大模型，确定性算法 + scikit-learn 时序预测。
"""
import json
from datetime import date, datetime
from typing import Optional

from fastapi import APIRouter, Query

from config.settings import settings
from services.redis_client import redis_client
from services.heatmap_engine import predict_branch, compute_busy_level, time_slots

router = APIRouter(prefix="/api/ai", tags=["heatmap"])


def _parse_date(date_param: str) -> Optional[date]:
    """解析 date 参数，支持 'today' 和 'yyyy-mm-dd'"""
    if date_param == "today":
        return date.today()
    try:
        return datetime.strptime(date_param, "%Y-%m-%d").date()
    except ValueError:
        return None


async def _get_branch_data(branch_id: int, target_date: date):
    """从 Redis 取缓存，未命中则预测"""
    cache_key = f"heatmap:{target_date.isoformat()}:{branch_id}"
    try:
        client = await redis_client.get_client()
        cached = await client.get(cache_key)
        if cached:
            return json.loads(cached), True
    except Exception:
        # Redis 不可用降级为每次预测
        pass

    values = predict_branch(branch_id)

    try:
        client = await redis_client.get_client()
        await client.set(
            cache_key,
            json.dumps(values),
            ex=settings.heatmap_cache_ttl,
        )
    except Exception:
        pass
    return values, False


@router.get("/heatmap")
async def heatmap(date_param: str = Query(..., alias="date", description="today 或 yyyy-mm-dd")):
    """返回 4 网点 × 24 时段客流热力图"""
    target = _parse_date(date_param)
    if target is None:
        return {
            "code": settings.err_param,
            "msg": f"date 参数不合法：{date_param}（应为 'today' 或 'yyyy-mm-dd'）",
            "data": None,
        }

    try:
        heatmap_data = []
        busy_levels = []
        for branch in settings.branches_seed:
            values, _ = await _get_branch_data(branch["id"], target)
            heatmap_data.append(values)
            busy_levels.append(compute_busy_level(values))

        return {
            "code": 0,
            "msg": "success",
            "data": {
                "date": target.isoformat(),
                "branches": [
                    {"id": b["id"], "name": b["name"]}
                    for b in settings.branches_seed
                ],
                "timeSlots": time_slots(),
                "heatmap": heatmap_data,
                "busyLevel": busy_levels,
            },
        }
    except Exception as e:
        return {
            "code": settings.err_predict,
            "msg": f"预测失败：{e}",
            "data": None,
        }
