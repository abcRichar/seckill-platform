package com.seckill.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.exception.BizException;
import com.seckill.system.entity.SysMenu;
import com.seckill.system.entity.SysRole;
import com.seckill.system.entity.SysRoleMenu;
import com.seckill.system.entity.SysUserRole;
import com.seckill.system.mapper.SysMenuMapper;
import com.seckill.system.mapper.SysRoleMapper;
import com.seckill.system.mapper.SysRoleMenuMapper;
import com.seckill.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserService userService;

    public List<SysRole> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public List<SysMenu> listMenus() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getId));
    }

    public SysRole getById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BizException.notFound("角色不存在");
        }
        return role;
    }

    public void create(SysRole role) {
        if (!StringUtils.hasText(role.getRoleName()) || !StringUtils.hasText(role.getRoleCode())) {
            throw BizException.badRequest("角色名和角色编码不能为空");
        }
        SysRole exists = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, role.getRoleCode()).last("LIMIT 1"));
        if (exists != null) {
            throw BizException.badRequest("角色编码已存在");
        }
        role.setId(null);
        roleMapper.insert(role);
    }

    public void update(Long id, SysRole role) {
        SysRole old = getById(id);
        if ("ADMIN".equals(old.getRoleCode()) && !"ADMIN".equals(role.getRoleCode())) {
            throw BizException.badRequest("内置管理员角色编码不可修改");
        }
        role.setId(id);
        roleMapper.updateById(role);
        evictRoleUsersPermissions(id);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = getById(id);
        if ("ADMIN".equals(role.getRoleCode())) {
            throw BizException.badRequest("内置管理员角色不可删除");
        }
        Long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (userCount > 0) {
            throw BizException.badRequest("该角色下还有 " + userCount + " 个用户，无法删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    public List<Long> getMenuIds(Long roleId) {
        getById(roleId);
        return menuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = getById(roleId);
        List<Long> effective = new java.util.ArrayList<>(menuIds == null ? List.of() : menuIds);
        // 内置管理员角色必须保留 "*" 权限，避免误操作把自己锁在系统外
        if ("ADMIN".equals(role.getRoleCode())) {
            SysMenu all = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getPermCode, "*").last("LIMIT 1"));
            if (all != null && !effective.contains(all.getId())) {
                effective.add(all.getId());
            }
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        for (Long menuId : effective) {
            if (menuMapper.selectById(menuId) != null) {
                roleMenuMapper.insert(new SysRoleMenu(roleId, menuId));
            }
        }
        evictRoleUsersPermissions(roleId);
    }

    /** 角色权限变了，该角色下所有用户的权限缓存都要清掉 */
    private void evictRoleUsersPermissions(Long roleId) {
        roleMapper.selectUserIdsByRoleId(roleId).forEach(userService::evictPermissions);
    }
}
