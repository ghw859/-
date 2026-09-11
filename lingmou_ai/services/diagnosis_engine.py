"""AI 诊断引擎（Day 6）。

两层策略：
1. 合成用户画像：以 userId 为种子，确定性生成用户预约/信用/业务画像
   （与 heatmap_engine 的合成历史客流同模式，零 Java 依赖）
2. LLM 生成诊断建议：调通义千问生成自然语言诊断 + 亮点 + 建议
   - 有 API Key：走真 LLM
   - 无 API Key：规则兜底（从画像直接拼诊断文案）

硬约束：响应结构 { code, msg, data }；错误码 6xxxx。
"""
import random
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional

from config.settings import settings
from services.llm_client import llm_client

# ============ 画像合成 ============

_BUSINESS_TYPES = ["开户", "挂失补卡", "大额取现", "转账汇款", "存款", "贷款申请", "理财咨询"]
# Day 8：与前端 6 个北京网点保持同一业务场景
_BRANCH_NAMES = [
    "北京分行营业部", "长安街智慧示范支行", "金融街私人银行旗舰支行",
    "中关村科技创新特色支行", "望京SOHO社区支行", "国贸CBD中心支行",
]


def _synthesize_profile(user_id: int) -> Dict[str, Any]:
    """以 userId 为种子，确定性合成用户画像。

    返回的画像字段喂给 LLM 生成诊断，或规则兜底直接拼接。
    """
    rng = random.Random(user_id * 9301 + 49297)  # 与 heatmap 不同的种子哈希

    # 近 30 天预约数 3-20
    appointment_count = rng.randint(3, 20)
    # 已完成率 60%-100%
    completion_rate = rng.randint(60, 100) / 100.0
    completed = int(appointment_count * completion_rate)

    # 信用分 60-100
    credit_score = rng.randint(60, 100)
    if credit_score >= 90:
        credit_level = "优秀"
    elif credit_score >= 75:
        credit_level = "良好"
    else:
        credit_level = "一般"

    # 高频业务类型（1-3 个）
    num_top = rng.randint(1, 3)
    top_businesses = rng.sample(_BUSINESS_TYPES, num_top)

    # 高频网点（1-2 个）
    num_branches = rng.randint(1, 2)
    top_branches = rng.sample(_BRANCH_NAMES, num_branches)

    # 近 7 天活跃天数 0-7
    active_days = rng.randint(0, 7)

    # 近 30 天取消/超时次数
    cancel_count = rng.randint(0, max(0, appointment_count - completed))

    # 平均办理时长压缩率（相比传统柜面）15%-55%
    time_saved_pct = rng.randint(15, 55)

    # 预填单使用次数
    preform_count = rng.randint(0, min(appointment_count, 12))

    # 安全事件（夜间大额转账等）0-3 笔
    security_flags = rng.randint(0, 3)

    # 综合安全系数
    safety_score = max(70, min(99, 100 - security_flags * 5 + (credit_score - 80) // 2))

    return {
        "userId": user_id,
        "appointmentCount": appointment_count,
        "completedCount": completed,
        "completionRate": round(completion_rate, 2),
        "creditScore": credit_score,
        "creditLevel": credit_level,
        "topBusinesses": top_businesses,
        "topBranches": top_branches,
        "activeDaysLast7": active_days,
        "cancelCount": cancel_count,
        "timeSavedPct": time_saved_pct,
        "preformCount": preform_count,
        "securityFlags": security_flags,
        "safetyScore": safety_score,
    }


# ============ LLM Prompt ============

_SYSTEM_PROMPT = """你是 ICBC·灵枢 智能银行平台的 AI 诊断引擎。
根据以下用户画像数据，生成一段 150-250 字的中文财富健康度诊断建议。
语气专业、亲切，用"灵枢 AI"自称。
输出必须为纯自然语言段落，不要 markdown、不要分点。"""


def _build_user_prompt(profile: Dict[str, Any]) -> str:
    """把画像数据翻译成 LLM 可读的 prompt"""
    biz_str = "、".join(profile["topBusinesses"])
    branch_str = "、".join(profile["topBranches"])
    return f"""用户画像（近30天）：
- 总预约 {profile['appointmentCount']} 笔，已办结 {profile['completedCount']} 笔（办结率 {profile['completionRate']:.0%}），取消/超时 {profile['cancelCount']} 笔
- 信用分 {profile['creditScore']}（{profile['creditLevel']}）
- 高频业务：{biz_str}
- 常去网点：{branch_str}
- 近7天活跃 {profile['activeDaysLast7']} 天
- AI 预填单使用 {profile['preformCount']} 次
- 平均办理时长压缩 {profile['timeSavedPct']}%
- 综合安全系数 {profile['safetyScore']}（识别到 {profile['securityFlags']} 笔夜间/大额风控事件）

请生成诊断建议："""


# ============ 规则兜底诊断 ============

def _rule_based_diagnosis(profile: Dict[str, Any]) -> str:
    """无 LLM 时的规则兜底诊断文案"""
    p = profile
    parts = []
    parts.append(
        f"经灵枢 AI 业务调度与风控模型扫描，近30天您通过平台预约办理柜面业务 {p['appointmentCount']} 笔，"
        f"办结率 {p['completionRate']:.0%}，依托线上资料预填与智能分流，平均办理时长压缩 {p['timeSavedPct']}%。"
    )
    if p['securityFlags'] > 0:
        parts.append(
            f"系统同时识别 {p['securityFlags']} 笔夜间/大额可疑转账并弹窗拦截，"
            f"账户综合安全系数 {p['safetyScore']} 分。"
        )
    else:
        parts.append(f"账户综合安全系数 {p['safetyScore']} 分，近期无异常交易。")

    if p['preformCount'] > 0:
        parts.append(f"您已使用 AI 预填单 {p['preformCount']} 次，柜台免手录体验良好。")
    else:
        parts.append("建议下次办理业务前使用 AI 预填单功能，可进一步压缩办理时长。")

    if p['creditScore'] >= 90:
        parts.append(f"您的信用分 {p['creditScore']}（{p['creditLevel']}），保持按时到店激活可享优先叫号。")
    elif p['cancelCount'] > 2:
        parts.append(f"注意：近30天取消/超时 {p['cancelCount']} 笔，下次超时将扣减信用分 10 分。")
    else:
        parts.append(f"您的信用分 {p['creditScore']}（{p['creditLevel']}），继续保持！")

    return "".join(parts)


def _extract_highlights(profile: Dict[str, Any]) -> List[str]:
    """从画像中提取 2-4 个亮点标签"""
    h: List[str] = []
    if profile["timeSavedPct"] >= 40:
        h.append(f"压缩{profile['timeSavedPct']}%办理时长")
    if profile["safetyScore"] >= 95:
        h.append(f"安全系数{profile['safetyScore']}分")
    if profile["completionRate"] >= 0.9:
        h.append("办结率超90%")
    if profile["preformCount"] >= 5:
        h.append(f"AI预填单{profile['preformCount']}次")
    if not h:
        h.append(f"信用分{profile['creditScore']}")
        h.append(f"预约{profile['appointmentCount']}笔")
    return h[:4]


def _extract_suggestions(profile: Dict[str, Any]) -> List[str]:
    """从画像中生成 2-3 条建议"""
    s: List[str] = []
    if profile["cancelCount"] > 2:
        s.append("建议提前 15 分钟到店，超时将扣信用分 10 分")
    if profile["preformCount"] < profile["appointmentCount"] // 2:
        s.append("推荐使用 AI 预填单，柜台免手录省时 3-5 分钟")
    if profile["securityFlags"] > 0:
        s.append("建议开通大额转账短信预警，强化夜间交易验证")
    # 通用建议
    biz_first = profile["topBusinesses"][0] if profile["topBusinesses"] else "常用业务"
    s.append(f"您常办「{biz_first}」，可关注工行低风险定期产品稳健增值")
    return s[:3]


# ============ 主入口 ============

async def generate_diagnosis(user_id: int) -> Dict[str, Any]:
    """生成 AI 诊断建议。

    Args:
        user_id: 用户 ID（画像合成种子）

    Returns:
        {
            "userId": int,
            "diagnosis": str,
            "highlights": List[str],
            "suggestions": List[str],
            "generatedAt": str,
            "model": str
        }
    """
    profile = _synthesize_profile(user_id)
    highlights = _extract_highlights(profile)
    suggestions = _extract_suggestions(profile)

    diagnosis_text: str
    model_name: str

    if llm_client.is_configured:
        # 调 LLM
        try:
            diagnosis_text = await llm_client.chat_once(
                prompt=_build_user_prompt(profile),
                system_prompt=_SYSTEM_PROMPT,
                temperature=0.6,
                max_tokens=512,
            )
            model_name = llm_client.model
        except Exception:
            # LLM 失败 → 降级规则兜底
            diagnosis_text = _rule_based_diagnosis(profile)
            model_name = "rule_fallback"
    else:
        # 无 Key → 规则兜底
        diagnosis_text = _rule_based_diagnosis(profile)
        model_name = "rule_fallback"

    return {
        "userId": user_id,
        "diagnosis": diagnosis_text,
        "highlights": highlights,
        "suggestions": suggestions,
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "model": model_name,
    }


def compose_text(result: Dict[str, Any]) -> str:
    """Day 8：把内部结构化结果拼成一段纯文本，供前端打字机单段落渲染。

    前端 aiDiagnosis 是单个 <p> 字符串逐字渲染，不接收列表、不识别换行，
    因此对外只暴露 text，且内部用句号/分号连接，不含 \\n。
    """
    parts = [result["diagnosis"].replace("\n", "").replace("\r", "").rstrip("。！!？? ") + "。"]
    if result.get("highlights"):
        parts.append("核心亮点：" + "；".join(result["highlights"]) + "。")
    if result.get("suggestions"):
        parts.append("优化建议：" + "；".join(result["suggestions"]) + "。")
    return "".join(parts)
