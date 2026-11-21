package com.seckill.auth.controller;

import com.seckill.auth.service.AuthService;
import com.seckill.auth.vo.UserProfileVO;
import com.seckill.common.context.UserContext;
import com.seckill.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-认证")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @Operation(summary = "当前管理员信息 + 权限码", description = "前端根据 permissions 控制菜单和按钮显隐")
    @GetMapping("/me")
    public R<UserProfileVO> me() {
        Long userId = UserContext.requireUserId();
        return R.ok(authService.getProfile(userId, true));
    }
}
