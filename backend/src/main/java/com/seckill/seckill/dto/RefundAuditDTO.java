package com.seckill.seckill.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundAuditDTO {

    @Size(max = 255, message = "审核备注最长 255 个字符")
    private String remark;
}
