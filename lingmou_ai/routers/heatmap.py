"""AI 客流预测热力图接口（Day 3 初版 / Day 8 契约对齐版）。

GET /api/ai/heatmap?date=today
严格适配前端 OverviewView.vue 冻结的数据形态：
- 6 个固定北京网点 × 8 个营业时段（9:00-16:00）
- 单元格 {val:10-100整数, bg, textClass}，颜色类名与前端 valToClass 逐字一致
- 行 {branch, shortName, cells}，tip 推荐语与前端拼接逻辑逐字一致
- 网点状态小写三态 free / moderate / busy

硬约束：不调大模型，确定性算法 + 线性回归时序预测。
缓存：heatmap:v2:{date}（v2 为 Day8 新契约，与旧 4×24 缓存键隔离）。
"""
import json
from datetime import date, datetime
from typing import Optional, Dict

from fastapi import APIRouter, Query

from config.settings import settings
from services.redis_client import redis_client
from services.heatmap_engine import predict_load, business_hours

router = APIRouter(prefix="/api/ai", tags=["heatmap"])

# 前端 valToClass 阈值逐字复刻：<35 绿 / <60 黄 / 其余红
def _val_to_class(val: int) -> Dict[str, str]:
    if val < 35:
        return {"bg": "bg-emerald-400", "textClass": "text-white"}
    if val < 60:
        return {"bg": "bg-amber-400", "textClass": "text-white"}
    return {"bg": "bg-rose-400", "textClass": "text-white"}


def _short_name(name: str) -> str:
    """行名截断：超 8 字保留前 7 字 + …（前端 name.length > 8 分支）"""
    return name[:7] + "…" if len(name) > 8 else name


def _tip_short(name: str) -> str:
    """tip 网点名截断：超 10 字保留前 9 字 + …（前端 globalMin 分支）"""
    return name[:9] + "…" if len(name) > 10 else name


def _parse_date(date_param: str) -> Optional[date]:
    """解析 date 参数，支持 'today' 和 'yyyy-mm-dd'"""
    if date_param == "today":
        return date.today()
    try:
        return datetime.strptime(date_param, "%Y-%m-%d").date()
    except ValueError:
        return None


def _build_payload(target: date) -> Dict:
    """按前端冻结契约组装完整 data"""
    hours = business_hours()
    rows = []
    global_min = {"val": 999, "branch": "", "hour": ""}

    for branch in settings.branches_seed:
        values = predict_load(branch, target)
        cells = []
        for k, val in enumerate(values):
            cells.append({"val": val, **_val_to_class(val)})
            # 与前端一致：严格小于、按 网点序×时段序 取第一个最小值
            if val < global_min["val"]:
                global_min = {"val": val, "branch": branch["name"], "hour": hours[k]}
        rows.append({
            "branch": branch["name"],
            "shortName": _short_name(branch["name"]),
            "cells": cells,
        })

    # tip 逐字复刻：AI推荐：{网点} {起}-{止}:00 全网客流最低（仅{val}%负载），建议此时段到店办理
    end_hour = f"{int(global_min['hour'].split(':')[0]) + 1}:00"
    tip = (
        f"AI推荐：{_tip_short(global_min['branch'])} "
        f"{global_min['hour']}-{end_hour} 全网客流最低"
        f"（仅{global_min['val']}%负载），建议此时段到店办理"
    )

    return {
        "date": target.isoformat(),
        "branches": [
            {"id": b["id"], "name": b["name"], "status": b["status"]}
            for b in settings.branches_seed
        ],
        "hours": hours,
        "rows": rows,
        "tip": tip,
    }


@router.get("/heatmap")
async def heatmap(date_param: str = Query(..., alias="date", description="today 或 yyyy-mm-dd")):
    """返回 6 网点 × 8 营业时段客流热力图（前端契约形态）"""
    target = _parse_date(date_param)
    if target is None:
        return {
            "code": settings.err_param,
            "msg": f"date 参数不合法：{date_param}（应为 'today' 或 'yyyy-mm-dd'）",
            "data": None,
        }

    cache_key = f"heatmap:v2:{target.isoformat()}"

    # 1. Redis 缓存（整图一个键，支撑 <200ms 目标）
    try:
        client = await redis_client.get_client()
        cached = await client.get(cache_key)
        if cached:
            return {"code": 0, "msg": "success (cached)", "data": json.loads(cached)}
    except Exception:
        pass

    # 2. 预测组装
    try:
        payload = _build_payload(target)
    except Exception as e:
        return {
            "code": settings.err_predict,
            "msg": f"预测失败：{e}",
            "data": None,
        }

    # 3. 写缓存（Redis 不可用不影响返回）
    try:
        client = await redis_client.get_client()
        await client.set(cache_key, json.dumps(payload, ensure_ascii=False),
                         ex=settings.heatmap_cache_ttl)
    except Exception:
        pass

    return {"code": 0, "msg": "success", "data": payload}
