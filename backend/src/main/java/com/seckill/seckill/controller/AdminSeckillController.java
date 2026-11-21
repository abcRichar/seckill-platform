package com.seckill.seckill.controller;

import com.seckill.common.annotation.OpLog;
import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.R;
import com.seckill.seckill.entity.SeckillActivity;
import com.seckill.seckill.service.SeckillActivityService;
import com.seckill.seckill.vo.SeckillActivityVO;
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

@Tag(name = "管理端-秒杀活动")
@RestController
@RequestMapping("/api/admin/seckill")
@RequiredArgsConstructor
public class AdminSeckillController {

    private final SeckillActivityService activityService;

    @Operation(summary = "活动分页")
    @RequirePerm("seckill:list")
    @GetMapping
    public R<PageResult<SeckillActivityVO>> page(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status) {
        return R.ok(activityService.pageAdmin(page, size, keyword, status));
    }

    @Operation(summary = "活动详情")
    @RequirePerm("seckill:list")
    @GetMapping("/{id}")
    public R<SeckillActivityVO> detail(@PathVariable Long id) {
        return R.ok(activityService.getAdmin(id));
    }

    @Operation(summary = "新增活动")
    @RequirePerm("seckill:edit")
    @OpLog(module = "秒杀活动", action = "新增活动")
    @PostMapping
    public R<Void> create(@RequestBody SeckillActivity activity) {
        activityService.create(activity);
        return R.ok();
    }

    @Operation(summary = "修改活动")
    @RequirePerm("seckill:edit")
    @OpLog(module = "秒杀活动", action = "修改活动")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SeckillActivity activity) {
        activityService.update(id, activity);
        return R.ok();
    }

    @Operation(summary = "删除活动")
    @RequirePerm("seckill:edit")
    @OpLog(module = "秒杀活动", action = "删除活动")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return R.ok();
    }

    @Operation(summary = "上架", description = "生成隐藏链接 + 用 DB 库存初始化 Redis")
    @RequirePerm("seckill:edit")
    @OpLog(module = "秒杀活动", action = "上架活动")
    @PutMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        activityService.publish(id);
        return R.ok();
    }

    @Operation(summary = "下架/结束")
    @RequirePerm("seckill:edit")
    @OpLog(module = "秒杀活动", action = "结束活动")
    @PutMapping("/{id}/stop")
    public R<Void> stop(@PathVariable Long id) {
        activityService.stop(id);
        return R.ok();
    }
}
