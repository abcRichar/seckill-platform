package com.seckill.seed;

import com.seckill.seckill.service.SeckillActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动预热：把进行中活动的库存和隐藏链接加载进 Redis。
 * 放在 ApplicationReadyEvent，确保表结构和种子数据都已就绪。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarmupRunner {

    private final SeckillActivityService activityService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        try {
            activityService.warmUp();
        } catch (Exception e) {
            log.warn("秒杀活动预热失败，请检查 Redis 是否启动: {}", e.getMessage());
        }
    }
}
