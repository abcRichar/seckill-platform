package com.seckill.system.controller;

import com.seckill.common.annotation.OpLog;
import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.result.R;
import com.seckill.system.dto.MenuIdsDTO;
import com.seckill.system.entity.SysMenu;
import com.seckill.system.entity.SysRole;
import com.seckill.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端-角色权限")
@RestController
@RequestMapping("/api/admin/roles")
@RequirePerm("role:manage")
@RequiredArgsConstructor
public class AdminRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色列表")
    @GetMapping
    public R<List<SysRole>> list() {
        return R.ok(roleService.listAll());
    }

    @Operation(summary = "全部权限点")
    @GetMapping("/menus")
    public R<List<SysMenu>> menus() {
        return R.ok(roleService.listMenus());
    }

    @Operation(summary = "角色拥有权限ID")
    @GetMapping("/{id}/menus")
    public R<List<Long>> roleMenus(@PathVariable Long id) {
        return R.ok(roleService.getMenuIds(id));
    }

    @Operation(summary = "新增角色")
    @OpLog(module = "权限管理", action = "新增角色")
    @PostMapping
    public R<Void> create(@RequestBody SysRole role) {
        roleService.create(role);
        return R.ok();
    }

    @Operation(summary = "修改角色")
    @OpLog(module = "权限管理", action = "修改角色")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        roleService.update(id, role);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @OpLog(module = "权限管理", action = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @Operation(summary = "给角色分配权限")
    @OpLog(module = "权限管理", action = "分配角色权限")
    @PutMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody MenuIdsDTO dto) {
        roleService.assignMenus(id, dto.getMenuIds());
        return R.ok();
    }
}
