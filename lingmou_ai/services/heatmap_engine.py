"""客流热力图引擎（Day 3）。

三层算法：
1. 基线模式（银行典型日客流 24 时段）
2. 网点差异因子（每个网点一个繁忙系数）
3. 时序预测（最小二乘线性回归，纯 numpy 实现；如有 sklearn 自动切换）

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


def _weekday_code(d: date) -> int:
    """周一=0 ... 周日=6"""
    return d.weekday()


def _is_weekend(d: date) -> int:
    return 1 if d.weekday() >= 5 else 0


def _synthesize_history(branch_id: int, days: int) -> np.ndarray:
    """合成过去 days 天 × 24 时段的历史客流。

    Day 3 用合成数据；Day 8 联调时改由 Java 拉取真实数据。
    合成规则：基线 × 网点系数 × (1 + 确定性扰动) × 周末衰减。
    """
    branch = settings.branches_seed[branch_id - 1]
    factor = branch["busy_factor"]
    today = date.today()
    rows = []
    for i in range(days):
        d = today - timedelta(days=days - i)
        weekend_factor = 0.55 if _is_weekend(d) else 1.0
        seed = settings.heatmap_seed_base + branch_id * 1000 + (d.toordinal() % 100000)
        rng = random.Random(seed)
        for hour in range(24):
            base = settings.hourly_baseline[hour]
            noise = 1.0 + rng.uniform(-0.08, 0.08)
            val = base * factor * weekend_factor * noise
            rows.append([i, _weekday_code(d), _is_weekend(d), hour, max(0, val)])
    return np.array(rows, dtype=float)


def _numpy_linear_fit(X: np.ndarray, y: np.ndarray) -> np.ndarray:
    """最小二乘法求解线性回归系数（含截距）。

    返回 shape=(n_features+1,) 的系数，最后一项为截距。
    """
    X_b = np.c_[X, np.ones(X.shape[0])]  # 追加截距列
    # 解正规方程 (X^T X) w = X^T y
    coef, _, _, _ = np.linalg.lstsq(X_b, y, rcond=None)
    return coef


def _train_and_predict(branch_id: int) -> List[int]:
    """训练线性回归并预测今日 24 时段"""
    history = _synthesize_history(branch_id, settings.heatmap_history_days)
    X = history[:, :4]  # [day_offset, weekday, is_weekend, hour]
    y = history[:, 4]
    today = date.today()
    X_today = np.array(
        [[settings.heatmap_history_days, _weekday_code(today), _is_weekend(today), h]
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

    branch = settings.branches_seed[branch_id - 1]
    factor = branch["busy_factor"]
    today_weekend_factor = 0.55 if _is_weekend(today) else 1.0
    result = []
    for h in range(24):
        base = settings.hourly_baseline[h] * factor * today_weekend_factor
        # 50% 模型预测 + 50% 基线，保证既有学习效果又不会飘出银行形态
        merged = 0.5 * float(preds[h]) + 0.5 * base
        result.append(max(0, round(merged)))
    return result


def predict_branch(branch_id: int) -> List[int]:
    """对外接口：预测某网点今日 24 时段客流"""
    return _train_and_predict(branch_id)


def predict_all() -> Dict[int, List[int]]:
    """预测所有网点"""
    return {b["id"]: predict_branch(b["id"]) for b in settings.branches_seed}


def compute_busy_level(values: List[int]) -> str:
    """根据 24 时段总客流判定整体繁忙度"""
    total = sum(values)
    if total < 600:
        return "IDLE"
    if total < 1000:
        return "MODERATE"
    if total < 1500:
        return "BUSY"
    return "VERY_BUSY"


def time_slots() -> List[str]:
    """返回 24 个整点字符串"""
    return [f"{h:02d}:00" for h in range(24)]
