package com.seckill.product.controller;

import com.seckill.common.annotation.OpLog;
import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.result.R;
import com.seckill.product.entity.Category;
import com.seckill.product.service.CategoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端-分类")
@RestController
@RequestMapping("/api/admin/categories")
@RequirePerm("category:manage")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表")
    @GetMapping
    public R<List<Category>> list() {
        return R.ok(categoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @OpLog(module = "分类管理", action = "新增分类")
    @PostMapping
    public R<Void> create(@RequestBody Category category) {
        categoryService.create(category);
        return R.ok();
    }

    @Operation(summary = "修改分类")
    @OpLog(module = "分类管理", action = "修改分类")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Category category) {
        categoryService.update(id, category);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @OpLog(module = "分类管理", action = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
