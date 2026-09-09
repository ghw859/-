"""预填单规则兜底解析引擎（Day 5）。

⚠ 红线：本模块严禁调用 services.llm_client，只做规则兜底。
主体解析由前端正则完成，本接口处理前端兜不住的场景：
1. 中文数字金额（"五万" → 50000）
2. 身份证 X 结尾（18 位 17 数字+X）
3. 业务类型关键词推断（"我要存钱" → DEPOSIT）
4. 手机号清洗分隔符（"138-0013-8000" → 13800138000）
5. 多字段拼一起的分词归位
6. 地址含特殊字符的宽松匹配
"""
import re
from typing import Any, Dict, Optional

from config.settings import settings


# ============ 中文数字转换 ============

_CN_NUM = {
    "零": 0, "〇": 0, "一": 1, "二": 2, "两": 2, "三": 3, "四": 4,
    "五": 5, "六": 6, "七": 7, "八": 8, "九": 9,
}
_CN_UNIT = {
    "十": 10, "拾": 10, "百": 100, "千": 1000,
    "万": 10000, "亿": 100000000,
}


def cn2num(s: str) -> Optional[int]:
    """中文数字转 int。

    Examples:
        "五万"   → 50000
        "三千五百" → 3500
        "三"     → 3
        "十二"   → 12
    """
    if not s or not all(c in _CN_NUM or c in _CN_UNIT for c in s):
        return None
    total = 0
    unit_acc = 0  # 当前单位累计
    section = 0   # 万/亿分段累计
    for ch in s:
        if ch in _CN_NUM:
            unit_acc = _CN_NUM[ch]
        elif ch in _CN_UNIT:
            u = _CN_UNIT[ch]
            if u >= 10000:
                section = (section + unit_acc * u) if unit_acc else section * u
                if not unit_acc:
                    section = section * u
                total += section
                section = 0
                unit_acc = 0
            else:
                section += (unit_acc or 1) * u
                unit_acc = 0
    total += section + unit_acc
    return total or None


# ============ 字段提取 ============

_NAME_RE = re.compile(r"(?:我叫|我是|姓名[：:]\s*)\s*([\u4e00-\u9fa5]{2,4})")
_ID_CARD_RE = re.compile(r"(?:身份证[号码]?\s*[：:]?\s*)?(\d{17}[\dXx])")
_PHONE_RE = re.compile(r"(?:手机|电话|联系[方式]?\s*[：:]?\s*)?(\d[\d\-\s()]{8,}\d)")
_AMOUNT_RE = re.compile(r"(?:金额|钱\s*)[：:]?\s*(\d+(?:\.\d+)?)\s*[元块万亿]?|(\d+(?:\.\d+)?)\s*[元块](?:整)?")
_AMOUNT_CN_RE = re.compile(r"([\u4e00-\u9fa5]{1,7})")
_ADDRESS_RE = re.compile(r"(?:地址|住址|住在|地址[：:]?\s*)([^\s,，。.；;]{4,80})")
_CARD_NUMBER_RE = re.compile(r"(?:卡号|原卡号|账号\s*[：:]?\s*)([\d]{16,19})")


def _extract_name(text: str) -> Optional[str]:
    m = _NAME_RE.search(text)
    return m.group(1) if m else None


def _extract_id_card(text: str) -> Optional[str]:
    m = _ID_CARD_RE.search(text)
    if not m:
        return None
    id_card = m.group(1).upper()
    if len(id_card) != 18:
        return None
    return id_card


def _extract_phone(text: str) -> Optional[str]:
    for m in _PHONE_RE.finditer(text):
        cleaned = re.sub(r"[\-\s()（）]", "", m.group(1))
        # 国内手机号 11 位，1 开头
        if len(cleaned) == 11 and cleaned.startswith("1"):
            return cleaned
    return None


