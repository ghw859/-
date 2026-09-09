"""聊天引擎（Day 2 规则版 + Day 7 升级）。

Day 2: 关键词匹配意图识别 + 话术模板 → 不调 LLM
Day 7: 新增情感识别 + LLM 双路径升级
       - 情感识别：关键词规则 → positive / neutral / negative
       - LLM 路径：有 Key 走通义千问 → 无 Key 规则兜底
       - 情感影响回复语气（negative 时加安抚前缀）
"""
from typing import Any, Dict, List, Tuple

from services.llm_client import llm_client

# ============ 意图识别（Day 2 原逻辑保留）============

INTENT_KEYWORDS: Dict[str, List[str]] = {
    "greeting":    ["你好", "您好", "hi", "hello", "在吗", "有人吗"],
    "appointment": ["预约", "挂号", "排队", "取号", "时段", "几点能办"],
    "branch":      ["网点", "地址", "在哪", "营业时间", "几点下班"],
    "business":    ["办卡", "存款", "取款", "转账", "开户", "挂失", "理财", "贷款", "汇款"],
    "material":    ["材料", "证件", "带什么", "需要什么", "身份证"],
    "credit":      ["信用分", "信用", "积分"],
    "progress":    ["进度", "办理", "到哪一步", "叫号"],
    "qrcode":      ["二维码", "直通码", "凭证", "扫码"],
    "thanks":      ["谢谢", "感谢", "thanks"],
    "bye":         ["再见", "拜拜", "bye", "88"],
}

INTENT_REPLIES: Dict[str, str] = {
    "greeting":    "您好！我是灵枢数字大堂经理。可以为您介绍预约、网点、所需材料、办理进度等。请问需要办理什么业务？",
    "appointment": "您可在「智能预约」页选择网点与时段，每时段半小时、最多 10 人；预约成功后会生成凭证号与排队号。",
    "branch":      "本系统支持 4 个网点查询与推荐。在「网点导航」可查看地址、营业时间与实时繁忙度。",
    "business":    "常见业务：办卡、开户、存款、取款、转账、挂失、理财、贷款。请告知具体业务，我会告知所需材料与流程。",
    "material":    "不同业务材料不同。生成直通码前可用「AI 材料预检」自查。例如开户需身份证 + 手机号，挂失需身份证 + 预留信息。",
    "credit":      "信用分影响预约资格：办结 +3、取消 -5、超时 -10。可在「数据总览」查看信用趋势。",
    "progress":    "办理进度：取号 → 排队 → 叫号 → 办理 → 完成。请前往「办理进度」页实时查看。",
    "qrcode":      "业务直通码为正方形 + 青色边框 + 「直通」标识 + SN 流水号，仅在预约成功后生成，不在导航栏入口。",
    "thanks":      "不客气，很高兴为您服务！还有其他问题吗？",
    "bye":         "再见！祝您办理顺利。",
}

DEFAULT_REPLY = (
    "抱歉，我还在学习中，暂时没理解您的意思。"
    "您可以尝试问我：如何预约、网点在哪、办卡需要什么材料、办理进度怎么看 等。"
)

# ============ 情感识别（Day 7 新增）============

_POSITIVE_KEYWORDS = [
    "谢谢", "感谢", "太好了", "真棒", "赞", "好的", "明白", "没问题",
    "开心", "喜欢", "满意", "不错", "方便", "快捷", "顺利", "很好",
    "👍", "😊", "😄", "🙏",
]
_NEGATIVE_KEYWORDS = [
    "不好", "糟糕", "失败", "麻烦", "慢", "贵", "差", "生气", "烦",
    "投诉", "骗", "坑", "不行", "不能", "办不了", "卡壳", "排队太久",
    "怎么回事", "搞什么", "太慢了", "太差了", "退款", "退钱",
    "😠", "😡", "😤", "😭", "💢",
]


