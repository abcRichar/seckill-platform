package com.seckill.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.exception.BizException;
import com.seckill.common.result.PageResult;
import com.seckill.common.util.RedisService;
import com.seckill.product.entity.Category;
import com.seckill.product.entity.Product;
import com.seckill.product.mapper.CategoryMapper;
import com.seckill.product.mapper.ProductMapper;
import com.seckill.product.vo.ProductVO;
import com.seckill.seckill.entity.SeckillActivity;
import com.seckill.seckill.mapper.SeckillActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品服务：演示缓存三大问题的工程解法
 *
 * 1. 穿透：不存在的 id 缓存空值（短 TTL）
 * 2. 击穿：热点 key 失效瞬间加分布式锁，只放一个线程回源
 * 3. 雪崩：TTL 加随机值，避免同一批 key 同时过期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String DETAIL_KEY = "sk:cache:product:";
    private static final String LOCK_KEY = "sk:lock:product:";
    private static final String NULL_VALUE = "__NULL__";

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final RedisService redisService;
    private final RedissonClient redissonClient;
    private final SeckillActivityMapper activityMapper;

    public PageResult<ProductVO> pageMall(long current, long size, Long categoryId,
                                          String keyword, String sort) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId);
        wrapper.and(StringUtils.hasText(keyword),
                w -> w.like(Product::getName, keyword).or().like(Product::getSubtitle, keyword));
        switch (sort == null ? "" : sort) {
            case "price_asc" -> wrapper.orderByAsc(Product::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Product::getPrice);
            case "new" -> wrapper.orderByDesc(Product::getId);
            default -> wrapper.orderByDesc(Product::getSales);
        }
        Page<Product> page = productMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> categoryNames = categoryMap();
        List<ProductVO> records = page.getRecords().stream()
                .map(p -> toVO(p, categoryNames))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 商品详情：cache-aside + 空值缓存 + 互斥锁重建
     */
    public ProductVO detailMall(Long id) {
        String key = DETAIL_KEY + id;
        String cached = redisService.getString(key);
        if (NULL_VALUE.equals(cached)) {
            throw BizException.notFound("商品不存在或已下架");
        }
        if (cached != null) {
            return redisService.get(key, ProductVO.class);
        }

        RLock lock = redissonClient.getLock(LOCK_KEY + id);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 10, TimeUnit.SECONDS);
            if (!locked) {
                // 没抢到锁：直接回源一次，避免请求堆积等待
                return loadFromDb(id);
            }
            // double check：可能别的线程已经重建好了
            String again = redisService.getString(key);
            if (again != null && !NULL_VALUE.equals(again)) {
                return redisService.get(key, ProductVO.class);
            }
            ProductVO vo = loadFromDb(id);
            long ttl = 1800 + ThreadLocalRandom.current().nextInt(300);
            redisService.set(key, vo, Duration.ofSeconds(ttl));
            return vo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("获取商品信息被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private ProductVO loadFromDb(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            // 空值缓存 60 秒：挡住恶意刷不存在 id 的请求
            redisService.set(DETAIL_KEY + id, NULL_VALUE, Duration.ofSeconds(60));
            throw BizException.notFound("商品不存在或已下架");
        }
        return toVO(product, categoryMap());
    }

    public PageResult<ProductVO> pageAdmin(long current, long size, String keyword,
                                           Long categoryId, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), Product::getName, keyword);
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId);
        wrapper.eq(status != null, Product::getStatus, status);
        wrapper.orderByDesc(Product::getId);
        Page<Product> page = productMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> categoryNames = categoryMap();
        List<ProductVO> records = page.getRecords().stream()
                .map(p -> toVO(p, categoryNames))
                .toList();
        return PageResult.of(page, records);
    }

    public ProductVO getAdmin(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        return toVO(product, categoryMap());
    }

    public void create(Product product) {
        validate(product);
        product.setId(null);
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getSales() == null) {
            product.setSales(0);
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
        productMapper.insert(product);
    }

    public void update(Long id, Product product) {
        Product old = productMapper.selectById(id);
        if (old == null) {
            throw BizException.notFound("商品不存在");
        }
        validate(product);
        product.setId(id);
        productMapper.updateById(product);
        evict(id);
    }

    public void updateStatus(Long id, Integer status) {
        Product old = productMapper.selectById(id);
        if (old == null) {
            throw BizException.notFound("商品不存在");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(status);
        productMapper.updateById(update);
        evict(id);
    }

    public void delete(Long id) {
        if (productMapper.selectById(id) == null) {
            throw BizException.notFound("商品不存在");
        }
        Long activityCount = activityMapper.selectCount(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getProductId, id));
        if (activityCount > 0) {
            throw BizException.badRequest("该商品已关联 " + activityCount + " 个秒杀活动，无法删除");
        }
        productMapper.deleteById(id);
        evict(id);
    }

    public Product getEntity(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        return product;
    }

    public long countOnSale() {
        return productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1));
    }

    public void evict(Long id) {
        redisService.delete(DETAIL_KEY + id);
    }

    private void validate(Product product) {
        if (!StringUtils.hasText(product.getName())) {
            throw BizException.badRequest("商品名称不能为空");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("商品价格必须大于 0");
        }
        if (product.getStock() != null && product.getStock() < 0) {
            throw BizException.badRequest("库存不能为负数");
        }
        if (product.getCategoryId() != null && categoryMapper.selectById(product.getCategoryId()) == null) {
            throw BizException.badRequest("商品分类不存在");
        }
    }

    private ProductVO toVO(Product product, Map<Long, String> categoryNames) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        vo.setCategoryName(categoryNames.getOrDefault(product.getCategoryId(), "未分类"));
        return vo;
    }

    private Map<Long, String> categoryMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }
}
