"""聊天引擎（Day 2 规则版 / Day 7 情感升级 / Day 8 前端话术对齐）。

演变：
- Day 2：关键词意图识别 + 短模板话术，不调 LLM
- Day 7：新增情感识别（positive/neutral/negative）+ LLM 双路径
- Day 8（前端代码冻结，后端适配前端）：将 AIAssistantView.vue 中 getAIReply
  的知识库话术**按原 if-else 顺序、原文案**逐条移植为本文件 KB_RULES，
  规则版独立保证演示质量，不依赖大模型可用（当前 DASHSCOPE key 401 也不影响）。

匹配语义与 JS 一致：re.search 非锚定、大小写敏感（等价 JS 的 regex.test）。
"""
import re
from typing import Any, Dict, List, Tuple

from services.llm_client import llm_client

# ============ 知识库话术（逐字移植自 AIAssistantView.vue getAIReply）============
# 注意：顺序即优先级，禁止重排（与前端 if-else 链一致）。

_DEFAULT_REPLY = (
    "您好，我是灵枢AI数字人大堂经理。我可以为您推荐网点、查询排队、"
    "指导业务办理。请告诉我您想办理什么业务？"
)

# (intent, 正则, 回复文案)
KB_RULES: List[Tuple[str, str, str]] = [
    ("branch_recommend", r"网点|人少|附近|哪个",
     "【AI网点推荐】根据实时客流分析，目前金融街私人银行旗舰支行客流最少（畅通状态），"
     "AI预测办理时长仅约6分钟，距您2.1km。其次是望京SOHO社区支行，同样畅通，预计5分钟。"
     "建议您优先选择客流畅通的网点，点击左侧'智能预约排队'即可查看详情并预约取号。"),

    ("forex", r"外汇|汇款|跨境|国外",
     "【外汇业务指引】跨境汇款需分两步办理：①购汇（人民币兑外币，约10分钟）"
     "②跨境汇款（填写收款信息，约20分钟）。所需材料：身份证、银行卡、收款人姓名及地址、"
     "收款银行SWIFT代码、汇款用途说明。推荐前往国贸CBD中心支行或北京分行营业部，均支持外汇业务。"),

    ("material", r"材料|带什么|证件",
     "【业务材料清单】不同业务所需材料不同：①开户办卡：身份证+手机号 ②大额取现：身份证+银行卡"
     "（须预约）③跨境汇款：身份证+收款人信息+SWIFT代码 ④理财咨询：身份证+银行卡 "
     "⑤挂失补卡：身份证+手机号。建议办理前确认材料齐全，避免白跑一趟。"
     "您也可点击右下角'AI导航'按钮，输入业务类型获取详细指引。"),

    ("queue", r"排队|等多久|等待",
     "【实时排队查询】目前各网点等待情况：北京分行营业部约25分钟（繁忙）、长安街支行约8分钟"
     "（适中）、金融街支行约3分钟（畅通）、中关村支行约12分钟（适中）。"
     "建议选择畅通网点预约取号，到场扫码激活后享优先叫号，无需久等。"),

    ("appointment", r"预约|取号",
     "【预约取号指引】线上预约流程：①点击左侧'智能预约排队'选择网点 "
     "②选择日期和时段（每半小时放10个号） ③选择业务类型 ④确认后生成虚拟号（灰色） "
     "⑤到店扫码激活为预约号（红色）享优先叫号。注意：未按时到店激活将自动失效并扣除信誉分10分。"),

    ("open_account", r"开户|办卡|新卡",
     "【开户办卡指引】个人开户需携带身份证原件和手机号，办理时长约15分钟。"
     "流程：①填写开户申请（5分钟）②柜面核验制卡（10分钟）。"
     "推荐前往长安街智慧示范支行，支持自助发卡机快速办理，AI预测仅约4分钟。"
     "点击左侧'智能预约排队'即可预约。"),

    ("withdraw", r"取现|现金|取钱",
     "【大额取现指引】大额现金提取须提前预约，办理时长约15分钟。所需材料：身份证+银行卡。"
     "流程：①线上预约提现金额和时段 ②到网点柜面核验取款。推荐北京分行营业部，设有大额现金专柜。"
     "建议避开上午高峰（10:00-11:00），下午14:00后客流较少。"),

    ("card_loss", r"挂失|丢了|补卡",
     "【挂失补卡指引】银行卡遗失请尽快挂失，办理时长约13分钟。"
     "流程：①紧急挂失冻结卡片（3分钟）②补办新卡（10分钟）。"
     "所需材料：身份证+手机号+挂失手续费。推荐长安街智慧示范支行，支持自助发卡机快速补办。"
     "建议立即预约以免资金风险。"),

    ("password_reset", r"密码|忘记|重置",
     "【密码重置指引】银行卡密码重置需到柜面办理，时长约8分钟。"
     "所需材料：身份证+银行卡+手机验证码。流程：①身份核验（3分钟）②密码重置（5分钟）。"
     "推荐前往设有VTM自助终端的网点，如长安街智慧示范支行，可快速办理。"),

    ("greeting", r"你好|hi|hello|您好",
     "您好！我是灵枢AI数字人大堂经理，很高兴为您服务。我可以为您推荐最合适的网点、"
     "查询实时排队情况、指导业务办理流程和所需材料。请问您今天想办理什么业务？"),

    ("help", r"帮助|功能|能做",
     "【大堂经理服务清单】我可以为您提供：①智能网点推荐（按客流+距离+业务匹配）"
     "②实时排队查询 ③业务办理流程指引 ④所需材料清单 ⑤预约取号指导 ⑥办理进度追踪。"
     "您也可点击右下角'AI导航'按钮，输入自然语言快速获取业务指引。"),

    ("thanks", r"谢谢|感谢",
     "不用客气！很高兴能帮到您。如果您在办理业务过程中有任何疑问，随时可以来找我。祝您办理顺利！"),

    # ---- 末尾兜底链（前端最后一个 else-if 的子分支）----
    # 说明：前端该链中的"开户/挂失/外汇"三子分支在主链中已提前命中，永远不可达，
    # 此处按等价行为仅保留可达的 5 条文案。
    ("transfer", r"转账",
     "【对公跨行转账汇款】转账需提供收款人账号、户名和开户行。对公跨行转账还需填写转账用途和加急标识。"
     "通过预填单可自动提取信息。所需材料：营业执照原件、法人身份证原件、公章+财务章、对公账户信息。"),

    ("loan", r"贷款",
     "【贷款咨询】贷款需准备：身份证、收入证明、征信报告、资产证明。"
     "不同贷款类型（经营贷/消费贷/房贷）条件不同，建议到网点详细咨询。"),

    ("large_cash", r"大额",
     "【大额取现指引】大额现金提取须提前预约，办理时长约15分钟。所需材料：身份证+银行卡。"
     "流程：①线上预约提现金额和时段 ②到网点柜面核验取款。"),

    ("business_hours", r"营业",
     "工行网点营业时间一般为09:00-17:00，部分商圈网点延长至19:00。具体可在网点查询页查看。"),

    ("location", r"位置",
     "支持6个网点预约：中关村、建国门、国贸、三里屯、金融街、望京。可在智能预约排队页查看详情。"),
]

