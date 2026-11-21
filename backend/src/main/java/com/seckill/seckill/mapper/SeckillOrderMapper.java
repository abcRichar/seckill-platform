package com.seckill.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.seckill.entity.SeckillOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    @Select("SELECT * FROM seckill_order WHERE order_no = #{orderNo} LIMIT 1")
    SeckillOrder selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM seckill_order WHERE user_id = #{userId} AND activity_id = #{activityId} LIMIT 1")
    SeckillOrder selectByUserAndActivity(@Param("userId") Long userId,
                                         @Param("activityId") Long activityId);

    /**
     * 状态机流转用条件更新：只有 status=0 才能支付/取消。
     * 返回 0 表示状态已经变了（并发重复回调就是靠这个做幂等）。
     */
    @Update("UPDATE seckill_order SET status = 1, pay_time = NOW() WHERE id = #{id} AND status = 0")
    int pay(@Param("id") Long id);

    @Update("UPDATE seckill_order SET status = 2, cancel_time = NOW() WHERE order_no = #{orderNo} AND status = 0")
    int cancel(@Param("orderNo") String orderNo);

    // ==================== 退款状态机 ====================

    /** 申请退款：仅已支付/已完成、且当前无退款或上次被拒 */
    @Update("UPDATE seckill_order SET refund_status = 1 WHERE id = #{id} " +
            "AND status IN (1, 3) AND refund_status IN (0, 3)")
    int applyRefund(@Param("id") Long id);

    /** 审核通过：待审核 -> 已退款（订单状态置为 4） */
    @Update("UPDATE seckill_order SET status = 4, refund_status = 2, refund_time = NOW() WHERE id = #{id} " +
            "AND status IN (1, 3) AND refund_status = 1")
    int markRefundedFromPending(@Param("id") Long id);

    /** 管理员直接退款：无退款或被拒 -> 已退款 */
    @Update("UPDATE seckill_order SET status = 4, refund_status = 2, refund_time = NOW() WHERE id = #{id} " +
            "AND status IN (1, 3) AND refund_status IN (0, 3)")
    int markRefundedDirect(@Param("id") Long id);

    /** 审核拒绝：订单仍是已支付/已完成，退款状态置为被拒，允许用户重新申请 */
    @Update("UPDATE seckill_order SET refund_status = 3 WHERE id = #{id} AND refund_status = 1")
    int markRefundRejected(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM seckill_order WHERE create_time >= CURDATE()")
    long countToday();

    @Select("SELECT IFNULL(SUM(seckill_price * quantity), 0) FROM seckill_order " +
            "WHERE status IN (1, 3) AND create_time >= CURDATE()")
    BigDecimal sumTodaySales();

    @Select("SELECT COUNT(*) FROM seckill_order WHERE status = #{status}")
    long countByStatus(@Param("status") int status);

    /**
     * 近 7 天订单趋势（缺失日期由 Java 补 0）
     */
    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS day, COUNT(*) AS orders, " +
            "IFNULL(SUM(CASE WHEN status IN (1,3) THEN seckill_price * quantity ELSE 0 END), 0) AS amount " +
            "FROM seckill_order WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%m-%d') ORDER BY MIN(create_time)")
    List<Map<String, Object>> select7DayTrend();
}