def detect_emotion(message: str) -> str:
    """简单情感识别 → positive / neutral / negative"""
    msg = message.lower()
    neg_hits = sum(1 for kw in _NEGATIVE_KEYWORDS if kw in msg)
    pos_hits = sum(1 for kw in _POSITIVE_KEYWORDS if kw in msg)
    if neg_hits > pos_hits:
        return "negative"
    if pos_hits > neg_hits:
        return "positive"
    return "neutral"


_EMOTION_PREFIX = {
    "negative": "抱歉让您感到不便，",
    "positive": "",
    "neutral": "",
}
_EMOTION_SUFFIX = {
    "negative": " 如果您有更具体的问题，我可以帮您进一步查询。",
    "positive": "",
    "neutral": "",
}


def _apply_emotion_tone(base_reply: str, emotion: str) -> str:
    """根据情感给回复加前缀/后缀"""
    prefix = _EMOTION_PREFIX.get(emotion, "")
    suffix = _EMOTION_SUFFIX.get(emotion, "")
    return f"{prefix}{base_reply}{suffix}"


# ============ Day 2 原接口（保留不动）============

def detect_intent(message: str) -> str:
    """关键词匹配，返回首个命中意图"""
    msg = message.lower()
    for intent, keywords in INTENT_KEYWORDS.items():
        if any(kw in msg for kw in keywords):
            return intent
    return "unknown"


def generate_reply(intent: str) -> str:
    """根据意图返回话术（Day 2 规则版）"""
    return INTENT_REPLIES.get(intent, DEFAULT_REPLY)


def chat(message: str, history: List[Dict[str, str]]) -> Tuple[str, str]:
    """一站式调用：意图识别 + 话术生成（Day 2 规则版，保留不动）"""
    intent = detect_intent(message)
    reply = generate_reply(intent)
    return reply, intent


# ============ Day 7 升级接口（新增）============

_CHAT_SYSTEM_PROMPT = """你是 ICBC·灵枢 智能银行平台的 AI 数字人大堂经理。
职责：解答银行业务咨询（预约、网点、材料、进度、信用分、直通码等），引导用户使用平台功能。
语气：专业、亲切、简洁。避免冗长，每段控制在 1-3 句话。
如果用户的问题超出银行业务范围，礼貌说明并引导回到业务咨询。"""


async def chat_with_emotion(
    message: str,
    history: List[Dict[str, str]],
) -> Dict[str, Any]:
    """Day 7 升级版：意图识别 + 情感识别 + 回复生成（LLM / 规则双路径）

    Args:
        message: 用户当前输入
        history: 对话历史（Redis 读取的 [{role, content}, ...]）

    Returns:
        {
            "reply": str,        # 回复文本
            "intent": str,       # 意图标签
            "emotion": str,      # 情感标签 positive/neutral/negative
            "model": str         # qwen-turbo 或 rule_fallback
        }
    """
    emotion = detect_emotion(message)
    intent = detect_intent(message)

    if llm_client.is_configured:
        # --- LLM 路径 ---
        try:
            # 构造 messages：system + history + 当前用户输入
            messages: List[Dict[str, str]] = [
                {"role": "system", "content": _CHAT_SYSTEM_PROMPT},
            ]
            for h in history[-10:]:  # 最多带 10 条历史
                messages.append({"role": h["role"], "content": h["content"]})
            messages.append({"role": "user", "content": message})

            reply_text = await llm_client.chat_once(
                prompt=message,
                system_prompt=_CHAT_SYSTEM_PROMPT,
                temperature=0.7,
                max_tokens=256,
            )
            model_name = llm_client.model
        except Exception:
            # LLM 失败 → 降级规则兜底
            base_reply = generate_reply(intent)
            reply_text = _apply_emotion_tone(base_reply, emotion)
            model_name = "rule_fallback"
    else:
        # --- 无 Key → 规则兜底 ---
        base_reply = generate_reply(intent)
        reply_text = _apply_emotion_tone(base_reply, emotion)
        model_name = "rule_fallback"

    return {
        "reply": reply_text,
        "intent": intent,
        "emotion": emotion,
        "model": model_name,
    }
