package com.seckill.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.system.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 一条 SQL 查出用户拥有的全部权限码（多表 join）。
     * 比"先查角色再查权限"少两次查询。
     */
    @Select("SELECT DISTINCT m.perm_code FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
            "WHERE ur.user_id = #{userId} ORDER BY m.perm_code")
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
