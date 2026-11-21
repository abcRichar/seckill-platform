package com.seckill.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户（从 JWT 解析而来）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private String roleType;

    public boolean isAdmin() {
        return "ADMIN".equals(roleType);
    }
}
