"""材料预检规则引擎（Day 4）。

基于 6 类业务的材料清单做校验，返回 passed + missing[]。
不调大模型，纯规则。
"""
from typing import Any, Dict, List, Tuple

from config.settings import settings

# 业务类型别名（Day 9 联调兼容）：Java DTO 注释示例使用 TRANSFER / LOAN，
# 前端可能透传别名；统一归一化到 6 个标准枚举，避免误返 60004
_BUSINESS_ALIASES = {
    "TRANSFER": "LARGE_TRANSFER",
    "LOAN": "LOAN_APPLICATION",
}


def _is_valid(value: Any) -> bool:
    """判定材料字段是否有效。

    - None / False / 空字符串 / "null" / "undefined" / 0  → 视为未提供
    - 非空字符串 / True / 正数 / 其他类型                  → 视为有效
    """
    if value is None:
        return False
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        v = value.strip()
        return len(v) > 0 and v.lower() not in ("null", "undefined", "none")
    if isinstance(value, (int, float)):
        return value > 0
    return True


def check(
    business_type: str,
    materials: Dict[str, Any],
) -> Tuple[bool, List[str], List[str], List[str]]:
    """校验材料完整性。

    Args:
        business_type: 业务类型枚举（如 OPEN_ACCOUNT，兼容别名 TRANSFER / LOAN）
        materials:     前端提交的材料字段字典

    Returns:
        (passed, missing, required, checked)
        - passed:   是否齐全
        - missing:  缺失字段名列表
        - required: 该业务必填字段名列表
        - checked:  本次已有效提交的字段名列表
    """
    # 别名归一化：TRANSFER→LARGE_TRANSFER、LOAN→LOAN_APPLICATION
    business_type = _BUSINESS_ALIASES.get(
        str(business_type).upper(), str(business_type).upper()
    )
    required = settings.business_materials.get(business_type)
    if required is None:
        raise ValueError(f"未知的业务类型：{business_type}")

    checked = [k for k in required if _is_valid(materials.get(k))]
    missing = [k for k in required if k not in checked]
    passed = len(missing) == 0
    return passed, missing, required, checked
