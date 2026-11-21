package com.seckill.job;

import com.seckill.seckill.constant.SeckillKeys;
import com.seckill.seckill.entity.SeckillOrder;
import com.seckill.seckill.service.SeckillActivityService;
import com.seckill.seckill.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务：
 * 1. ZSET 延迟队列关单（10 秒一次，精度高）
 * 2. DB 兜底扫描（90 秒一次，防 Redis 延迟消息丢失）
 * 3. 活动到期自动结束（60 秒一次）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final SeckillOrderService orderService;
    private final SeckillActivityService activityService;
    private final StringRedisTemplate redis;

    @Scheduled(fixedDelay = 10_000, initialDelay = 15_000)
    public void closeTimeoutOrders() {
        List<String> dueOrders = orderService.dueOrderNos(50);
        for (String orderNo : dueOrders) {
            try {
                boolean cancelled = orderService.cancelIfUnpaid(orderNo, "支付超时");
                if (!cancelled) {
                    // 已支付/已取消的订单，直接从延迟队列移除
                    orderService.removeFromCloseQueue(orderNo);
                }
            } catch (Exception e) {
                log.error("关闭超时订单失败 orderNo={}", orderNo, e);
            }
        }
    }

    /**
     * 兜底扫描：即使 Redis ZSET 丢了消息，超过支付时间的待支付订单也会被关闭
     */
    @Scheduled(fixedDelay = 90_000, initialDelay = 60_000)
    public void closeTimeoutOrdersFromDb() {
        List<SeckillOrder> expired = orderService.findExpiredUnpaid(100);
        for (SeckillOrder order : expired) {
            try {
                orderService.cancelIfUnpaid(order.getOrderNo(), "支付超时(DB兜底)");
            } catch (Exception e) {
                log.error("兜底关闭订单失败 orderNo={}", order.getOrderNo(), e);
            }
        }
    }

    /**
     * 裁剪订单 Stream：ACK 只移出 pending，不删除消息。
     * 超过 2 万条时保留最近 1 万条（近似裁剪），防止长期运行内存膨胀。
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 300_000)
    public void trimOrderStream() {
        try {
            Long size = redis.opsForStream().size(SeckillKeys.ORDER_STREAM);
            if (size != null && size > 20_000) {
                Long trimmed = redis.opsForStream().trim(SeckillKeys.ORDER_STREAM, 10_000, true);
                log.info("订单 Stream 裁剪完成，size={}，trimmed={}", size, trimmed);
            }
        } catch (Exception e) {
            log.debug("订单 Stream 裁剪跳过: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeExpiredActivities() {
        try {
            int count = activityService.closeExpiredActivities();
            if (count > 0) {
                log.info("自动结束过期秒杀活动 {} 个", count);
            }
        } catch (Exception e) {
            log.error("自动结束活动失败", e);
        }
    }
}
