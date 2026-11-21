package com.seckill.seckill.service;

import com.seckill.common.util.RedisService;
import com.seckill.seckill.constant.SeckillKeys;
import com.seckill.seckill.entity.SeckillActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 库存操作：所有"读-改-写"都放进 Lua，保证原子性。
 *
 * 不用 DECR 再判断的原因：DECR 后库存可能变负数，回补逻辑复杂且并发下仍会多卖。
 * Lua 在 Redis 单线程内一次执行完"判断是否重复 + 判断库存 + 扣减 + 记录用户"。
 */
@Service
@RequiredArgsConstructor
public class SeckillStockService {

    private static final RedisScript<Long> SECKILL_SCRIPT =
            RedisScript.of(new ClassPathResource("lua/seckill.lua"), Long.class);
    private static final RedisScript<Long> ROLLBACK_SCRIPT =
            RedisScript.of(new ClassPathResource("lua/rollback.lua"), Long.class);

    private final StringRedisTemplate redis;
    private final RedisService redisService;

    /** 上架/重新上架：用 DB 剩余库存重置 Redis，并清空已购用户集合 */
    public void reset(SeckillActivity activity) {
        redis.opsForValue().set(stockKey(activity.getId()), String.valueOf(activity.getStock()));
        redis.delete(userKey(activity.getId()));
    }

    /** 只补库存 key，不动已购用户集合（避免重新上架时清掉购买记录） */
    public void ensureStock(SeckillActivity activity) {
        redis.opsForValue().setIfAbsent(stockKey(activity.getId()), String.valueOf(activity.getStock()));
    }

    /**
     * @return >=0 剩余库存；-1 售罄；-2 已购买；-3 未初始化
     */
    public long tryDeduct(SeckillActivity activity, Long userId, int quantity) {
        ensureStock(activity);
        Long result = execute(activity.getId(), userId, quantity);
        if (result != null && result == -3) {
            ensureStock(activity);
            result = execute(activity.getId(), userId, quantity);
        }
        return result == null ? -3 : result;
    }

    private Long execute(Long activityId, Long userId, int quantity) {
        return redis.execute(SECKILL_SCRIPT,
                List.of(stockKey(activityId), userKey(activityId)),
                String.valueOf(userId), String.valueOf(quantity));
    }

    /** 下单失败 / 取消订单时回补 */
    public void rollback(Long activityId, Long userId, int quantity) {
        redis.execute(ROLLBACK_SCRIPT,
                List.of(stockKey(activityId), userKey(activityId)),
                String.valueOf(userId), String.valueOf(quantity));
    }

    public Integer remaining(Long activityId) {
        String value = redis.opsForValue().get(stockKey(activityId));
        return value == null ? null : Integer.valueOf(value);
    }

    /** 活动结束时清理，避免冷数据占内存 */
    public void clear(Long activityId) {
        redis.delete(List.of(stockKey(activityId), userKey(activityId)));
    }

    private String stockKey(Long activityId) {
        return SeckillKeys.STOCK + activityId;
    }

    private String userKey(Long activityId) {
        return SeckillKeys.USERS + activityId;
    }
}
