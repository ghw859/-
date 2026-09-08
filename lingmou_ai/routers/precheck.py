"""AI 材料预检接口（Day 4）。

POST /api/ai/precheck
6 类业务材料清单校验，返回 passed + missing[]。
硬约束：不调大模型，纯规则。
Java 的 POST /api/qrcode/generate 会先调本接口做预检。
"""
from typing import Any, Dict

from fastapi import APIRouter
from pydantic import BaseModel, Field

from config.settings import settings
from services.precheck_engine import check

router = APIRouter(prefix="/api/ai", tags=["precheck"])


class PrecheckRequest(BaseModel):
    businessType: str = Field(..., description="业务类型，如 OPEN_ACCOUNT")
    materials: Dict[str, Any] = Field(
        default_factory=dict, description="提交的材料字段字典"
    )


@router.post("/precheck")
async def precheck(req: PrecheckRequest):
    """6 类业务材料清单校验"""
    if not req.businessType or not req.businessType.strip():
        return {
            "code": settings.err_param,
            "msg": "businessType 不能为空",
            "data": None,
        }

    try:
        passed, missing, required, checked = check(
            req.businessType, req.materials
        )
    except ValueError as e:
        return {
            "code": settings.err_unknown_business,
            "msg": str(e),
            "data": None,
        }
    except Exception as e:
        return {
            "code": settings.err_internal,
            "msg": f"内部错误：{e}",
            "data": None,
        }

    return {
        "code": 0,
        "msg": "success",
        "data": {
            "businessType": req.businessType,
            "passed": passed,
            "missing": missing,
            "required": required,
            "checked": checked,
        },
    }
