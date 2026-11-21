package com.seckill.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端权限校验：例如 @RequirePerm("product:edit")
 * 拦截器会查询当前用户拥有的 perm_code；拥有 "*" 表示超级管理员
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePerm {

    String value();
}
