package com.icbc.lingmou.common;

import lombok.Getter;

/**
 * 业务异常
 * 用于在业务逻辑中主动抛出，全局异常处理器会捕获并返回给前端
 *
 * 使用方式：
 *   throw new BusinessException(20001, "该时段已约满");
 *   throw new BusinessException(ResultCode.APPOINTMENT_CONFLICT);
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 接收ResultCode构造
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 常用错误码快捷方法
     */
    public static BusinessException paramError(String msg) {
        return new BusinessException(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    public static BusinessException unauthorized(String msg) {
        return new BusinessException(ResultCode.UNAUTHORIZED.getCode(), msg);
    }

    public static BusinessException forbidden(String msg) {
        return new BusinessException(ResultCode.FORBIDDEN.getCode(), msg);
    }

    public static BusinessException notFound(String msg) {
        return new BusinessException(ResultCode.NOT_FOUND.getCode(), msg);
    }

    public static BusinessException conflict(String msg) {
        return new BusinessException(ResultCode.APPOINTMENT_CONFLICT.getCode(), msg);
    }

    public static BusinessException slotFull(String msg) {
        return new BusinessException(ResultCode.SLOT_FULL.getCode(), msg);
    }

    public static BusinessException systemError(String msg) {
        return new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), msg);
    }
}
