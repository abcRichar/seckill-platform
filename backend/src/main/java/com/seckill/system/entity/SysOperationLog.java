package com.seckill.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String module;
    private String action;
    private String method;
    private String params;
    private String ip;
    private Long duration;
    /** 1成功 0失败 */
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;
}
