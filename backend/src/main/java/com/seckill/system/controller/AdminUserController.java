package com.seckill.system.controller;

import com.seckill.common.annotation.OpLog;
import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.R;
import com.seckill.system.entity.SysRole;
import com.seckill.system.entity.SysUser;
import com.seckill.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端-用户管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserService userService;

    @Operation(summary = "用户分页")
    @RequirePerm("user:list")
    @GetMapping
    public R<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(PageResult.of(userService.page(page, size, keyword, status)));
    }

    @Operation(summary = "启用/禁用用户")
    @RequirePerm("user:edit")
    @OpLog(module = "用户管理", action = "修改用户状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @RequirePerm("user:edit")
    @OpLog(module = "用户管理", action = "重置用户密码")
    @PutMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id,
                                 @RequestParam(defaultValue = "123456") String password) {
        userService.resetPassword(id, password);
        return R.ok();
    }

    @Operation(summary = "用户角色ID列表")
    @RequirePerm("user:list")
    @GetMapping("/{id}/roles")
    public R<List<Long>> roleIds(@PathVariable Long id) {
        return R.ok(userService.getRoleIds(id));
    }

    @Operation(summary = "分配角色")
    @RequirePerm("user:edit")
    @OpLog(module = "用户管理", action = "分配用户角色")
    @PutMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return R.ok();
    }

    @Operation(summary = "用户已有角色")
    @RequirePerm("user:list")
    @GetMapping("/{id}/role-list")
    public R<List<SysRole>> roles(@PathVariable Long id) {
        return R.ok(userService.getRoles(id));
    }
}
