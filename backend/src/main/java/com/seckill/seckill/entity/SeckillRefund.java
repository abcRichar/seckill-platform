package com.seckill.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_refund")
public class SeckillRefund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long activityId;
    private Long productId;
    private String productName;
    private String productCover;
    private BigDecimal refundAmount;
    private String reason;
    private String description;
    /** 0待审核 1已通过 2已拒绝 */
    private Integer status;
    private Long auditBy;
    private String auditName;
    private LocalDateTime auditTime;
    private String auditRemark;
    private LocalDateTime refundTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
