package com.seckill.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityVO {

    private Long id;
    private Long productId;
    private String title;
    private String productName;
    private String productSubtitle;
    private String productCover;
    private String productDescription;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    /** 当前剩余库存 */
    private Integer stock;
    private Integer initialStock;
    private Integer limitPerUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    /** UPCOMING / ONGOING / ENDED */
    private String state;
    private LocalDateTime serverTime;
    private String seckillPath;
    /** 已抢百分比，前端画进度条 */
    private Integer soldPercent;
}
