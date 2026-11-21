package com.seckill.product.controller;

import com.seckill.common.result.PageResult;
import com.seckill.common.result.R;
import com.seckill.product.entity.Category;
import com.seckill.product.service.CategoryService;
import com.seckill.product.service.ProductService;
import com.seckill.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户端-商品")
@RestController
@RequestMapping("/api/mall")
@RequiredArgsConstructor
public class MallProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @Operation(summary = "商品分页搜索")
    @GetMapping("/products")
    public R<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "12") long size,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "sales") String sort) {
        return R.ok(productService.pageMall(page, size, categoryId, keyword, sort));
    }

    @Operation(summary = "商品详情（带缓存）")
    @GetMapping("/products/{id}")
    public R<ProductVO> detail(@PathVariable Long id) {
        return R.ok(productService.detailMall(id));
    }

    @Operation(summary = "分类列表")
    @GetMapping("/categories")
    public R<List<Category>> categories() {
        return R.ok(categoryService.listAll());
    }
}
