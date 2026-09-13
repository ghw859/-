package com.icbc.lingmou.service.support;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * T3 业务直通码预检适配层。
 *
 * 背景：前端 5 个业务枚举（预填单模板，代码冻结）与 Python precheck 的标准枚举
 * 命名/材料字段不同，直接透传会全部误报 60004。为保持
 *   - 前端枚举不动
 *   - Python /api/ai/precheck 请求/响应契约不动
 * 由 Java 在调用 AI 前做一次归一化：业务类型映射 + 材料字段改名，
 * 返回 missing 时再把字段键翻成中文标签给前端弹窗展示。
 *
 * 前端业务类型 → AI 枚举：
 *   cash_reserve  → CASH_RESERVE   大额取现预约
 *   open_card     → OPEN_CARD      办卡开户
 *   corp_transfer → CORP_TRANSFER  对公跨行转账汇款
 *   cash_deposit  → CASH_DEPOSIT   对公现金缴款
 *   fx_exchange   → FX_EXCHANGE    外币兑换
 * 非以上枚举（如 6 个标准枚举 OPEN_ACCOUNT / TRANSFER 等）原样大写透传。
 */
@Component
public class PrecheckBizSupport {

    /** 前端业务类型 → AI 预检业务枚举 */
    private static final Map<String, String> BIZ_TYPE_TO_AI = Map.of(
            "cash_reserve", "CASH_RESERVE",
            "open_card", "OPEN_CARD",
            "corp_transfer", "CORP_TRANSFER",
            "cash_deposit", "CASH_DEPOSIT",
            "fx_exchange", "FX_EXCHANGE"
    );

    /** 前端业务类型 → 中文名（审计落链与返回展示复用） */
    private static final Map<String, String> BIZ_TYPE_NAME = Map.of(
            "cash_reserve", "大额取现预约",
            "open_card", "办卡开户",
            "corp_transfer", "对公跨行转账汇款",
            "cash_deposit", "对公现金缴款",
            "fx_exchange", "外币兑换"
    );

    /**
     * 每个前端业务的材料字段映射：AI 字段键 → 前端材料键。
     * 顺序即预检弹窗推荐展示顺序。
     */
    private static final Map<String, LinkedHashMap<String, String>> FIELD_MAPPING = new HashMap<>();

    static {
        // 大额取现：身份证、金额、预约日期
        FIELD_MAPPING.put("cash_reserve", ordered(
                "idCard", "idCard",
                "amount", "amount",
                "date", "date"
        ));
        // 办卡开户：身份证、实名手机号、地址
        FIELD_MAPPING.put("open_card", ordered(
                "idCard", "idCard",
                "phone", "phone",
                "address", "address"
        ));
        // 对公跨行转账：身份证、付款单位/账号、收款方名称/账号、金额
        FIELD_MAPPING.put("corp_transfer", ordered(
                "idCard", "idCard",
                "payerName", "payerName",
                "payerAccount", "payerAccount",
                "payeeName", "recvName",
                "payeeAccount", "recvCard",
                "amount", "amount"
        ));
        // 对公现金缴款：付款单位/账号、金额、缴款方式
        FIELD_MAPPING.put("cash_deposit", ordered(
                "payerName", "payerName",
                "payerAccount", "payerAccount",
                "amount", "amount",
                "depositMethod", "depositMethod"
        ));
        // 外币兑换：身份证、币种、方向、外币金额
        FIELD_MAPPING.put("fx_exchange", ordered(
                "idCard", "idCard",
                "fxCurrency", "fxCurrency",
                "fxDirection", "fxDirection",
                "fxAmount", "fxAmount"
        ));
    }

    /** AI/标准材料键 → 中文标签（预检缺失弹窗展示） */
    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("idCard", "身份证原件"),
            Map.entry("phone", "实名认证手机号"),
            Map.entry("address", "联系地址"),
            Map.entry("amount", "业务金额"),
            Map.entry("date", "预约办理日期"),
            Map.entry("payerName", "付款单位名称"),
            Map.entry("payerAccount", "付款单位账号"),
            Map.entry("payeeName", "收款方名称"),
            Map.entry("payeeAccount", "收款方账号"),
            Map.entry("depositMethod", "缴款方式"),
            Map.entry("fxCurrency", "兑换币种"),
            Map.entry("fxDirection", "兑换方向"),
            Map.entry("fxAmount", "外币金额"),
            // Python 6 个标准枚举可能出现的键，一并给标签
            Map.entry("cardNumber", "银行卡号"),
            Map.entry("incomeProof", "收入证明"),
            Map.entry("employer", "工作单位信息"),
            Map.entry("loanTerm", "贷款期限"),
            Map.entry("riskAssessment", "风险评估报告"),
            Map.entry("investAmount", "投资/认购金额")
    );

    private static LinkedHashMap<String, String> ordered(String... kv) {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    /**
     * 归一化业务类型：前端 5 枚举映射为 AI 枚举；其他原样大写透传
     * （Python 侧另兼容 TRANSFER/LOAN 别名）。
     */
    public String normalizeBizType(String frontendBizType) {
        if (frontendBizType == null || frontendBizType.isBlank()) {
            return frontendBizType;
        }
        String key = frontendBizType.trim();
        return BIZ_TYPE_TO_AI.getOrDefault(key, key.toUpperCase());
    }

    /**
     * 按映射表把前端材料字典重命名为 AI 预检材料字典；
     * 未知业务类型不做字段改造，原样透传。
     */
    public Map<String, Object> normalizeMaterials(String frontendBizType, Map<String, Object> rawMaterials) {
        Map<String, Object> raw = rawMaterials != null ? rawMaterials : Collections.emptyMap();
        LinkedHashMap<String, String> mapping = FIELD_MAPPING.get(frontendBizType);
        if (mapping == null) {
            return new HashMap<>(raw);
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : mapping.entrySet()) {
            Object value = raw.get(e.getValue());
            if (value != null) {
                normalized.put(e.getKey(), value);
            }
        }
        return normalized;
    }

    /**
     * 材料键转中文标签；未知键原样返回（避免弹窗出现空白项）。
     */
    public String label(String fieldKey) {
        return FIELD_LABELS.getOrDefault(fieldKey, fieldKey);
    }

    public List<String> labels(List<String> fieldKeys) {
        if (fieldKeys == null) {
            return Collections.emptyList();
        }
        List<String> labels = new ArrayList<>(fieldKeys.size());
        for (String key : fieldKeys) {
            labels.add(label(key));
        }
        return labels;
    }

    /**
     * 业务类型中文名；未知类型返回原值。
     */
    public String bizTypeName(String bizType) {
        if (bizType == null) {
            return "其他业务";
        }
        return BIZ_TYPE_NAME.getOrDefault(bizType, bizType);
    }
}
