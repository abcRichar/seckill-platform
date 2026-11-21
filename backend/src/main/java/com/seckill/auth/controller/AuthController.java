package com.seckill.auth.controller;

import com.seckill.auth.dto.LoginDTO;
import com.seckill.auth.dto.RegisterDTO;
import com.seckill.auth.service.AuthService;
import com.seckill.auth.vo.LoginVO;
import com.seckill.common.annotation.RateLimit;
import com.seckill.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录 / 注册（拦截器白名单）
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "管理端登录", description = "仅 role_type=ADMIN 的账号可登录；同一 IP 60 秒最多 10 次")
    @RateLimit(key = "auth:admin-login", count = 10, seconds = 60, dimension = RateLimit.Dimension.IP)
    @PostMapping("/admin/login")
    public R<LoginVO> adminLogin(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto, true));
    }

    @Operation(summary = "用户端登录", description = "同一 IP 60 秒最多 20 次")
    @RateLimit(key = "auth:login", count = 20, seconds = 60, dimension = RateLimit.Dimension.IP)
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto, false));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok("注册成功", authService.register(dto));
    }
}
