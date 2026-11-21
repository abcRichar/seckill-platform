package com.seckill.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserProfileVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String roleType;
    private Integer status;
    private LocalDateTime createTime;
    private List<String> permissions;
    private List<String> roleNames;
}
