package com.seckill.seckill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.annotation.OpLog;
import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.R;
import com.seckill.seckill.dto.RefundAuditDTO;
import com.seckill.seckill.entity.SeckillRefund;
import com.seckill.seckill.service.RefundService;
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

import java.util.Map;

@Tag(name = "管理端-退款审核")
@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    @Operation(summary = "退款单分页")
    @RequirePerm("refund:list")
    @GetMapping
    public R<PageResult<SeckillRefund>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        Page<SeckillRefund> result = refundService.pageAdmin(page, size, keyword, status);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "退款统计")
    @RequirePerm("refund:list")
    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        return R.ok(refundService.summary());
    }

    @Operation(summary = "退款单详情")
    @RequirePerm("refund:list")
    @GetMapping("/{id}")
    public R<SeckillRefund> detail(@PathVariable Long id) {
        return R.ok(refundService.getAdmin(id));
    }

    @Operation(summary = "审核通过", description = "通过后订单状态变为已退款（演示环境不回补库存）")
    @RequirePerm("refund:audit")
    @OpLog(module = "退款管理", action = "通过退款")
    @PostMapping("/{id}/approve")
    public R<SeckillRefund> approve(@PathVariable Long id,
                                    @Valid @RequestBody(required = false) RefundAuditDTO dto) {
        String remark = dto == null ? null : dto.getRemark();
        return R.ok("退款已通过", refundService.approve(id, remark));
    }

    @Operation(summary = "审核拒绝", description = "拒绝后订单保持已支付，用户可以重新申请")
    @RequirePerm("refund:audit")
    @OpLog(module = "退款管理", action = "拒绝退款")
    @PostMapping("/{id}/reject")
    public R<SeckillRefund> reject(@PathVariable Long id,
                                   @Valid @RequestBody(required = false) RefundAuditDTO dto) {
        String remark = dto == null ? null : dto.getRemark();
        return R.ok("退款已拒绝", refundService.reject(id, remark));
    }
}
