package com.seckill.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis 限流注解（固定窗口计数）
 *
 * 示例：@RateLimit(key = "seckill", count = 5, seconds = 10)
 * 表示同一用户 10 秒内最多请求 5 次。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 业务标识，最终 key = rate:{key}:{userId|ip} */
    String key();

    /** 窗口内最大请求数 */
    int count() default 10;

    /** 窗口大小（秒） */
    int seconds() default 1;

    /** 限流维度：USER 优先按用户，未登录按 IP */
    Dimension dimension() default Dimension.USER;

    enum Dimension {
        USER, IP
    }
}
