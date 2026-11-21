package com.seckill.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_order")
public class SeckillOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long activityId;
    private Long productId;
    private String productName;
    private String productCover;
    private BigDecimal seckillPrice;
    private Integer quantity;
    /** 0待支付 1已支付 2已取消 3已完成 4已退款 */
    private Integer status;
    /** 0无退款 1退款审核中 2已退款 3退款被拒 */
    private Integer refundStatus;
    private LocalDateTime refundTime;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