_KB_COMPILED = [(intent, re.compile(pattern), reply)
                for intent, pattern, reply in KB_RULES]


def get_kb_reply(message: str) -> Tuple[str, str]:
    """知识库有序匹配（Day 8 长文版）。

    Returns:
        (reply, intent)；未命中返回默认欢迎文案与 'unknown'。
    """
    for intent, regex, reply in _KB_COMPILED:
        if regex.search(message):
            return reply, intent
    return _DEFAULT_REPLY, "unknown"


# ============ 情感识别（Day 7 保留）============

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
    neg_hits = sum(1 for kw in _NEGATIVE_KEYWORDS if kw in message)
    pos_hits = sum(1 for kw in _POSITIVE_KEYWORDS if kw in message)
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
    return f"{_EMOTION_PREFIX.get(emotion, '')}{base_reply}{_EMOTION_SUFFIX.get(emotion, '')}"


# ============ Day 2 对外入口（HTTP POST /api/ai/chat 使用）============

def chat(message: str, history: List[Dict[str, str]]) -> Tuple[str, str]:
    """规则版一站式调用：知识库匹配 → 长文话术。不调大模型。"""
    return get_kb_reply(message)


# ============ Day 7 升级版入口（WebSocket 使用）============

_CHAT_SYSTEM_PROMPT = """你是 ICBC·灵枢 智能银行平台的 AI 数字人大堂经理。
职责：解答银行业务咨询（预约、网点、材料、进度、信用分、直通码等），引导用户使用平台功能。
语气：专业、亲切、简洁。避免冗长，每段控制在 1-3 句话。
如果用户的问题超出银行业务范围，礼貌说明并引导回到业务咨询。"""


async def chat_with_emotion(
    message: str,
    history: List[Dict[str, str]],
) -> Dict[str, Any]:
    """Day 7 升级版 / Day 8 健壮性修订：情感识别 + 回复生成。

    策略（Day 8 修订：规则优先，unknown 才问 LLM）：
    1. 先走 17 条知识库规则；命中（intent != unknown）→ 直接返回确定性长文，
       零 LLM 调用，model="rule_kb"（避免 LLM 把演示话术带偏）
    2. 仅知识库未覆盖（unknown）才尝试 LLM；失败/未配置 → 默认兜底，
       model="rule_fallback"（LLM 超时默认 10s，不长时间卡演示）
    情感识别与安抚语气在所有路径生效。

    Returns:
        {"reply", "intent", "emotion", "model"}
    """
    emotion = detect_emotion(message)
    base_reply, intent = get_kb_reply(message)

    if intent != "unknown":
        # 规则优先：知识库命中，确定性话术，不调 LLM
        reply_text = _apply_emotion_tone(base_reply, emotion)
        model_name = "rule_kb"
    elif llm_client.is_configured:
        # 仅 unknown 才问大模型
        try:
            reply_text = await llm_client.chat_once(
                prompt=message,
                system_prompt=_CHAT_SYSTEM_PROMPT,
                temperature=0.7,
                max_tokens=256,
            )
            model_name = llm_client.model
        except Exception:
            # LLM 失败（如 key 401 / 超时）→ 默认兜底
            reply_text = _apply_emotion_tone(base_reply, emotion)
            model_name = "rule_fallback"
    else:
        # 无 Key → 默认兜底
        reply_text = _apply_emotion_tone(base_reply, emotion)
        model_name = "rule_fallback"

    return {
        "reply": reply_text,
        "intent": intent,
        "emotion": emotion,
        "model": model_name,
    }
