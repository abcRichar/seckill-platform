package com.seckill.seckill.service;

import com.seckill.common.context.UserContext;
import com.seckill.common.exception.BizException;
import com.seckill.common.result.ResultCode;
import com.seckill.common.util.RedisService;
import com.seckill.product.entity.Product;
import com.seckill.product.mapper.ProductMapper;
import com.seckill.seckill.constant.SeckillKeys;
import com.seckill.seckill.dto.SeckillOrderRequestDTO;
import com.seckill.seckill.entity.SeckillActivity;
import com.seckill.seckill.entity.SeckillOrder;
import com.seckill.seckill.mapper.SeckillOrderMapper;
import com.seckill.seckill.vo.SeckillResultVO;
import com.seckill.seckill.vo.SeckillTokenVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户端秒杀入口
 *
 * 完整链路：
 * 1. 校验活动状态 + 隐藏链接
 * 2. 换取一次性秒杀令牌（限流）
 * 3. Lua 原子预扣 Redis 库存 + 一人一单
 * 4. 写 Redis Stream 后立即返回"排队中"（异步削峰）
 * 5. 消费者落库，前端轮询结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final long TOKEN_TTL_SECONDS = 60;

    private final SeckillActivityService activityService;
    private final SeckillStockService stockService;
    private final SeckillOrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final RedisService redisService;
    private final StringRedisTemplate redis;

    /**
     * 换取秒杀令牌：把"是否重复购买/活动状态"等校验提前，真正下单接口只做最轻的事。
     */
    public SeckillTokenVO getToken(Long activityId, String path) {
        Long userId = UserContext.requireUserId();
        SeckillActivity activity = activityService.getEntity(activityId);
        checkOnGoing(activity);
        checkPath(activity, path);

        SeckillOrder old = orderMapper.selectByUserAndActivity(userId, activityId);
        if (old != null) {
            throw new BizException(ResultCode.REPEAT_BUY, "您已参与过该活动");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = SeckillKeys.TOKEN + userId + ":" + activityId;
        redisService.set(tokenKey, token, Duration.ofSeconds(TOKEN_TTL_SECONDS));
        return new SeckillTokenVO(activityId, token, TOKEN_TTL_SECONDS);
    }

    /**
     * 秒杀下单：Redis 层面完成"是否售罄/是否重复"，不碰数据库，扛住瞬时流量。
     */
    public SeckillResultVO seckill(SeckillOrderRequestDTO dto) {
        Long userId = UserContext.requireUserId();
        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        if (quantity != 1) {
            throw BizException.badRequest("当前活动每人限购 1 件");
        }

        // 1. 令牌一次性使用：先校验再删除，防脚本重放
        String tokenKey = SeckillKeys.TOKEN + userId + ":" + dto.getActivityId();
        String cacheToken = redisService.getString(tokenKey);
        if (!StringUtils.hasText(cacheToken) || !cacheToken.equals(dto.getToken())) {
            throw BizException.badRequest("秒杀令牌无效或已过期，请重新获取");
        }
        redis.delete(tokenKey);

        // 2. 活动状态 + 隐藏链接
        SeckillActivity activity = activityService.getEntity(dto.getActivityId());
        checkOnGoing(activity);
        checkPath(activity, dto.getPath());

        // 3. Lua 原子预扣
        long result = stockService.tryDeduct(activity, userId, quantity);
        if (result == -2) {
            throw new BizException(ResultCode.REPEAT_BUY, "您已参与过该活动");
        }
        if (result == -1) {
            throw new BizException(ResultCode.SOLD_OUT, "手慢了，商品已售罄");
        }
        if (result == -3) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "库存尚未初始化，请稍后重试");
        }

        // 4. 写 Stream，异步落库
        String orderNo = generateOrderNo(userId);
        try {
            sendOrderMessage(orderNo, userId, activity, quantity);
        } catch (Exception e) {
            stockService.rollback(activity.getId(), userId, quantity);
            log.error("写入订单 Stream 失败，已回滚 Redis 库存 orderNo={}", orderNo, e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "下单失败，请稍后重试");
        }
        redisService.set(SeckillKeys.ORDER_OWNER + orderNo, String.valueOf(userId), Duration.ofHours(1));
        log.info("秒杀预扣成功 userId={}, activityId={}, orderNo={}, remaining={}",
                userId, activity.getId(), orderNo, result);
        return SeckillResultVO.queued(orderNo);
    }

    /**
     * 当前用户在该活动的参与状态，前端详情页用来禁用按钮
     */
    public Map<String, Object> myActivityStatus(Long activityId) {
        Long userId = UserContext.requireUserId();
        SeckillOrder order = orderMapper.selectByUserAndActivity(userId, activityId);
        if (order == null) {
            return Map.of("purchased", false);
        }
        return Map.of(
                "purchased", true,
                "orderId", order.getId(),
                "orderStatus", order.getStatus());
    }

    /**
     * 前端轮询下单结果：先查订单表，未落库再查失败标记，否则就是排队中
     */
    public SeckillResultVO result(String orderNo) {
        Long userId = UserContext.requireUserId();
        SeckillOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order != null) {
            if (!order.getUserId().equals(userId) && !UserContext.require().isAdmin()) {
                throw BizException.forbidden("无权查看该订单");
            }
            return new SeckillResultVO(orderNo, "SUCCESS", order.getId(), order.getStatus(), "订单创建成功");
        }
        String owner = redisService.getString(SeckillKeys.ORDER_OWNER + orderNo);
        if (owner == null) {
            throw BizException.notFound("订单不存在或已过期");
        }
        if (!owner.equals(String.valueOf(userId))) {
            throw BizException.forbidden("无权查看该订单");
        }
        String failReason = redisService.getString(SeckillKeys.ORDER_FAIL + orderNo);
        if (failReason != null) {
            return new SeckillResultVO(orderNo, "FAILED", null, null, failReason);
        }
        return SeckillResultVO.queued(orderNo);
    }

    private void sendOrderMessage(String orderNo, Long userId, SeckillActivity activity, int quantity) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("orderNo", orderNo);
        message.put("userId", String.valueOf(userId));
        message.put("activityId", String.valueOf(activity.getId()));
        message.put("productId", String.valueOf(activity.getProductId()));
        message.put("productName", activity.getTitle());
        // 订单里要展示商品快照，只在下单成功后查一次商品表
        Product product = productMapper.selectById(activity.getProductId());
        message.put("productCover", product == null || product.getCover() == null ? "" : product.getCover());
        message.put("seckillPrice", activity.getSeckillPrice().toPlainString());
        message.put("quantity", String.valueOf(quantity));
        // 注意：ACK 不会删除消息，Stream 会持续增长，由 ScheduledTasks.trimOrderStream 定时裁剪
        RecordId recordId = redis.<String, String>opsForStream().add(SeckillKeys.ORDER_STREAM, message);
        if (recordId == null) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "写入订单队列失败");
        }
    }

    private void checkOnGoing(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            throw new BizException(ResultCode.ACTIVITY_CLOSED, "活动未上架");
        }
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new BizException(ResultCode.ACTIVITY_CLOSED, "活动尚未开始");
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new BizException(ResultCode.ACTIVITY_CLOSED, "活动已结束");
        }
    }

    private void checkPath(SeckillActivity activity, String path) {
        if (!StringUtils.hasText(path) || !path.equals(activity.getSeckillPath())) {
            throw BizException.badRequest("秒杀链接无效");
        }
    }

    private String generateOrderNo(Long userId) {
        String suffix = String.format("%04d", userId % 10000);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "SK" + LocalDateTime.now().format(ORDER_NO_FORMAT) + suffix + random;
    }
}
