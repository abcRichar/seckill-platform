package com.seckill.common.result;

/**
 * 状态码约定
 *
 * HTTP 状态码与 body.code 保持一致：2xx 成功 / 4xx 客户端问题 / 5xx 服务端异常。
 * body.bizCode 只在需要区分业务细分时出现（售罄、重复购买、活动结束）。
 */
public final class ResultCode {

    private ResultCode() {
    }

    /** 成功（HTTP 200） */
    public static final int SUCCESS = 200;
    /** 参数/业务校验失败（HTTP 400） */
    public static final int BAD_REQUEST = 400;
    /** 未登录 / token 失效（HTTP 401） */
    public static final int UNAUTHORIZED = 401;
    /** 无权限（HTTP 403） */
    public static final int FORBIDDEN = 403;
    /** 资源不存在（HTTP 404） */
    public static final int NOT_FOUND = 404;
    /** 请求方法不支持（HTTP 405） */
    public static final int METHOD_NOT_ALLOWED = 405;
    /** 资源状态冲突（HTTP 409） */
    public static final int CONFLICT = 409;
    /** 上传文件过大（HTTP 413） */
    public static final int PAYLOAD_TOO_LARGE = 413;
    /** 请求过于频繁（HTTP 429） */
    public static final int TOO_MANY_REQUESTS = 429;
    /** 服务端异常（HTTP 500） */
    public static final int ERROR = 500;
    /** 服务暂时不可用（HTTP 503） */
    public static final int SERVICE_UNAVAILABLE = 503;

    // ==================== 业务细分码（bizCode） ====================
    /** 秒杀售罄：HTTP 409 + bizCode 1001 */
    public static final int SOLD_OUT = 1001;
    /** 重复购买：HTTP 409 + bizCode 1002 */
    public static final int REPEAT_BUY = 1002;
    /** 活动未开始/已结束：HTTP 409 + bizCode 1003 */
    public static final int ACTIVITY_CLOSED = 1003;
}
