package com.seckill.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.result.R;
import com.seckill.product.service.ProductService;
import com.seckill.seckill.constant.OrderStatus;
import com.seckill.seckill.entity.SeckillOrder;
import com.seckill.seckill.mapper.SeckillOrderMapper;
import com.seckill.seckill.service.RefundService;
import com.seckill.seckill.service.SeckillActivityService;
import com.seckill.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "管理端-工作台")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequirePerm("dashboard:view")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final SysUserService userService;
    private final ProductService productService;
    private final SeckillActivityService activityService;
    private final SeckillOrderMapper orderMapper;
    private final RefundService refundService;

    @Operation(summary = "工作台统计")
    @GetMapping
    public R<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userService.countUsers());
        data.put("productCount", productService.countOnSale());
        data.put("activeActivities", activityService.countActive());
        data.put("todayOrders", orderMapper.countToday());
        data.put("todaySales", orderMapper.sumTodaySales());
        data.put("unpaidOrders", orderMapper.countByStatus(OrderStatus.UNPAID));
        data.put("paidOrders", orderMapper.countByStatus(OrderStatus.PAID));
        data.put("cancelledOrders", orderMapper.countByStatus(OrderStatus.CANCELLED));
        data.put("finishedOrders", orderMapper.countByStatus(OrderStatus.FINISHED));
        data.put("refundedOrders", orderMapper.countByStatus(OrderStatus.REFUNDED));
        data.put("pendingRefunds", refundService.countPending());
        data.put("trend", trend());

        Page<SeckillOrder> recent = orderMapper.selectPage(new Page<>(1, 8),
                new LambdaQueryWrapper<SeckillOrder>().orderByDesc(SeckillOrder::getId));
        data.put("recentOrders", recent.getRecords());
        return R.ok(data);
    }

    /**
     * 近 7 天趋势，缺失日期补 0，前端直接画柱状图
     */
    private List<Map<String, Object>> trend() {
        List<Map<String, Object>> rows = orderMapper.select7DayTrend();
        Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byDay.put(String.valueOf(row.get("day")), row);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).format(formatter);
            Map<String, Object> row = byDay.get(day);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", day);
            item.put("orders", row == null ? 0 : row.get("orders"));
            item.put("amount", row == null ? 0 : row.get("amount"));
            result.add(item);
        }
        return result;
    }
}
