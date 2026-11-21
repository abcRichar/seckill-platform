package com.seckill.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.seckill.common.exception.BizException;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.ResultCode;
import com.seckill.common.util.RedisService;
import com.seckill.product.entity.Product;
import com.seckill.product.mapper.ProductMapper;
import com.seckill.seckill.constant.SeckillKeys;
import com.seckill.seckill.entity.SeckillActivity;
import com.seckill.seckill.entity.SeckillOrder;
import com.seckill.seckill.mapper.SeckillActivityMapper;
import com.seckill.seckill.mapper.SeckillOrderMapper;
import com.seckill.seckill.vo.SeckillActivityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 秒杀活动管理 + 用户端展示
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillActivityService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillOrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final SeckillStockService stockService;
    private final RedisService redisService;

    // ==================== 管理端 ====================

    public PageResult<SeckillActivityVO> pageAdmin(long current, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), SeckillActivity::getTitle, keyword);
        wrapper.eq(status != null, SeckillActivity::getStatus, status);
        wrapper.orderByDesc(SeckillActivity::getId);
        Page<SeckillActivity> page = activityMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, Product> products = loadProducts(page.getRecords());
        List<SeckillActivityVO> records = page.getRecords().stream()
                .map(a -> toVO(a, products.get(a.getProductId()), false))
                .toList();
        return PageResult.of(page, records);
    }

    public SeckillActivityVO getAdmin(Long id) {
        SeckillActivity activity = getEntity(id);
        Product product = productMapper.selectById(activity.getProductId());
        SeckillActivityVO vo = toVO(activity, product, false);
        vo.setState(calculateState(activity, LocalDateTime.now()));
        return vo;
    }

    @Transactional
    public void create(SeckillActivity activity) {
        validate(activity);
        productMapper.selectById(activity.getProductId());
        activity.setId(null);
        activity.setStatus(0);
        activity.setInitialStock(activity.getStock());
        if (activity.getLimitPerUser() == null) {
            activity.setLimitPerUser(1);
        }
        activity.setSeckillPath(null);
        activityMapper.insert(activity);
        evictListCache();
    }

    @Transactional
    public void update(Long id, SeckillActivity activity) {
        SeckillActivity old = getEntity(id);
        if (old.getStatus() != null && old.getStatus() == 1) {
            throw BizException.badRequest("活动进行中，请先下架再编辑");
        }
        validate(activity);
        if (productMapper.selectById(activity.getProductId()) == null) {
            throw BizException.badRequest("商品不存在");
        }
        activity.setId(id);
        activity.setInitialStock(activity.getStock());
        activityMapper.updateById(activity);
        evict(id);
    }

    @Transactional
    public void delete(Long id) {
        SeckillActivity activity = getEntity(id);
        if (activity.getStatus() != null && activity.getStatus() == 1) {
            throw BizException.badRequest("活动进行中，无法删除");
        }
        Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getActivityId, id));
        if (orderCount > 0) {
            throw BizException.badRequest("该活动已有 " + orderCount + " 笔订单，无法删除");
        }
        activityMapper.deleteById(id);
        evict(id);
        redisService.delete(SeckillKeys.PATH + id);
    }

    /**
     * 上架：生成隐藏链接 + 用 DB 剩余库存初始化 Redis + 清空已购集合
     */
    @Transactional
    public void publish(Long id) {
        SeckillActivity activity = getEntity(id);
        LocalDateTime now = LocalDateTime.now();
        if (activity.getEndTime() == null || activity.getEndTime().isBefore(now)) {
            throw BizException.badRequest("活动结束时间已过，请先修改时间");
        }
        if (activity.getStock() == null || activity.getStock() <= 0) {
            throw BizException.badRequest("剩余库存为 0，无法上架");
        }
        if (!StringUtils.hasText(activity.getSeckillPath())) {
            activity.setSeckillPath(UUID.randomUUID().toString().replace("-", ""));
        }
        activity.setStatus(1);
        activityMapper.updateById(activity);
        stockService.reset(activity);
        redisService.set(SeckillKeys.PATH + id, activity.getSeckillPath(), Duration.ofDays(30));
        evict(id);
        log.info("秒杀活动已上架 id={}, stock={}, path={}", id, activity.getStock(), activity.getSeckillPath());
    }

    @Transactional
    public void stop(Long id) {
        SeckillActivity activity = getEntity(id);
        activity.setStatus(2);
        activityMapper.updateById(activity);
        stockService.clear(id);
        redisService.delete(SeckillKeys.PATH + id);
        evict(id);
        log.info("秒杀活动已结束 id={}", id);
    }

    /**
     * 定时任务：把过了结束时间的活动自动置为结束，并清理 Redis
     */
    @Transactional
    public int closeExpiredActivities() {
        List<SeckillActivity> expired = activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, 1)
                .lt(SeckillActivity::getEndTime, LocalDateTime.now()));
        int count = 0;
        for (SeckillActivity activity : expired) {
            activity.setStatus(2);
            activityMapper.updateById(activity);
            stockService.clear(activity.getId());
            redisService.delete(SeckillKeys.PATH + activity.getId());
            count++;
        }
        if (count > 0) {
            evictListCache();
        }
        return count;
    }

    // ==================== 用户端 ====================

    /**
     * 活动列表：缓存 15~25 秒。库存每次读取时从 Redis 实时覆盖，避免缓存里的库存过期。
     */
    public List<SeckillActivityVO> listMall() {
        List<SeckillActivityVO> cached = redisService.get(SeckillKeys.ACTIVITY_LIST,
                new TypeReference<List<SeckillActivityVO>>() {
                });
        if (cached == null) {
            cached = loadMallList();
            long ttl = 15 + ThreadLocalRandom.current().nextInt(10);
            redisService.set(SeckillKeys.ACTIVITY_LIST, cached, Duration.ofSeconds(ttl));
        }
        cached.forEach(this::refreshDynamicFields);
        return cached;
    }

    private List<SeckillActivityVO> loadMallList() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, 1)
                .gt(SeckillActivity::getEndTime, now)
                .orderByAsc(SeckillActivity::getStartTime)
                .last("LIMIT 20"));
        Map<Long, Product> products = loadProducts(activities);
        return activities.stream()
                .map(a -> toVO(a, products.get(a.getProductId()), false))
                .toList();
    }

    /**
     * 活动详情：静态信息缓存 30 秒，库存/状态/秒杀链接实时刷新
     */
    public SeckillActivityVO detailMall(Long id) {
        String key = SeckillKeys.ACTIVITY_DETAIL + id;
        SeckillActivityVO vo = redisService.get(key, SeckillActivityVO.class);
        if (vo == null) {
            SeckillActivity activity = activityMapper.selectById(id);
            if (activity == null || activity.getStatus() == null || activity.getStatus() == 0) {
                throw BizException.notFound("秒杀活动不存在");
            }
            Product product = productMapper.selectById(activity.getProductId());
            if (product == null) {
                throw BizException.notFound("活动商品不存在");
            }
            vo = toVO(activity, product, true);
            long ttl = 30 + ThreadLocalRandom.current().nextInt(15);
            redisService.set(key, vo, Duration.ofSeconds(ttl));
        }
        refreshDynamicFields(vo);
        if ("ONGOING".equals(vo.getState())) {
            vo.setSeckillPath(loadPath(id));
        }
        return vo;
    }

    public SeckillActivity getEntity(Long id) {
        SeckillActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw BizException.notFound("秒杀活动不存在");
        }
        return activity;
    }

    /**
     * 启动预热：保证已上架活动在 Redis 里有库存 key 和隐藏链接
     */
    public void warmUp() {
        List<SeckillActivity> activities = activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, 1)
                .gt(SeckillActivity::getEndTime, LocalDateTime.now()));
        for (SeckillActivity activity : activities) {
            stockService.ensureStock(activity);
            if (!StringUtils.hasText(activity.getSeckillPath())) {
                activity.setSeckillPath(UUID.randomUUID().toString().replace("-", ""));
                activityMapper.updateById(activity);
            }
            redisService.set(SeckillKeys.PATH + activity.getId(), activity.getSeckillPath(), Duration.ofDays(30));
        }
        log.info("秒杀活动预热完成，共 {} 个进行中活动", activities.size());
    }

    public long countActive() {
        return activityMapper.countActive();
    }

    // ==================== 内部 ====================

    private void refreshDynamicFields(SeckillActivityVO vo) {
        LocalDateTime now = LocalDateTime.now();
        Integer remaining = stockService.remaining(vo.getId());
        if (remaining != null) {
            vo.setStock(remaining);
        }
        vo.setState(calculateState(vo.getStatus(), vo.getStartTime(), vo.getEndTime(), now));
        vo.setServerTime(now);
        int initial = vo.getInitialStock() == null ? 0 : vo.getInitialStock();
        int stock = vo.getStock() == null ? 0 : vo.getStock();
        int sold = Math.max(0, initial - stock);
        vo.setSoldPercent(initial <= 0 ? 0 : Math.min(100, sold * 100 / initial));
    }

    private String loadPath(Long activityId) {
        String path = redisService.getString(SeckillKeys.PATH + activityId);
        if (StringUtils.hasText(path)) {
            return path;
        }
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity != null && StringUtils.hasText(activity.getSeckillPath())) {
            redisService.set(SeckillKeys.PATH + activityId, activity.getSeckillPath(), Duration.ofDays(30));
            return activity.getSeckillPath();
        }
        return null;
    }

    private SeckillActivityVO toVO(SeckillActivity activity, Product product, boolean withDescription) {
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(activity.getId());
        vo.setProductId(activity.getProductId());
        vo.setTitle(activity.getTitle());
        vo.setSeckillPrice(activity.getSeckillPrice());
        vo.setStock(activity.getStock());
        vo.setInitialStock(activity.getInitialStock());
        vo.setLimitPerUser(activity.getLimitPerUser());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setState(calculateState(activity, LocalDateTime.now()));
        vo.setServerTime(LocalDateTime.now());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductSubtitle(product.getSubtitle());
            vo.setProductCover(product.getCover());
            vo.setOriginalPrice(product.getPrice());
            if (withDescription) {
                vo.setProductDescription(product.getDescription());
            }
        }
        int initial = activity.getInitialStock() == null ? 0 : activity.getInitialStock();
        int stock = activity.getStock() == null ? 0 : activity.getStock();
        vo.setSoldPercent(initial <= 0 ? 0 : Math.min(100, Math.max(0, (initial - stock) * 100 / initial)));
        return vo;
    }

    private String calculateState(SeckillActivity activity, LocalDateTime now) {
        return calculateState(activity.getStatus(), activity.getStartTime(), activity.getEndTime(), now);
    }

    private String calculateState(Integer status, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (status == null || status == 0) {
            return "DRAFT";
        }
        if (status == 2 || (end != null && now.isAfter(end))) {
            return "ENDED";
        }
        if (start != null && now.isBefore(start)) {
            return "UPCOMING";
        }
        return "ONGOING";
    }

    private Map<Long, Product> loadProducts(List<SeckillActivity> activities) {
        Set<Long> ids = activities.stream().map(SeckillActivity::getProductId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
    }

    private void validate(SeckillActivity activity) {
        if (activity.getProductId() == null) {
            throw BizException.badRequest("请选择商品");
        }
        if (!StringUtils.hasText(activity.getTitle())) {
            throw BizException.badRequest("活动标题不能为空");
        }
        if (activity.getSeckillPrice() == null
                || activity.getSeckillPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("秒杀价必须大于 0");
        }
        if (activity.getStock() == null || activity.getStock() <= 0) {
            throw BizException.badRequest("秒杀库存必须大于 0");
        }
        if (activity.getStartTime() == null || activity.getEndTime() == null) {
            throw BizException.badRequest("请设置活动起止时间");
        }
        if (!activity.getStartTime().isBefore(activity.getEndTime())) {
            throw BizException.badRequest("开始时间必须早于结束时间");
        }
        if (activity.getLimitPerUser() != null && activity.getLimitPerUser() < 1) {
            throw BizException.badRequest("限购数量至少为 1");
        }
    }

    private void evict(Long id) {
        redisService.delete(SeckillKeys.ACTIVITY_DETAIL + id, SeckillKeys.ACTIVITY_LIST);
    }

    private void evictListCache() {
        redisService.delete(SeckillKeys.ACTIVITY_LIST);
    }
}
