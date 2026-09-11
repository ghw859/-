"""客流热力图引擎（Day 3 初版 / Day 8 契约对齐版）。

三层算法（保留）：
1. 基线模式（银行典型日客流 24 时段）
2. 网点差异因子（每个网点一个繁忙系数）
3. 时序预测（最小二乘线性回归，纯 numpy 实现；如有 sklearn 自动切换）

Day 8 改造（前端代码冻结，以后端适配前端）：
- 网点固定 6 个北京网点（名称/顺序/status 与 OverviewView.vue 完全一致）
- 对外只输出营业时间 8 个时段（9:00-16:00）的负载百分比（10-100 整数）
- 线性回归仍参与预测：回归给出当日小时形态，与网点 status 基线 50/50 融合，
  再叠加与前端 buildHeatmap 完全相同的确定性扰动 delta
- 修复 Day 3 遗留 bug：预测日期现在正确透传（旧版内部误用 date.today()）

硬约束：不调大模型。
"""
import random
from datetime import date, timedelta
from typing import List, Dict

import numpy as np

from config.settings import settings

# 可选切换 sklearn（若已安装则用其 LinearRegression，否则用 numpy 最小二乘）
try:
    from sklearn.linear_model import LinearRegression as _SkLR
    _HAS_SKLEARN = True
except ImportError:
    _SkLR = None
    _HAS_SKLEARN = False

# ============ Day 8 前端契约常量（逐字复刻 OverviewView.vue）============

# 营业时段：8 列（9:00-16:00）
BUSINESS_HOUR_INDEXES = list(range(9, 17))

# 各 status 的 8 时段负载基线（前端 basePatterns）
STATUS_BASE_PATTERNS: Dict[str, List[int]] = {
    "busy":     [85, 95, 80, 45, 55, 70, 60, 40],
    "moderate": [55, 65, 50, 25, 35, 45, 40, 30],
    "free":     [30, 35, 28, 15, 20, 25, 22, 18],
}


def business_hours() -> List[str]:
    """返回 8 个营业整点字符串：9:00 ... 16:00"""
    return [f"{h}:00" for h in BUSINESS_HOUR_INDEXES]


# ============ 历史合成与线性回归（三层算法底层）============

def _weekday_code(d: date) -> int:
    """周一=0 ... 周日=6"""
    return d.weekday()


def _is_weekend(d: date) -> int:
    return 1 if d.weekday() >= 5 else 0


def _synthesize_history(branch: dict, days: int, target_date: date) -> np.ndarray:
    """合成过去 days 天 × 24 时段的历史客流。

    合成规则：基线 × 网点系数 × (1 + 确定性扰动) × 周末衰减。
    Day 8 起 branch 为网点 dict，target_date 为预测目标日（历史相对它回溯）。
    """
    factor = branch["busy_factor"]
    rows = []
    for i in range(days):
        d = target_date - timedelta(days=days - i)
        weekend_factor = 0.55 if _is_weekend(d) else 1.0
        seed = settings.heatmap_seed_base + branch["id"] * 1000 + (d.toordinal() % 100000)
        rng = random.Random(seed)
        for hour in range(24):
            base = settings.hourly_baseline[hour]
            noise = 1.0 + rng.uniform(-0.08, 0.08)
            val = base * factor * weekend_factor * noise
            rows.append([i, _weekday_code(d), _is_weekend(d), hour, max(0, val)])
    return np.array(rows, dtype=float)


def _numpy_linear_fit(X: np.ndarray, y: np.ndarray) -> np.ndarray:
    """最小二乘法求解线性回归系数（含截距）。"""
    X_b = np.c_[X, np.ones(X.shape[0])]
    coef, _, _, _ = np.linalg.lstsq(X_b, y, rcond=None)
    return coef


def _predict_24h(branch: dict, target_date: date) -> List[float]:
    """训练线性回归并预测目标日 24 时段客流（人数尺度）。"""
    history = _synthesize_history(branch, settings.heatmap_history_days, target_date)
    X = history[:, :4]  # [day_offset, weekday, is_weekend, hour]
    y = history[:, 4]
    X_today = np.array(
        [[settings.heatmap_history_days, _weekday_code(target_date),
          _is_weekend(target_date), h]
         for h in range(24)],
        dtype=float,
    )

    if _HAS_SKLEARN:
        model = _SkLR()
        model.fit(X, y)
        preds = model.predict(X_today)
    else:
        coef = _numpy_linear_fit(X, y)
        X_today_b = np.c_[X_today, np.ones(X_today.shape[0])]
        preds = X_today_b @ coef

    factor = branch["busy_factor"]
    weekend_factor = 0.55 if _is_weekend(target_date) else 1.0
    result = []
    for h in range(24):
        base = settings.hourly_baseline[h] * factor * weekend_factor
        # 50% 模型预测 + 50% 基线，保证既有学习效果又不飘出银行形态
        merged = 0.5 * float(preds[h]) + 0.5 * base
        result.append(max(0.0, merged))
    return result


# ============ Day 8 对外接口：8 时段负载百分比 ============

def _char_seed(name: str) -> int:
    """与前端完全一致的字符种子：charCode 累加（常用汉字/ASCII 在 Python ord 与
    JS charCodeAt 下数值相同）。"""
    return sum(ord(c) for c in name)


def predict_load(branch: dict, target_date: date) -> List[int]:
    """预测某网点目标日 8 个营业时段（9:00-16:00）的负载百分比。

    输出：8 个 10-100 的整数，尺度与前端 mock 一致（非客流人数）。
    算法：线性回归给出小时形态比例 → 与 status 基线 50/50 融合
          → 叠加前端同款确定性 delta → clamp[10,100]。
    """
    status = branch["status"]
    base_pattern = STATUS_BASE_PATTERNS[status]

    # 1. 线性回归预测 24 时段，切出营业 8 时段
    preds_24 = _predict_24h(branch, target_date)
    preds_8 = [preds_24[h] for h in BUSINESS_HOUR_INDEXES]

    # 2. 回归形态归一为相对均值的比例（限定 0.75-1.25，防止噪声放大）
    mean_pred = sum(preds_8) / len(preds_8) or 1.0
    ratios = [max(0.75, min(1.25, v / mean_pred)) for v in preds_8]

    # 3. 与前端相同的确定性扰动
    seed = _char_seed(branch["name"])

    cells: List[int] = []
    for k, base in enumerate(base_pattern):
        regression_shaped = base * ratios[k]
        merged = 0.5 * base + 0.5 * regression_shaped
        delta = ((seed + k * 7) % 11) - 5
        val = int(round(max(10, min(100, merged + delta))))
        cells.append(val)
    return cells


def predict_all_loads(target_date: date) -> Dict[int, List[int]]:
    """预测全部 6 个网点的 8 时段负载。"""
    return {
        b["id"]: predict_load(b, target_date)
        for b in settings.branches_seed
    }
