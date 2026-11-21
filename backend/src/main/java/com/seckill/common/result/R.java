package com.seckill.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * code == HTTP 状态码：200 成功 / 4xx 客户端问题 / 5xx 服务端问题
 * bizCode 是可选的业务细分码，例如：
 *   HTTP 409 + bizCode 1001 表示售罄
 *   HTTP 409 + bizCode 1002 表示重复购买
 *   HTTP 409 + bizCode 1003 表示活动未开始/已结束
 *
 * 成功示例：{ "code": 200, "message": "ok", "data": ..., "timestamp": ... }
 * 冲突示例：{ "code": 409, "bizCode": 1002, "message": "您已参与过该活动", ... }
 */
@Data
public class R<T> implements Serializable {

    private int code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer bizCode;

    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS);
        r.setMessage("ok");
        r.setData(data);
        return r;
    }

    public static <T> R<T> ok(String message, T data) {
        R<T> r = ok(data);
        r.setMessage(message);
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(ResultCode.ERROR, null, message);
    }

    public static <T> R<T> fail(int code, String message) {
        return fail(code, null, message);
    }

    public static <T> R<T> fail(int code, Integer bizCode, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setBizCode(bizCode);
        r.setMessage(message);
        return r;
    }
}
