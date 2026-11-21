package com.seckill.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权限点（菜单/按钮），perm_code 与 @RequirePerm 对应
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String permName;
    private String permCode;
    /** MENU / BUTTON */
    private String type;
}
