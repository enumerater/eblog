package com.enumerate.common.core.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应格式
 * 所有服务统一使用此格式返回数据
 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private Long timestamp;

    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    // ─────────────────── 成功 ───────────────────

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> Result<T> success(T data, String message) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = message;
        r.data = data;
        return r;
    }

    // ─────────────────── 失败 ───────────────────

    public static <T> Result<T> fail(String message) {
        return fail(ResultCode.FAIL.getCode(), message);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }

    // ─────────────────── 便捷方法 ───────────────────

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}