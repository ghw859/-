package com.icbc.lingmou.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果
 * 所有接口统一返回此格式：{ code, msg, data }
 *
 * code = 0 表示成功，其他表示错误码
 */
@Data
@NoArgsConstructor
public class Result<T> {

    /** 状态码：0=成功，其他=错误码 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 实际数据 */
    private T data;

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg("success");
        return r;
    }

    /**
     * 成功返回（自定义提示）
     */
    public static <T> Result<T> success(String msg, T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    /**
     * 失败返回（带数据）
     */
    public static <T> Result<T> error(Integer code, String msg, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
