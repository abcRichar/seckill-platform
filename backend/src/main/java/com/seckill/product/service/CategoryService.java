package com.seckill.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.exception.BizException;
import com.seckill.product.entity.Category;
import com.seckill.product.entity.Product;
import com.seckill.product.mapper.CategoryMapper;
import com.seckill.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    public List<Category> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    public Category getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BizException.notFound("分类不存在");
        }
        return category;
    }

    public void create(Category category) {
        if (!StringUtils.hasText(category.getName())) {
            throw BizException.badRequest("分类名称不能为空");
        }
        category.setId(null);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        categoryMapper.insert(category);
    }

    public void update(Long id, Category category) {
        getById(id);
        if (!StringUtils.hasText(category.getName())) {
            throw BizException.badRequest("分类名称不能为空");
        }
        category.setId(id);
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        getById(id);
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw BizException.badRequest("该分类下还有 " + productCount + " 个商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
