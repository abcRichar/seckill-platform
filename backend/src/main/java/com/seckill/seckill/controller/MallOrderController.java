package com.seckill.seckill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.annotation.RateLimit;
import com.seckill.common.annotation.RequireLogin;
import com.seckill.common.context.UserContext;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.R;
import com.seckill.seckill.dto.RefundApplyDTO;
import com.seckill.seckill.entity.SeckillOrder;
import com.seckill.seckill.entity.SeckillRefund;
import com.seckill.seckill.service.RefundService;
import com.seckill.seckill.service.SeckillOrderService;
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

@Tag(name = "用户端-订单")
@RestController
@RequestMapping("/api/mall/orders")
@RequireLogin
@RequiredArgsConstructor
public class MallOrderController {

    private final SeckillOrderService orderService;
    private final RefundService refundService;

    @Operation(summary = "我的订单")
    @GetMapping
    public R<PageResult<SeckillOrder>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Integer status) {
        Page<SeckillOrder> result = orderService.pageMine(UserContext.requireUserId(), status, page, size);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<SeckillOrder> detail(@PathVariable Long id) {
        return R.ok(orderService.getOwned(id, UserContext.requireUserId()));
    }

    @Operation(summary = "模拟支付", description = "条件更新 + 状态判断，重复支付幂等")
    @PostMapping("/{id}/pay")
    public R<SeckillOrder> pay(@PathVariable Long id) {
        return R.ok("支付成功", orderService.pay(id, UserContext.requireUserId()));
    }

    @Operation(summary = "取消订单", description = "取消后 DB 库存 + Redis 库存都会回补")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancelByUser(id, UserContext.requireUserId());
        return R.ok();
    }

    @Operation(summary = "申请退款", description = "已支付/已完成订单可申请，提交后进入管理员审核")
    @RateLimit(key = "refund:apply", count = 3, seconds = 10)
    @PostMapping("/{id}/refund")
    public R<SeckillRefund> applyRefund(@PathVariable Long id, @Valid @RequestBody RefundApplyDTO dto) {
        return R.ok("退款申请已提交", refundService.apply(id, dto));
    }

    @Operation(summary = "查看订单退款单")
    @GetMapping("/{id}/refund")
    public R<SeckillRefund> refund(@PathVariable Long id) {
        return R.ok(refundService.getMine(id));
    }
}
