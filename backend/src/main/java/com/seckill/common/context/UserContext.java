package com.seckill.common.context;

import com.seckill.common.exception.BizException;

/**
 * ThreadLocal 保存当前请求的登录用户。
 * Controller 里直接 UserContext.requireUserId()，不用每个方法都接 token 参数。
 * 注意：异步线程/线程池里取不到，需要显式传递。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("请先登录");
        }
        return user;
    }

    public static Long requireUserId() {
        return require().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
