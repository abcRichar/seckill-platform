package com.seckill.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.system.entity.SysOperationLog;
import com.seckill.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogService {

    private final SysOperationLogMapper logMapper;

    /**
     * 异步落库：日志写失败不能影响业务接口
     */
    @Async
    public void saveAsync(SysOperationLog logRecord) {
        try {
            logMapper.insert(logRecord);
        } catch (Exception e) {
            log.error("操作日志保存失败: {}", e.getMessage());
        }
    }

    public Page<SysOperationLog> page(long current, long size, String keyword, String module) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysOperationLog::getUsername, keyword)
                    .or().like(SysOperationLog::getAction, keyword));
        }
        wrapper.eq(StringUtils.hasText(module), SysOperationLog::getModule, module);
        wrapper.orderByDesc(SysOperationLog::getId);
        return logMapper.selectPage(new Page<>(current, size), wrapper);
    }
}
