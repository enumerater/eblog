package com.enumerate.common.core.exception;

import com.enumerate.common.core.result.ResultCode;

/**
 * 业务异常 — 统一业务层异常
 * 由 GlobalExceptionHandler 统一捕获并转换为 Result
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String detail) {
        super(resultCode.getMessage() + ": " + detail);
        this.code = resultCode.getCode();
    }

    public int getCode() {
        return code;
    }
}