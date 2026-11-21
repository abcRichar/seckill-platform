package com.seckill.seckill.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.annotation.OpLog;
import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.R;
import com.seckill.seckill.dto.RefundApplyDTO;
import com.seckill.seckill.entity.SeckillOrder;
import com.seckill.seckill.entity.SeckillRefund;
import com.seckill.seckill.service.RefundService;
import com.seckill.seckill.service.SeckillOrderService;
import com.seckill.seckill.vo.OrderExportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "管理端-订单")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final SeckillOrderService orderService;
    private final RefundService refundService;

    @Operation(summary = "订单分页")
    @RequirePerm("order:list")
    @GetMapping
    public R<PageResult<SeckillOrder>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Long activityId,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate) {
        Page<SeckillOrder> result = orderService.pageAdmin(page, size, keyword, status,
                activityId, startDate, endDate);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "订单详情")
    @RequirePerm("order:list")
    @GetMapping("/{id}")
    public R<SeckillOrder> detail(@PathVariable Long id) {
        return R.ok(orderService.adminDetail(id));
    }

    @Operation(summary = "管理员取消订单")
    @RequirePerm("order:edit")
    @OpLog(module = "订单管理", action = "取消订单")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.adminCancel(id);
        return R.ok();
    }

    @Operation(summary = "管理员直接退款", description = "对已支付/已完成订单直接退款，无需用户先申请")
    @RequirePerm("refund:audit")
    @OpLog(module = "订单管理", action = "管理员退款")
    @PostMapping("/{id}/refund")
    public R<SeckillRefund> refund(@PathVariable Long id, @Valid @RequestBody RefundApplyDTO dto) {
        return R.ok("退款已完成", refundService.directRefund(id, dto));
    }

    @Operation(summary = "导出订单 Excel", description = "EasyExcel 流式导出，最多 5000 条")
    @RequirePerm("order:export")
    @OpLog(module = "订单管理", action = "导出订单")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Long activityId,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) throws IOException {
        List<OrderExportVO> data = orderService.listForExport(keyword, status, activityId, startDate, endDate);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("秒杀订单-" + LocalDate.now(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), OrderExportVO.class)
                .sheet("订单")
                .doWrite(data);
    }
}
