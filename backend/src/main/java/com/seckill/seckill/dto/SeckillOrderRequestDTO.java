package com.seckill.seckill.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillOrderRequestDTO {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotBlank(message = "秒杀令牌不能为空")
    private String token;

    @NotBlank(message = "秒杀链接不能为空")
    private String path;

    @Min(value = 1, message = "购买数量至少为 1")
    private Integer quantity = 1;
}
