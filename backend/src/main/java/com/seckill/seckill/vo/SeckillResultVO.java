package com.seckill.seckill.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀请求结果 / 轮询结果
 * status: QUEUED 排队中、SUCCESS 已创建、FAILED 失败
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResultVO {

    private String orderNo;
    private String status;
    private Long orderId;
    private Integer orderStatus;
    private String message;

    public static SeckillResultVO queued(String orderNo) {
        return new SeckillResultVO(orderNo, "QUEUED", null, null, "排队中，正在为您创建订单");
    }

    public static SeckillResultVO failed(String message) {
        return new SeckillResultVO(null, "FAILED", null, null, message);
    }
}
