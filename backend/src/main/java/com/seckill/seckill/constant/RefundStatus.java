package com.seckill.seckill.constant;

/**
 * 退款状态约定
 *
 * 订单上的 refund_status：
 *  0 无退款 / 1 退款审核中 / 2 已退款 / 3 退款被拒
 *
 * 退款单 seckill_refund.status：
 *  0 待审核 / 1 已通过 / 2 已拒绝
 */
public final class RefundStatus {

    private RefundStatus() {
    }

    // 订单退款状态
    public static final int NONE = 0;
    public static final int PENDING = 1;
    public static final int REFUNDED = 2;
    public static final int REJECTED = 3;

    // 退款单审核状态
    public static final int AUDIT_PENDING = 0;
    public static final int AUDIT_APPROVED = 1;
    public static final int AUDIT_REJECTED = 2;

    public static String orderText(Integer status) {
        if (status == null) {
            return "无退款";
        }
        return switch (status) {
            case NONE -> "无退款";
            case PENDING -> "退款审核中";
            case REFUNDED -> "已退款";
            case REJECTED -> "退款被拒";
            default -> "未知";
        };
    }

    public static String auditText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case AUDIT_PENDING -> "待审核";
            case AUDIT_APPROVED -> "已通过";
            case AUDIT_REJECTED -> "已拒绝";
            default -> "未知";
        };
    }
}