def _extract_amount(text: str) -> Optional[int]:
    """提取金额，先阿拉伯数字（需带单位/关键词），无则查中文数字"""
    # 阿拉伯数字必须带"金额/钱"关键词 或 "元/块"单位
    for m in _AMOUNT_RE.finditer(text):
        val_str = m.group(1) or m.group(2)
        if not val_str:
            continue
        try:
            val = float(val_str)
            if val > 0:
                # 检查单位
                end = text[m.end():m.end()+2]
                if "万" in end:
                    return int(val * 10000)
                if "亿" in end:
                    return int(val * 100000000)
                return int(val)
        except (ValueError, IndexError):
            continue

    # 中文数字：抓金额关键词附近
    for kw in ["存入", "存款", "存钱", "存", "金额", "转账", "汇款", "投资", "贷款", "借款"]:
        idx = text.find(kw)
        if idx >= 0:
            tail = text[idx + len(kw): idx + len(kw) + 12]
            cm = _AMOUNT_CN_RE.match(tail)
            if cm:
                cn_str = cm.group(1)
                # 剥掉货币单位后缀（"五万元"→"五万"），否则 cn2num 校验失败
                cn_str = cn_str.rstrip("元块整")
                # 必须含至少一个中文数字字符
                if cn_str and any(c in _CN_NUM or c in _CN_UNIT for c in cn_str):
                    n = cn2num(cn_str)
                    if n and n > 0:
                        return n
    return None


def _extract_address(text: str) -> Optional[str]:
    m = _ADDRESS_RE.search(text)
    if not m:
        return None
    addr = m.group(1).strip()
    # 允许特殊字符的宽松校验
    if len(addr) < 4:
        return None
    return addr


def _extract_card_number(text: str) -> Optional[str]:
    m = _CARD_NUMBER_RE.search(text)
    return m.group(1) if m else None


# ============ 业务类型推断 ============

def _infer_business_type(text: str) -> Optional[str]:
    """根据关键词推断业务类型"""
    for bt, keywords in settings.business_keywords.items():
        if any(kw in text for kw in keywords):
            return bt
    return None


# ============ 主入口 ============

def parse(raw_text: str, business_type: Optional[str] = None) -> Dict[str, Any]:
    """规则兜底解析预填单文本。

    Args:
        raw_text:      原始文本
        business_type: 前端已识别则传入；None 则本接口推断

    Returns:
        {
            "businessType": str | None,
            "fields":       {name, idCard, phone, amount, address, cardNumber, ...},
            "confidence":   float [0,1],
            "source":       "rule_fallback"
        }
    """
    fields: Dict[str, Any] = {}

    fields["name"] = _extract_name(raw_text)
    fields["idCard"] = _extract_id_card(raw_text)
    fields["phone"] = _extract_phone(raw_text)
    fields["amount"] = _extract_amount(raw_text)
    fields["address"] = _extract_address(raw_text)
    fields["cardNumber"] = _extract_card_number(raw_text)

    # 业务类型：前端未传则推断
    if not business_type:
        business_type = _infer_business_type(raw_text)

    # 清理 None 字段
    fields = {k: v for k, v in fields.items() if v is not None}

    # 计算置信度
    confidence = _calc_confidence(fields, business_type)

    return {
        "businessType": business_type,
        "fields": fields,
        "confidence": confidence,
        "source": "rule_fallback",
    }


def _calc_confidence(fields: Dict[str, Any], business_type: Optional[str]) -> float:
    """根据命中字段数 / 期望字段数计算置信度"""
    if not business_type:
        # 没识别业务类型，看通用字段命中数
        generic = ["name", "idCard", "phone", "amount"]
        hit = sum(1 for k in generic if k in fields)
        return round(hit / len(generic) * 0.6, 2)  # 上限 0.6

    expected = settings.business_expected_fields.get(business_type, [])
    if not expected:
        return 0.5
    hit = sum(1 for k in expected if k in fields)
    return round(hit / len(expected), 2)
