package com.icbc.lingmou.common;

/**
 * 错误码定义
 * 段位分配（5位数）：
 *   用户 1xxxx / 预约 2xxxx / 网点 3xxxx / 预填单 4xxxx / 审计 5xxxx / AI 6xxxx / 系统 9xxxx
 */
public enum ResultCode {

    // ========== 通用（归入用户段） ==========
    SUCCESS(0, "成功"),
    PARAM_ERROR(10001, "参数错误"),
    UNAUTHORIZED(10002, "未登录或登录已过期"),
    FORBIDDEN(10003, "无权限"),
    NOT_FOUND(10004, "资源不存在"),

    // ========== 用户认证模块（1xxxx） ==========
    USER_NOT_EXIST(11001, "用户不存在"),
    PASSWORD_ERROR(11002, "密码错误"),
    USER_EXIST(11003, "用户名已存在"),
    PHONE_EXIST(11004, "手机号已注册"),
    ID_CARD_EXIST(11005, "身份证已注册"),
    SMS_CODE_ERROR(11006, "验证码错误或已过期"),
    SMS_CODE_SEND_FAIL(11007, "验证码发送失败"),
    TOKEN_INVALID(11008, "Token无效"),
    CREDIT_TOO_LOW(11009, "信用分过低，暂无法预约"),

    // ========== 预约管理模块（2xxxx） ==========
    APPOINTMENT_CONFLICT(20001, "该时段已在其他网点预约"),
    SLOT_FULL(20002, "该时段已约满"),
    APPOINTMENT_NOT_FOUND(20003, "预约不存在"),
    APPOINTMENT_EXPIRED(20004, "预约已过期"),
    APPOINTMENT_COMPLETED(20005, "预约已完成"),
    APPOINTMENT_CANCELLED(20006, "预约已取消"),
    VOUCHER_NUM_NOT_FOUND(20007, "凭证号不存在"),
    VOUCHER_NUM_GENERATE_FAIL(20008, "凭证号生成失败"),

    // ========== 网点管理模块（3xxxx） ==========
    BRANCH_NOT_FOUND(30001, "网点不存在"),
    BRANCH_CLOSED(30002, "网点已关门"),

    // ========== 预填单模块（4xxxx） ==========
    NLP_PARSE_FAIL(40001, "AI解析失败，请检查输入内容"),
    AUDIT_LOG_NOT_FOUND(40002, "审计记录不存在"),
    SIGNATURE_UPLOAD_FAIL(40003, "签名上传失败"),
    PDF_GENERATE_FAIL(40004, "PDF生成失败"),

    // ========== 审计模块（5xxxx） ==========
    AUDIT_BLOCKED(50001, "审计拦截，请完善材料后重试"),
    AUDIT_HASH_ERROR(50002, "审计链校验失败"),

    // ========== AI模块（6xxxx） ==========
    AI_SERVICE_ERROR(60001, "AI服务异常"),
    AI_TIMEOUT(60002, "AI服务超时"),
    AI_PRECHECK_REJECTED(60003, "AI预检未通过，材料不完整"),
    AI_PRECHECK_UNKNOWN_BUSINESS(60004, "未知业务类型"),

    // ========== 系统（9xxxx） ==========
    SYSTEM_ERROR(90000, "系统繁忙，请稍后重试"),
    REMOTE_CALL_FAIL(90001, "远程服务调用失败");

    private final Integer code;
    private final String msg;

    ResultCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
