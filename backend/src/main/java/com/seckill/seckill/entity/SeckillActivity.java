package com.seckill.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_activity")
public class SeckillActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private String title;
    private BigDecimal seckillPrice;
    /** 剩余库存（DB 为准，Redis 是并发入口） */
    private Integer stock;
    private Integer initialStock;
    private Integer limitPerUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0草稿 1上架 2结束 */
    private Integer status;
    /** 隐藏秒杀链接 */
    private String seckillPath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
