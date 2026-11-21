package com.seckill.common.exception;

import com.seckill.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：可预期的问题统一抛它，由 GlobalExceptionHandler 转成 R.fail
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(ResultCode.ERROR, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException of(int code, String message) {
        return new BizException(code, message);
    }

    public static BizException badRequest(String message) {
        return new BizException(ResultCode.BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(ResultCode.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ResultCode.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ResultCode.NOT_FOUND, message);
    }
}
