package com.seckill.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.seckill.common.exception.BizException;
import com.seckill.common.util.RedisService;
import com.seckill.system.entity.SysRole;
import com.seckill.system.entity.SysUser;
import com.seckill.system.entity.SysUserRole;
import com.seckill.system.mapper.SysMenuMapper;
import com.seckill.system.mapper.SysRoleMapper;
import com.seckill.system.mapper.SysUserMapper;
import com.seckill.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

/**
 * 用户 + RBAC 查询
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final String PERM_CACHE_KEY = "sk:auth:perms:";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;

    public SysUser getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
    }

    public SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user;
    }

    /**
     * 权限码查询 + Redis 缓存 10 分钟。
     * 角色/权限变更时会主动删除缓存（见 SysRoleService），避免权限改完不生效。
     */
    public List<String> getPermissions(Long userId) {
        String key = PERM_CACHE_KEY + userId;
        List<String> cached = redisService.get(key, new TypeReference<List<String>>() {
        });
        if (cached != null) {
            return cached;
        }
        List<String> permissions = menuMapper.selectPermCodesByUserId(userId);
        redisService.set(key, permissions, Duration.ofMinutes(10));
        return permissions;
    }

    public void evictPermissions(Long userId) {
        redisService.delete(PERM_CACHE_KEY + userId);
    }

    public Page<SysUser> page(long current, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.eq(status != null, SysUser::getStatus, status);
        wrapper.orderByDesc(SysUser::getId);
        return userMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public void updateStatus(Long userId, Integer status) {
        SysUser user = getById(userId);
        if ("ADMIN".equals(user.getRoleType()) && status == 0) {
            throw BizException.badRequest("不能禁用管理员账号");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    public void resetPassword(Long userId, String rawPassword) {
        getById(userId);
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(update);
    }

    public List<Long> getRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    public List<SysRole> getRoles(Long userId) {
        return roleMapper.selectByUserId(userId);
    }

    /**
     * 分配角色：先删后插，同时根据是否拥有 ADMIN 角色同步 role_type。
     * 注意：已签发的 JWT 里 roleType 是旧的，需要重新登录才会进入新身份，这是无状态 token 的取舍。
     */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        SysUser current = getById(userId);
        boolean hasAdmin = false;
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                SysRole role = roleMapper.selectById(roleId);
                if (role != null) {
                    hasAdmin = hasAdmin || "ADMIN".equals(role.getRoleCode());
                }
            }
        }
        if ("ADMIN".equals(current.getRoleType()) && !hasAdmin) {
            Long otherAdmins = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRoleType, "ADMIN")
                    .ne(SysUser::getId, userId));
            if (otherAdmins == null || otherAdmins == 0) {
                throw BizException.badRequest("系统至少保留一个管理员，不能移除最后一个管理员身份");
            }
        }

        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                SysRole role = roleMapper.selectById(roleId);
                if (role == null) {
                    continue;
                }
                userRoleMapper.insert(new SysUserRole(userId, roleId));
            }
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setRoleType(hasAdmin ? "ADMIN" : "USER");
        userMapper.updateById(update);
        evictPermissions(userId);
    }

    public long countUsers() {
        return userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleType, "USER"));
    }

    public SysUserMapper getMapper() {
        return userMapper;
    }
}
