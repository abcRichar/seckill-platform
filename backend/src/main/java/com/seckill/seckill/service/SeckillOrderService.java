package com.seckill.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.exception.BizException;
import com.seckill.common.util.RedisService;
import com.seckill.product.mapper.ProductMapper;
import com.seckill.seckill.constant.OrderStatus;
import com.seckill.seckill.constant.RefundStatus;
import com.seckill.seckill.constant.SeckillKeys;
import com.seckill.seckill.entity.SeckillOrder;
import com.seckill.seckill.mapper.SeckillActivityMapper;
import com.seckill.seckill.mapper.SeckillOrderMapper;
import com.seckill.seckill.vo.OrderExportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 订单服务：异步落库、支付、取消、超时关单、导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SeckillOrderMapper orderMapper;
    private final SeckillActivityMapper activityMapper;
    private final ProductMapper productMapper;
    private final SeckillStockService stockService;
    private final RedisService redisService;
    private final StringRedisTemplate redis;

    @Value("${seckill.order-timeout-minutes:15}")
    private int timeoutMinutes;

    /**
     * Stream 消费者调用：把秒杀订单真正写入数据库。
     *
     * 幂等设计：
     * - 同一 orderNo 重复消费：直接返回已有订单
     * - 同一用户同一活动重复下单：抛 DuplicateOrderException，消费者回滚 Redis
     * - DB 唯一索引 uk_user_activity 兜底并发
     */
    @Transactional
    public SeckillOrder createFromMessage(Map<String, String> message) {
        String orderNo = message.get("orderNo");
        Long userId = Long.valueOf(message.get("userId"));
        Long activityId = Long.valueOf(message.get("activityId"));
        Long productId = Long.valueOf(message.get("productId"));
        int quantity = Integer.parseInt(message.getOrDefault("quantity", "1"));
        BigDecimal price = new BigDecimal(message.getOrDefault("seckillPrice", "0"));

        SeckillOrder exists = orderMapper.selectByOrderNo(orderNo);
        if (exists != null) {
            return exists;
        }
        SeckillOrder old = orderMapper.selectByUserAndActivity(userId, activityId);
        if (old != null) {
            throw new DuplicateOrderException("您已参与过该活动");
        }

        // DB 库存再兜底一次：Redis 和 DB 不一致时也不能超卖
        int rows = activityMapper.deductStock(activityId, quantity);
        if (rows == 0) {
            throw new BizException(com.seckill.common.result.ResultCode.SOLD_OUT, "库存不足，秒杀失败");
        }

        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setProductId(productId);
        order.setProductName(message.getOrDefault("productName", ""));
        order.setProductCover(message.getOrDefault("productCover", ""));
        order.setSeckillPrice(price);
        order.setQuantity(quantity);
        order.setStatus(OrderStatus.UNPAID);
        orderMapper.insert(order);

        // 事务提交后再写延迟队列；即使这里失败，兜底的 DB 扫描任务也会关闭超时订单
        long closeAt = System.currentTimeMillis() + timeoutMinutes * 60_000L;
        afterCommit(() -> redis.opsForZSet().add(SeckillKeys.CLOSE_QUEUE, orderNo, closeAt));
        log.info("秒杀订单落库成功 orderNo={}, userId={}, activityId={}", orderNo, userId, activityId);
        return order;
    }

    public boolean existsByOrderNo(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo) != null;
    }

    // ==================== 用户端 ====================

    public Page<SeckillOrder> pageMine(Long userId, Integer status, long current, long size) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getUserId, userId);
        wrapper.eq(status != null, SeckillOrder::getStatus, status);
        wrapper.orderByDesc(SeckillOrder::getId);
        return orderMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public SeckillOrder getOwned(Long id, Long userId) {
        SeckillOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw BizException.forbidden("无权查看该订单");
        }
        return order;
    }

    /**
     * 模拟支付：条件更新保证重复回调只生效一次，后续调用直接返回成功（幂等）
     */
    @Transactional
    public SeckillOrder pay(Long id, Long userId) {
        SeckillOrder order = getOwned(id, userId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw BizException.badRequest("订单已取消，无法支付");
        }
        if (order.getStatus() == OrderStatus.REFUNDED) {
            throw BizException.badRequest("订单已退款，无需支付");
        }
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.FINISHED) {
            return order;
        }
        int rows = orderMapper.pay(id);
        if (rows == 0) {
            SeckillOrder latest = orderMapper.selectById(id);
            if (latest != null && latest.getStatus() != OrderStatus.UNPAID) {
                return latest;
            }
            throw new BizException(com.seckill.common.result.ResultCode.CONFLICT, "支付失败，请刷新后重试");
        }
        productMapper.addSales(order.getProductId(), order.getQuantity());
        afterCommit(() -> redis.opsForZSet().remove(SeckillKeys.CLOSE_QUEUE, order.getOrderNo()));
        log.info("订单支付成功 orderNo={}", order.getOrderNo());
        return orderMapper.selectById(id);
    }

    @Transactional
    public void cancelByUser(Long id, Long userId) {
        SeckillOrder order = getOwned(id, userId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw BizException.badRequest("订单已取消");
        }
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw BizException.badRequest("已支付订单不能取消");
        }
        if (!cancelInternal(order)) {
            throw BizException.badRequest("订单状态已变更，请刷新后重试");
        }
    }

    // ==================== 管理端 ====================

    public Page<SeckillOrder> pageAdmin(long current, long size, String keyword, Integer status,
                                        Long activityId, String startDate, String endDate) {
        return orderMapper.selectPage(new Page<>(current, size),
                buildAdminWrapper(keyword, status, activityId, startDate, endDate));
    }

    public SeckillOrder adminDetail(Long id) {
        SeckillOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        return order;
    }

    @Transactional
    public void adminCancel(Long id) {
        SeckillOrder order = adminDetail(id);
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw BizException.badRequest("只有待支付订单可以取消");
        }
        if (!cancelInternal(order)) {
            throw BizException.badRequest("订单状态已变更，请刷新后重试");
        }
    }

    public List<OrderExportVO> listForExport(String keyword, Integer status, Long activityId,
                                             String startDate, String endDate) {
        List<SeckillOrder> orders = orderMapper.selectList(
                buildAdminWrapper(keyword, status, activityId, startDate, endDate)
                        .orderByAsc(SeckillOrder::getId)
                        .last("LIMIT 5000"));
        return orders.stream().map(order -> {
            OrderExportVO vo = new OrderExportVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setUserId(String.valueOf(order.getUserId()));
            vo.setProductName(order.getProductName());
            vo.setQuantity(order.getQuantity());
            vo.setSeckillPrice(order.getSeckillPrice().toPlainString());
            vo.setStatus(OrderStatus.text(order.getStatus()));
            vo.setRefundStatus(RefundStatus.orderText(order.getRefundStatus()));
            vo.setCreateTime(format(order.getCreateTime()));
            vo.setPayTime(format(order.getPayTime()));
            return vo;
        }).toList();
    }

    // ==================== 定时任务 ====================

    /**
     * 延迟队列到期订单
     */
    public List<String> dueOrderNos(int limit) {
        var set = redis.opsForZSet().rangeByScore(SeckillKeys.CLOSE_QUEUE, 0,
                System.currentTimeMillis(), 0, limit);
        return set == null ? List.of() : List.copyOf(set);
    }

    public void removeFromCloseQueue(String orderNo) {
        redis.opsForZSet().remove(SeckillKeys.CLOSE_QUEUE, orderNo);
    }

    /**
     * 兜底：直接扫 DB 找超时未支付订单（防止 Redis 丢失延迟消息）
     */
    public List<SeckillOrder> findExpiredUnpaid(int limit) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return orderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getStatus, OrderStatus.UNPAID)
                .lt(SeckillOrder::getCreateTime, deadline)
                .orderByAsc(SeckillOrder::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 关闭订单（独立事务，由定时任务调用）
     */
    @Transactional
    public boolean cancelIfUnpaid(String orderNo, String reason) {
        SeckillOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || order.getStatus() != OrderStatus.UNPAID) {
            return false;
        }
        boolean cancelled = cancelInternal(order);
        if (cancelled) {
            log.info("订单已关闭 {}({})", orderNo, reason);
        }
        return cancelled;
    }

    // ==================== 内部 ====================

    private boolean cancelInternal(SeckillOrder order) {
        int rows = orderMapper.cancel(order.getOrderNo());
        if (rows == 0) {
            return false;
        }
        activityMapper.restoreStock(order.getActivityId(), order.getQuantity());
        // DB 提交后再回补 Redis，避免事务回滚但 Redis 已加库存
        afterCommit(() -> {
            stockService.rollback(order.getActivityId(), order.getUserId(), order.getQuantity());
            redis.opsForZSet().remove(SeckillKeys.CLOSE_QUEUE, order.getOrderNo());
        });
        return true;
    }

    private LambdaQueryWrapper<SeckillOrder> buildAdminWrapper(String keyword, Integer status,
                                                                Long activityId, String startDate,
                                                                String endDate) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SeckillOrder::getOrderNo, keyword)
                    .or().like(SeckillOrder::getProductName, keyword));
        }
        wrapper.eq(status != null, SeckillOrder::getStatus, status);
        wrapper.eq(activityId != null, SeckillOrder::getActivityId, activityId);
        wrapper.ge(StringUtils.hasText(startDate), SeckillOrder::getCreateTime, startDate + " 00:00:00");
        wrapper.le(StringUtils.hasText(endDate), SeckillOrder::getCreateTime, endDate + " 23:59:59");
        return wrapper;
    }

    private void afterCommit(Runnable action) {
        Runnable safeAction = () -> {
            try {
                action.run();
            } catch (Exception e) {
                log.error("事务提交后 Redis 操作失败", e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeAction.run();
                }
            });
        } else {
            safeAction.run();
        }
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }
}
