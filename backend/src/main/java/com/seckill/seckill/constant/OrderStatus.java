package com.seckill.seckill.constant;

/**
 * 订单状态机：0 待支付 -> 1 已支付 -> 3 已完成
 *                \-> 2 已取消（仅待支付可取消）
 * 所有流转都用 "UPDATE ... WHERE status = 旧状态" 保证幂等。
 */
public final class OrderStatus {

    private OrderStatus() {
    }

    public static final int UNPAID = 0;
    public static final int PAID = 1;
    public static final int CANCELLED = 2;
    public static final int FINISHED = 3;
    public static final int REFUNDED = 4;

    public static String text(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case UNPAID -> "待支付";
            case PAID -> "已支付";
            case CANCELLED -> "已取消";
            case FINISHED -> "已完成";
            case REFUNDED -> "已退款";
            default -> "未知";
        };
    }
}
