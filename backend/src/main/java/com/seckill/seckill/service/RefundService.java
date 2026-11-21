package com.seckill.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.context.LoginUser;
import com.seckill.common.context.UserContext;
import com.seckill.common.exception.BizException;
import com.seckill.seckill.constant.OrderStatus;
import com.seckill.seckill.constant.RefundStatus;
import com.seckill.seckill.dto.RefundApplyDTO;
import com.seckill.seckill.entity.SeckillOrder;
import com.seckill.seckill.entity.SeckillRefund;
import com.seckill.seckill.mapper.SeckillOrderMapper;
import com.seckill.seckill.mapper.SeckillRefundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退款流程
 *
 * 用户申请（待审核） → 管理员通过（订单变已退款） / 拒绝（订单保持已支付，可重新申请）
 * 管理员也可以对已支付订单直接退款。
 *
 * 说明：本演示只退金额，不回补秒杀库存（商品已售出）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final SeckillRefundMapper refundMapper;
    private final SeckillOrderMapper orderMapper;
    private final SeckillOrderService orderService;

    // ==================== 用户端 ====================

    @Transactional
    public SeckillRefund apply(Long orderId, RefundApplyDTO dto) {
        Long userId = UserContext.requireUserId();
        SeckillOrder order = orderService.getOwned(orderId, userId);
        checkCanRefund(order, true);

        int rows = orderMapper.applyRefund(orderId);
        if (rows == 0) {
            throw BizException.badRequest("订单状态已变更，请刷新后重试");
        }

        BigDecimal amount = refundAmount(order);
        String reason = dto.getReason().trim();
        String description = dto.getDescription() == null ? "" : dto.getDescription().trim();
        SeckillRefund existing = refundMapper.selectByOrderId(orderId);
        if (existing == null) {
            SeckillRefund refund = new SeckillRefund();
            refund.setRefundNo(generateRefundNo(userId));
            refund.setOrderId(order.getId());
            refund.setOrderNo(order.getOrderNo());
            refund.setUserId(order.getUserId());
            refund.setActivityId(order.getActivityId());
            refund.setProductId(order.getProductId());
            refund.setProductName(order.getProductName());
            refund.setProductCover(order.getProductCover());
            refund.setRefundAmount(amount);
            refund.setReason(reason);
            refund.setDescription(description);
            refund.setStatus(RefundStatus.AUDIT_PENDING);
            refundMapper.insert(refund);
        } else if (existing.getStatus() == RefundStatus.AUDIT_REJECTED) {
            int updated = refundMapper.reapply(existing.getId(), reason, description, amount);
            if (updated == 0) {
                throw BizException.badRequest("退款单状态已变更，请刷新后重试");
            }
        } else if (existing.getStatus() == RefundStatus.AUDIT_PENDING) {
            // 极端不一致场景：退款单还挂着，但订单退款状态被重置了，直接刷新申请内容
            existing.setReason(reason);
            existing.setDescription(description);
            existing.setRefundAmount(amount);
            refundMapper.updateById(existing);
        } else {
            throw BizException.badRequest("退款单状态异常，请联系客服");
        }

        log.info("用户申请退款 orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, amount);
        return refundMapper.selectByOrderId(orderId);
    }

    public SeckillRefund getMine(Long orderId) {
        SeckillOrder order = orderService.getOwned(orderId, UserContext.requireUserId());
        return refundMapper.selectByOrderId(order.getId());
    }

    // ==================== 管理端 ====================

    public Page<SeckillRefund> pageAdmin(long current, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SeckillRefund> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SeckillRefund::getRefundNo, keyword)
                    .or().like(SeckillRefund::getOrderNo, keyword)
                    .or().like(SeckillRefund::getProductName, keyword));
        }
        wrapper.eq(status != null, SeckillRefund::getStatus, status);
        wrapper.orderByDesc(SeckillRefund::getId);
        return refundMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public Map<String, Object> summary() {
        Map<String, Object> data = new LinkedHashMap<>();
        long pending = refundMapper.countByStatus(RefundStatus.AUDIT_PENDING);
        long approved = refundMapper.countByStatus(RefundStatus.AUDIT_APPROVED);
        long rejected = refundMapper.countByStatus(RefundStatus.AUDIT_REJECTED);
        data.put("pending", pending);
        data.put("approved", approved);
        data.put("rejected", rejected);
        data.put("total", pending + approved + rejected);
        return data;
    }

    public SeckillRefund getAdmin(Long id) {
        SeckillRefund refund = refundMapper.selectById(id);
        if (refund == null) {
            throw BizException.notFound("退款单不存在");
        }
        return refund;
    }

    @Transactional
    public SeckillRefund approve(Long id, String remark) {
        SeckillRefund refund = getAdmin(id);
        if (refund.getStatus() == null || refund.getStatus() != RefundStatus.AUDIT_PENDING) {
            throw BizException.badRequest("该退款单已审核，不能重复操作");
        }
        LoginUser admin = UserContext.require();
        String auditRemark = StringUtils.hasText(remark) ? remark.trim() : "同意退款，原路退回";
        int rows = refundMapper.approve(id, admin.getUserId(), admin.getUsername(), auditRemark);
        if (rows == 0) {
            throw BizException.badRequest("退款单状态已变更，请刷新后重试");
        }
        int orderRows = orderMapper.markRefundedFromPending(refund.getOrderId());
        if (orderRows == 0) {
            throw BizException.badRequest("订单状态已变更，无法退款");
        }
        log.info("退款审核通过 refundNo={}, orderNo={}, admin={}", refund.getRefundNo(), refund.getOrderNo(), admin.getUsername());
        return refundMapper.selectById(id);
    }

    @Transactional
    public SeckillRefund reject(Long id, String remark) {
        SeckillRefund refund = getAdmin(id);
        if (refund.getStatus() == null || refund.getStatus() != RefundStatus.AUDIT_PENDING) {
            throw BizException.badRequest("该退款单已审核，不能重复操作");
        }
        LoginUser admin = UserContext.require();
        String auditRemark = StringUtils.hasText(remark) ? remark.trim() : "未通过审核";
        int rows = refundMapper.reject(id, admin.getUserId(), admin.getUsername(), auditRemark);
        if (rows == 0) {
            throw BizException.badRequest("退款单状态已变更，请刷新后重试");
        }
        int orderRows = orderMapper.markRefundRejected(refund.getOrderId());
        if (orderRows == 0) {
            throw BizException.badRequest("订单状态已变更，无法拒绝退款");
        }
        log.info("退款审核拒绝 refundNo={}, orderNo={}, admin={}", refund.getRefundNo(), refund.getOrderNo(), admin.getUsername());
        return refundMapper.selectById(id);
    }

    /**
     * 管理员直接在订单页退款：无需用户先申请
     */
    @Transactional
    public SeckillRefund directRefund(Long orderId, RefundApplyDTO dto) {
        SeckillOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        if (order.getStatus() == null
                || (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.FINISHED)) {
            throw BizException.badRequest("只有已支付/已完成订单可以退款");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == RefundStatus.PENDING) {
            throw BizException.badRequest("该订单有待审核的退款申请，请到退款审核页处理");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == RefundStatus.REFUNDED) {
            throw BizException.badRequest("订单已退款，请勿重复操作");
        }

        LoginUser admin = UserContext.require();
        BigDecimal amount = refundAmount(order);
        int rows = orderMapper.markRefundedDirect(orderId);
        if (rows == 0) {
            throw BizException.badRequest("订单状态已变更，请刷新后重试");
        }

        String reason = StringUtils.hasText(dto.getReason()) ? dto.getReason().trim() : "管理员直接退款";
        String description = dto.getDescription() == null ? "" : dto.getDescription().trim();
        String auditRemark = "管理员直接退款";
        SeckillRefund existing = refundMapper.selectByOrderId(orderId);
        if (existing == null) {
            SeckillRefund refund = new SeckillRefund();
            refund.setRefundNo(generateRefundNo(order.getUserId()));
            refund.setOrderId(order.getId());
            refund.setOrderNo(order.getOrderNo());
            refund.setUserId(order.getUserId());
            refund.setActivityId(order.getActivityId());
            refund.setProductId(order.getProductId());
            refund.setProductName(order.getProductName());
            refund.setProductCover(order.getProductCover());
            refund.setRefundAmount(amount);
            refund.setReason(reason);
            refund.setDescription(description);
            refund.setStatus(RefundStatus.AUDIT_APPROVED);
            refund.setAuditBy(admin.getUserId());
            refund.setAuditName(admin.getUsername());
            refund.setAuditTime(LocalDateTime.now());
            refund.setAuditRemark(auditRemark);
            refund.setRefundTime(LocalDateTime.now());
            refundMapper.insert(refund);
        } else if (existing.getStatus() != null && existing.getStatus() == RefundStatus.AUDIT_REJECTED) {
            int updated = refundMapper.directApproveRejected(existing.getId(), reason, description, amount,
                    admin.getUserId(), admin.getUsername(), auditRemark);
            if (updated == 0) {
                throw BizException.badRequest("退款单状态已变更，请刷新后重试");
            }
        } else {
            throw BizException.badRequest("退款单状态异常，请先处理已有退款申请");
        }

        log.info("管理员直接退款 orderNo={}, amount={}, admin={}", order.getOrderNo(), amount, admin.getUsername());
        return refundMapper.selectByOrderId(orderId);
    }

    public long countPending() {
        return refundMapper.countByStatus(RefundStatus.AUDIT_PENDING);
    }

    // ==================== 内部 ====================

    private void checkCanRefund(SeckillOrder order, boolean forApply) {
        if (order.getStatus() == null
                || (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.FINISHED)) {
            throw BizException.badRequest("只有已支付/已完成订单可以申请退款");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == RefundStatus.PENDING) {
            throw BizException.badRequest(forApply ? "退款申请已提交，请等待审核" : "该订单有待审核的退款申请");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == RefundStatus.REFUNDED) {
            throw BizException.badRequest("订单已退款，请勿重复申请");
        }
    }

    private BigDecimal refundAmount(SeckillOrder order) {
        BigDecimal price = order.getSeckillPrice() == null ? BigDecimal.ZERO : order.getSeckillPrice();
        int quantity = order.getQuantity() == null ? 1 : order.getQuantity();
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    private String generateRefundNo(Long userId) {
        String suffix = String.format("%04d", userId % 10000);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "RF" + LocalDateTime.now().format(NO_FORMAT) + suffix + random;
    }
}
