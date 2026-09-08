"""AI 预填单规则兜底解析接口（Day 5）。

POST /api/ai/parse-preform
仅兜底规则增强，不调大模型（守约束）。
主体解析由前端正则完成，本接口处理前端兜不住的场景。
"""
from typing import Optional

from fastapi import APIRouter
from pydantic import BaseModel, Field

from config.settings import settings
from services.preform_parser import parse

router = APIRouter(prefix="/api/ai", tags=["parse-preform"])


class ParseRequest(BaseModel):
    rawText: str = Field(..., min_length=1, max_length=2000, description="前端兜不住的原始文本")
    businessType: Optional[str] = Field(
        None, description="前端已识别的业务类型（不传则由本接口推断）"
    )


@router.post("/parse-preform")
async def parse_preform(req: ParseRequest):
    """规则兜底解析预填单（不调大模型）"""
    try:
        result = parse(req.rawText, req.businessType)
    except Exception as e:
        return {
            "code": settings.err_internal,
            "msg": f"内部错误：{e}",
            "data": None,
        }

    # 信息严重不足判定
    if result["confidence"] < settings.parse_confidence_threshold:
        return {
            "code": settings.err_parse_failed,
            "msg": "无法解析：原始文本信息不足",
            "data": result,
        }

    return {
        "code": 0,
        "msg": "success",
        "data": result,
    }
