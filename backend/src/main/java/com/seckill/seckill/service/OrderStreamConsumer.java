package com.seckill.seckill.service;

import com.seckill.common.exception.BizException;
import com.seckill.common.util.RedisService;
import com.seckill.seckill.constant.SeckillKeys;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Stream 消费者：异步削峰的核心
 *
 * 为什么不用同步写库：
 * 秒杀瞬时 QPS 远高于数据库承受能力，Lua 扣完库存后把"创建订单"丢进消息队列，
 * 接口立刻返回，数据库按自己的节奏消费。Redis Stream 自带消费组 + ACK + pending，
 * 不引入 MQ 也能讲清楚"消息不丢"的机制。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStreamConsumer {

    private final StringRedisTemplate redis;
    private final SeckillOrderService orderService;
    private final SeckillStockService stockService;
    private final RedisService redisService;

    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Object processLock = new Object();
    private volatile boolean running = true;
    private Thread worker;

    @PostConstruct
    public void start() {
        worker = new Thread(this::loop, "seckill-order-consumer");
        worker.setDaemon(true);
        worker.start();
        log.info("订单 Stream 消费者已启动");
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void loop() {
        while (running) {
            try {
                List<MapRecord<String, String, String>> records = readNew();
                if (records != null && !records.isEmpty()) {
                    synchronized (processLock) {
                        records.forEach(this::handle);
                    }
                }
            } catch (Exception e) {
                // 消费组还没创建的常见原因是流还不存在，创建后重试即可
                ensureGroup();
                log.debug("读取订单 Stream 失败: {}", e.getMessage());
                sleep(1000);
            }
        }
    }

    private List<MapRecord<String, String, String>> readNew() {
        StreamOperations<String, String, String> ops = redis.<String, String>opsForStream();
        return ops.read(
                Consumer.from(SeckillKeys.ORDER_GROUP, SeckillKeys.ORDER_CONSUMER),
                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                StreamOffset.create(SeckillKeys.ORDER_STREAM, ReadOffset.lastConsumed()));
    }

    private void ensureGroup() {
        try {
            redis.opsForStream().createGroup(SeckillKeys.ORDER_STREAM,
                    ReadOffset.from("0"), SeckillKeys.ORDER_GROUP);
            log.info("订单 Stream 消费组创建成功");
        } catch (Exception e) {
            // BUSYGROUP: 组已存在；NOGROUP/流不存在：等有订单再创建
            log.debug("消费组创建跳过: {}", e.getMessage());
        }
    }

    private void handle(MapRecord<String, String, String> record) {
        Map<String, String> message = record.getValue();
        String orderNo = message.get("orderNo");
        String recordId = record.getId().getValue();
        try {
            orderService.createFromMessage(message);
            acknowledge(record);
            attempts.remove(recordId);
        } catch (DuplicateOrderException e) {
            rollbackAndFail(message, e.getMessage());
            acknowledge(record);
            attempts.remove(recordId);
        } catch (DuplicateKeyException e) {
            // 先判断是不是同一条消息重复消费（orderNo 已存在），这种情况不能回滚库存
            if (orderService.existsByOrderNo(orderNo)) {
                acknowledge(record);
            } else {
                rollbackAndFail(message, "您已参与过该活动");
                acknowledge(record);
            }
            attempts.remove(recordId);
        } catch (BizException e) {
            rollbackAndFail(message, e.getMessage());
            acknowledge(record);
            attempts.remove(recordId);
        } catch (Exception e) {
            int count = attempts.merge(recordId, 1, Integer::sum);
            log.warn("订单处理失败（第 {} 次），保留 pending 等待重试 orderNo={}", count, orderNo, e);
            if (count >= 5) {
                rollbackAndFail(message, "系统繁忙，下单失败");
                acknowledge(record);
                attempts.remove(recordId);
            }
        }
    }

    /**
     * 定时重试 pending 消息：模拟 MQ 的失败重投
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void retryPending() {
        synchronized (processLock) {
            try {
                List<MapRecord<String, String, String>> pending = redis.<String, String>opsForStream().read(
                        Consumer.from(SeckillKeys.ORDER_GROUP, SeckillKeys.ORDER_CONSUMER),
                        StreamReadOptions.empty().count(20),
                        StreamOffset.create(SeckillKeys.ORDER_STREAM, ReadOffset.from("0")));
                if (pending != null && !pending.isEmpty()) {
                    log.info("重试 pending 订单消息 {} 条", pending.size());
                    pending.forEach(this::handle);
                }
            } catch (Exception e) {
                log.debug("重试 pending 消息失败: {}", e.getMessage());
            }
        }
    }

    private void acknowledge(MapRecord<String, String, String> record) {
        try {
            redis.opsForStream().acknowledge(SeckillKeys.ORDER_STREAM,
                    SeckillKeys.ORDER_GROUP, record.getId());
        } catch (Exception e) {
            log.warn("ACK 失败 recordId={}", record.getId(), e);
        }
    }

    private void rollbackAndFail(Map<String, String> message, String reason) {
        try {
            stockService.rollback(Long.valueOf(message.get("activityId")),
                    Long.valueOf(message.get("userId")),
                    Integer.parseInt(message.getOrDefault("quantity", "1")));
        } catch (Exception e) {
            log.error("回滚 Redis 库存失败 message={}", message, e);
        }
        try {
            String orderNo = message.get("orderNo");
            if (orderNo != null) {
                redisService.set(SeckillKeys.ORDER_FAIL + orderNo, reason, Duration.ofHours(1));
            }
        } catch (Exception e) {
            log.error("记录失败结果失败", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
