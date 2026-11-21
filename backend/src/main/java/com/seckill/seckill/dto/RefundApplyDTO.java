package com.seckill.seckill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundApplyDTO {

    @NotBlank(message = "请选择/填写退款原因")
    @Size(max = 100, message = "退款原因最长 100 个字符")
    private String reason;

    @Size(max = 300, message = "补充说明最长 300 个字符")
    private String description;
}
