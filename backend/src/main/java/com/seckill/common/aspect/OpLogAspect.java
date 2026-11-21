package com.seckill.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.common.annotation.OpLog;
import com.seckill.common.context.UserContext;
import com.seckill.common.util.IpUtil;
import com.seckill.system.entity.SysOperationLog;
import com.seckill.system.service.SysOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

/**
 * 操作日志切面：记录管理端写操作。
 * 日志落库走 @Async，避免拖慢接口响应。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final ObjectMapper objectMapper;
    private final SysOperationLogService operationLogService;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperationLog record = new SysOperationLog();
        record.setModule(opLog.module());
        record.setAction(opLog.action());
        record.setMethod(joinPoint.getSignature().toShortString());
        record.setUserId(UserContext.getUserId());
        record.setUsername(UserContext.get() == null ? "anonymous" : UserContext.get().getUsername());
        record.setParams(serializeArgs(joinPoint.getArgs()));
        record.setIp(currentIp());

        try {
            Object result = joinPoint.proceed();
            record.setStatus(1);
            return result;
        } catch (Throwable e) {
            record.setStatus(0);
            record.setErrorMsg(truncate(e.getMessage(), 480));
            throw e;
        } finally {
            record.setDuration(System.currentTimeMillis() - start);
            operationLogService.saveAsync(record);
        }
    }

    private String serializeArgs(Object[] args) {
        try {
            Object[] filtered = Arrays.stream(args)
                    .filter(arg -> !(arg instanceof MultipartFile)
                            && !(arg instanceof HttpServletRequest)
                            && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                    // 顶层字符串参数可能是密码等敏感值，直接脱敏（如重置密码的 password 参数）
                    .map(arg -> arg instanceof String s && s.length() >= 6 ? "***" : arg)
                    .toArray();
            return truncate(objectMapper.writeValueAsString(filtered), 4000);
        } catch (Exception e) {
            return "参数序列化失败";
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private String currentIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        return IpUtil.getIp(request);
    }
}
