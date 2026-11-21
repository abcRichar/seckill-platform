package com.seckill.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.auth.dto.ChangePasswordDTO;
import com.seckill.auth.dto.LoginDTO;
import com.seckill.auth.dto.ProfileDTO;
import com.seckill.auth.dto.RegisterDTO;
import com.seckill.auth.vo.LoginVO;
import com.seckill.auth.vo.UserProfileVO;
import com.seckill.common.exception.BizException;
import com.seckill.common.util.JwtUtil;
import com.seckill.system.entity.SysRole;
import com.seckill.system.entity.SysUser;
import com.seckill.system.entity.SysUserRole;
import com.seckill.system.mapper.SysRoleMapper;
import com.seckill.system.mapper.SysUserMapper;
import com.seckill.system.mapper.SysUserRoleMapper;
import com.seckill.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * @param adminOnly 管理端登录要求 role_type=ADMIN；用户端任意账号都可登录
     */
    public LoginVO login(LoginDTO dto, boolean adminOnly) {
        SysUser user = userService.getByUsername(dto.getUsername().trim());
        // 用户不存在和密码错误返回同样的提示，避免账号枚举
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw BizException.badRequest("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已被禁用，请联系管理员");
        }
        if (adminOnly && !"ADMIN".equals(user.getRoleType())) {
            throw BizException.forbidden("该账号没有管理端权限");
        }
        return buildLoginVO(user);
    }

    /**
     * 注册：默认 USER 身份 + 分配普通用户角色
     */
    @Transactional
    public LoginVO register(RegisterDTO dto) {
        String username = dto.getUsername().trim();
        if (userService.getByUsername(username) != null) {
            throw BizException.badRequest("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : "用户" + username);
        user.setAvatar(StringUtils.hasText(dto.getAvatar()) ? dto.getAvatar() : defaultAvatar(username));
        user.setRoleType("USER");
        user.setStatus(1);
        userMapper.insert(user);

        SysRole userRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "USER").last("LIMIT 1"));
        if (userRole != null) {
            userRoleMapper.insert(new SysUserRole(user.getId(), userRole.getId()));
        }
        return buildLoginVO(user);
    }

    public UserProfileVO getProfile(Long userId, boolean withPermissions) {
        SysUser user = userService.getById(userId);
        List<String> permissions = withPermissions ? userService.getPermissions(userId) : List.of();
        List<String> roleNames = roleMapper.selectByUserId(userId).stream()
                .map(SysRole::getRoleName).toList();
        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .email(user.getEmail())
                .roleType(user.getRoleType())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .permissions(permissions)
                .roleNames(roleNames)
                .build();
    }

    public void updateProfile(Long userId, ProfileDTO dto) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setNickname(dto.getNickname());
        update.setAvatar(dto.getAvatar());
        update.setPhone(dto.getPhone());
        update.setEmail(dto.getEmail());
        userMapper.updateById(update);
    }

    public void changePassword(Long userId, ChangePasswordDTO dto) {
        SysUser user = userService.getById(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw BizException.badRequest("原密码不正确");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw BizException.badRequest("新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    private LoginVO buildLoginVO(SysUser user) {
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRoleType());
        List<String> permissions = "ADMIN".equals(user.getRoleType())
                ? userService.getPermissions(user.getId())
                : List.of();
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roleType(user.getRoleType())
                .permissions(permissions)
                .build();
    }

    /** 使用本地头像，避免离线环境依赖外部图片服务 */
    private String defaultAvatar(String username) {
        String[] seeds = {"alice", "bob", "carol", "david", "emma", "admin"};
        int index = Math.floorMod(username.hashCode(), seeds.length);
        return "/uploads/seed/avatar-" + seeds[index] + ".svg";
    }
}
