package com.seckill.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.system.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT r.* FROM sys_role r " +
            "JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} ORDER BY r.id")
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT ur.user_id FROM sys_user_role ur WHERE ur.role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
