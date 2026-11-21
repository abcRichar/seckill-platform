package com.seckill.seckill.controller;

import com.seckill.common.annotation.RateLimit;
import com.seckill.common.annotation.RequireLogin;
import com.seckill.common.result.R;
import com.seckill.seckill.dto.SeckillOrderRequestDTO;
import com.seckill.seckill.service.SeckillActivityService;
import com.seckill.seckill.service.SeckillService;
import com.seckill.seckill.vo.SeckillActivityVO;
import com.seckill.seckill.vo.SeckillResultVO;
import com.seckill.seckill.vo.SeckillTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户端-秒杀")
@RestController
@RequestMapping("/api/mall/seckill")
@RequiredArgsConstructor
public class MallSeckillController {

    private final SeckillActivityService activityService;
    private final SeckillService seckillService;

    @Operation(summary = "秒杀活动列表")
    @GetMapping("/list")
    public R<List<SeckillActivityVO>> list() {
        return R.ok(activityService.listMall());
    }

    @Operation(summary = "秒杀活动详情")
    @GetMapping("/{id}")
    public R<SeckillActivityVO> detail(@PathVariable Long id) {
        return R.ok(activityService.detailMall(id));
    }

    @Operation(summary = "获取秒杀令牌", description = "10 秒内最多 3 次；令牌 60 秒有效且只能用一次")
    @RequireLogin
    @RateLimit(key = "seckill:token", count = 3, seconds = 10)
    @PostMapping("/{id}/token")
    public R<SeckillTokenVO> token(@PathVariable Long id, @RequestParam String path) {
        return R.ok(seckillService.getToken(id, path));
    }

    @Operation(summary = "秒杀下单", description = "Lua 预扣库存后写入 Redis Stream，立即返回排队中")
    @RequireLogin
    @RateLimit(key = "seckill:order", count = 2, seconds = 5)
    @PostMapping("/order")
    public R<SeckillResultVO> seckill(@Valid @RequestBody SeckillOrderRequestDTO dto) {
        return R.ok(seckillService.seckill(dto));
    }

    @Operation(summary = "轮询秒杀结果")
    @RequireLogin
    @GetMapping("/result")
    public R<SeckillResultVO> result(@RequestParam String orderNo) {
        return R.ok(seckillService.result(orderNo));
    }

    @Operation(summary = "当前用户是否已参与该活动")
    @RequireLogin
    @GetMapping("/{id}/mine")
    public R<java.util.Map<String, Object>> mine(@PathVariable Long id) {
        return R.ok(seckillService.myActivityStatus(id));
    }
}
