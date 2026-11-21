package com.seckill.auth.controller;

import com.seckill.auth.dto.ChangePasswordDTO;
import com.seckill.auth.dto.ProfileDTO;
import com.seckill.auth.service.AuthService;
import com.seckill.auth.vo.UserProfileVO;
import com.seckill.common.annotation.RequireLogin;
import com.seckill.common.context.UserContext;
import com.seckill.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-个人中心")
@RestController
@RequestMapping("/api/mall/user")
@RequireLogin
@RequiredArgsConstructor
public class MallUserController {

    private final AuthService authService;

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public R<UserProfileVO> me() {
        return R.ok(authService.getProfile(UserContext.requireUserId(), false));
    }

    @Operation(summary = "修改资料")
    @PutMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody ProfileDTO dto) {
        authService.updateProfile(UserContext.requireUserId(), dto);
        return R.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(UserContext.requireUserId(), dto);
        return R.ok();
    }
}
