package com.seckill.common.aspect;

import com.seckill.common.annotation.RateLimit;
import com.seckill.common.context.UserContext;
import com.seckill.common.exception.BizException;
import com.seckill.common.result.ResultCode;
import com.seckill.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 限流切面：Redis 固定窗口计数
 *
 * 固定窗口实现简单，但窗口边界可能放过 2 倍流量；
 * 生产可用滑动窗口（ZSET）或令牌桶（Redisson RRateLimiter），这里保留最易懂的版本。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String PREFIX = "sk:rate:";

    private final StringRedisTemplate redis;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String identity;
        if (rateLimit.dimension() == RateLimit.Dimension.IP) {
            identity = "ip:" + currentIp();
        } else {
            Long userId = UserContext.getUserId();
            identity = userId != null ? "u:" + userId : "ip:" + currentIp();
        }

        // 时间分桶：同一个窗口内共享一个 key，过期后自动重置
        long bucket = System.currentTimeMillis() / 1000 / rateLimit.seconds();
        String key = PREFIX + rateLimit.key() + ":" + identity + ":" + bucket;

        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofSeconds(rateLimit.seconds() + 1L));
        }
        if (count != null && count > rateLimit.count()) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
        }
        return joinPoint.proceed();
    }

    private String currentIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        return IpUtil.getIp(request);
    }
}
