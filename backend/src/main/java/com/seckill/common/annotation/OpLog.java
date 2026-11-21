package com.seckill.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标在管理端的增删改接口上，AOP 自动记录谁、什么时候、做了什么、耗时多少
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {

    /** 模块名，如 商品管理 */
    String module();

    /** 动作名，如 新增商品 */
    String action();
}
