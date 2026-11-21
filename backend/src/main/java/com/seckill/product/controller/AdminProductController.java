package com.seckill.product.controller;

import com.seckill.common.annotation.OpLog;
import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.R;
import com.seckill.product.entity.Product;
import com.seckill.product.service.ProductService;
import com.seckill.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-商品")
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页")
    @RequirePerm("product:list")
    @GetMapping
    public R<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) Integer status) {
        return R.ok(productService.pageAdmin(page, size, keyword, categoryId, status));
    }

    @Operation(summary = "商品详情")
    @RequirePerm("product:list")
    @GetMapping("/{id}")
    public R<ProductVO> detail(@PathVariable Long id) {
        return R.ok(productService.getAdmin(id));
    }

    @Operation(summary = "新增商品")
    @RequirePerm("product:edit")
    @OpLog(module = "商品管理", action = "新增商品")
    @PostMapping
    public R<Void> create(@RequestBody Product product) {
        productService.create(product);
        return R.ok();
    }

    @Operation(summary = "修改商品")
    @RequirePerm("product:edit")
    @OpLog(module = "商品管理", action = "修改商品")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Product product) {
        productService.update(id, product);
        return R.ok();
    }

    @Operation(summary = "上下架")
    @RequirePerm("product:edit")
    @OpLog(module = "商品管理", action = "商品上下架")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "删除商品")
    @RequirePerm("product:edit")
    @OpLog(module = "商品管理", action = "删除商品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return R.ok();
    }
}
