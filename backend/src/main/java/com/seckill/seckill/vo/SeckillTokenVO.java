package com.seckill.seckill.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeckillTokenVO {

    private Long activityId;
    private String token;
    private long expireSeconds;
}
