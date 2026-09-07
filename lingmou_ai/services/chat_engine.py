"""规则版聊天引擎（Day 2）。

基于关键词匹配的意图识别 + 话术模板。
不调用大模型，所有回复均为确定性生成。
Day 7 升级为 WebSocket + 情感识别时再扩展。
"""
from typing import Dict, List, Tuple

# 意图 -> 关键词
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

# 意图 -> 话术
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


def detect_intent(message: str) -> str:
    """关键词匹配，返回首个命中意图"""
    msg = message.lower()
    for intent, keywords in INTENT_KEYWORDS.items():
        if any(kw in msg for kw in keywords):
            return intent
    return "unknown"


def generate_reply(intent: str) -> str:
    """根据意图返回话术（Day 2 规则版，不调 LLM）"""
    return INTENT_REPLIES.get(intent, DEFAULT_REPLY)


def chat(message: str, history: List[Dict[str, str]]) -> Tuple[str, str]:
    """一站式调用：意图识别 + 话术生成"""
    intent = detect_intent(message)
    reply = generate_reply(intent)
    return reply, intent
