package com.seckill.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.seckill.entity.SeckillRefund;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface SeckillRefundMapper extends BaseMapper<SeckillRefund> {

    @Select("SELECT * FROM seckill_refund WHERE order_id = #{orderId} LIMIT 1")
    SeckillRefund selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT COUNT(*) FROM seckill_refund WHERE status = #{status}")
    long countByStatus(@Param("status") int status);

    /**
     * 审核通过：条件更新保证并发下只有一个管理员能通过同一笔退款
     */
    @Update("UPDATE seckill_refund SET status = 1, audit_by = #{auditBy}, audit_name = #{auditName}, " +
            "audit_time = NOW(), audit_remark = #{remark}, refund_time = NOW() " +
            "WHERE id = #{id} AND status = 0")
    int approve(@Param("id") Long id,
                @Param("auditBy") Long auditBy,
                @Param("auditName") String auditName,
                @Param("remark") String remark);

    @Update("UPDATE seckill_refund SET status = 2, audit_by = #{auditBy}, audit_name = #{auditName}, " +
            "audit_time = NOW(), audit_remark = #{remark} " +
            "WHERE id = #{id} AND status = 0")
    int reject(@Param("id") Long id,
               @Param("auditBy") Long auditBy,
               @Param("auditName") String auditName,
               @Param("remark") String remark);

    /** 被拒后重新申请：复用原退款单，清空上一次审核信息 */
    @Update("UPDATE seckill_refund SET status = 0, reason = #{reason}, description = #{description}, " +
            "refund_amount = #{amount}, audit_by = NULL, audit_name = NULL, audit_time = NULL, " +
            "audit_remark = NULL, refund_time = NULL WHERE id = #{id} AND status = 2")
    int reapply(@Param("id") Long id,
                @Param("reason") String reason,
                @Param("description") String description,
                @Param("amount") BigDecimal amount);

    /** 管理员直接退款：把之前被拒的退款单直接置为通过 */
    @Update("UPDATE seckill_refund SET status = 1, reason = #{reason}, description = #{description}, " +
            "refund_amount = #{amount}, audit_by = #{auditBy}, audit_name = #{auditName}, " +
            "audit_time = NOW(), audit_remark = #{remark}, refund_time = NOW() " +
            "WHERE id = #{id} AND status = 2")
    int directApproveRejected(@Param("id") Long id,
                              @Param("reason") String reason,
                              @Param("description") String description,
                              @Param("amount") BigDecimal amount,
                              @Param("auditBy") Long auditBy,
                              @Param("auditName") String auditName,
                              @Param("remark") String remark);
}
