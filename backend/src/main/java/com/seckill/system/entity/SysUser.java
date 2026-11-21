package com.seckill.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 永远不返回给前端 */
    @JsonIgnore
    private String password;

    private String nickname;
    private String avatar;
    private String phone;
    private String email;

    /** ADMIN / USER */
    private String roleType;

    /** 1正常 0禁用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
