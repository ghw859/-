package com.icbc.lingmou.common;

/**
 * 错误码定义
 * 三个人共同遵守这套错误码体系
 */
public enum ResultCode {

    // ========== 通用 ==========
    SUCCESS(0, "成功"),
    PARAM_ERROR(1001, "参数错误"),
    UNAUTHORIZED(1002, "未登录或登录已过期"),
    FORBIDDEN(1003, "无权限"),
    NOT_FOUND(1004, "资源不存在"),

    // ========== 用户认证模块 ==========
    USER_NOT_EXIST(1101, "用户不存在"),
    PASSWORD_ERROR(1102, "密码错误"),
    USER_EXIST(1103, "用户名已存在"),
    PHONE_EXIST(1104, "手机号已注册"),
    ID_CARD_EXIST(1105, "身份证已注册"),
    SMS_CODE_ERROR(1106, "验证码错误或已过期"),
    SMS_CODE_SEND_FAIL(1107, "验证码发送失败"),
    TOKEN_INVALID(1108, "Token无效"),

    // ========== 预约管理模块 ==========
    APPOINTMENT_CONFLICT(2001, "该时段已在其他网点预约"),
    SLOT_FULL(2002, "该时段已约满"),
    APPOINTMENT_NOT_FOUND(2003, "预约不存在"),
    APPOINTMENT_EXPIRED(2004, "预约已过期"),
    APPOINTMENT_COMPLETED(2005, "预约已完成"),
    APPOINTMENT_CANCELLED(2006, "预约已取消"),
    VOUCHER_NUM_NOT_FOUND(2007, "凭证号不存在"),

    // ========== 网点管理模块 ==========
    BRANCH_NOT_FOUND(3001, "网点不存在"),
    BRANCH_CLOSED(3002, "网点已关门"),

    // ========== 预填单/审计模块 ==========
    NLP_PARSE_FAIL(4001, "AI解析失败，请检查输入内容"),
    AUDIT_LOG_NOT_FOUND(4002, "审计记录不存在"),
    SIGNATURE_UPLOAD_FAIL(4003, "签名上传失败"),
    PDF_GENERATE_FAIL(4004, "PDF生成失败"),

    // ========== AI模块 ==========
    AI_SERVICE_ERROR(5001, "AI服务异常"),
    AI_TIMEOUT(5002, "AI服务超时"),

    // ========== 系统 ==========
    SYSTEM_ERROR(5000, "系统繁忙，请稍后重试");

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
