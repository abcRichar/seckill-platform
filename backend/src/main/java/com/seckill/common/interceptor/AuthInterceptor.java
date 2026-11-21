package com.seckill.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.common.annotation.RequireLogin;
import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.context.LoginUser;
import com.seckill.common.context.UserContext;
import com.seckill.common.result.R;
import com.seckill.common.result.ResultCode;
import com.seckill.common.util.JwtUtil;
import com.seckill.system.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 统一鉴权拦截器：
 * 1. /api/admin/**  必须 ADMIN + 具体权限（@RequirePerm）
 * 2. mall 接口默认匿名可读，标注 @RequireLogin 的必须登录
 * 3. 解析出的用户放进 ThreadLocal
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final SysUserService sysUserService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 防御性清理：避免线程池复用上一次请求残留的用户
        UserContext.clear();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = extractToken(request);
        LoginUser user = StringUtils.hasText(token) ? jwtUtil.parse(token) : null;
        boolean adminPath = request.getRequestURI().startsWith("/api/admin/");
        boolean loginRequired = adminPath || hasAnnotation(handlerMethod, RequireLogin.class);

        if (user == null) {
            if (loginRequired) {
                writeError(response, ResultCode.UNAUTHORIZED, "请先登录或登录已过期");
                return false;
            }
            return true;
        }

        if (adminPath && !user.isAdmin()) {
            writeError(response, ResultCode.FORBIDDEN, "无权访问管理端");
            return false;
        }

        UserContext.set(user);

        if (adminPath) {
            RequirePerm requirePerm = findAnnotation(handlerMethod, RequirePerm.class);
            if (requirePerm != null) {
                List<String> permissions = sysUserService.getPermissions(user.getUserId());
                if (!permissions.contains("*") && !permissions.contains(requirePerm.value())) {
                    writeError(response, ResultCode.FORBIDDEN, "缺少权限：" + requirePerm.value());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 线程池复用，必须清理，否则会串号
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization)) {
            return authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        }
        return request.getHeader("token");
    }

    private boolean hasAnnotation(HandlerMethod handlerMethod, Class<? extends java.lang.annotation.Annotation> type) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), type)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), type);
    }

    private <A extends java.lang.annotation.Annotation> A findAnnotation(HandlerMethod handlerMethod, Class<A> type) {
        A annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), type);
        return annotation != null ? annotation : AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), type);
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        // preHandle 返回 false 时不会触发 afterCompletion，这里必须手动清理
        UserContext.clear();
        // HTTP 状态码与 body code 保持一致：401 未登录 / 403 无权限
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code, message)));
    }
}
