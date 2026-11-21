package com.seckill.system.controller;

import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.R;
import com.seckill.system.entity.SysOperationLog;
import com.seckill.system.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-操作日志")
@RestController
@RequestMapping("/api/admin/logs")
@RequirePerm("log:list")
@RequiredArgsConstructor
public class AdminLogController {

    private final SysOperationLogService logService;

    @Operation(summary = "操作日志分页")
    @GetMapping
    public R<PageResult<SysOperationLog>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String module) {
        return R.ok(PageResult.of(logService.page(page, size, keyword, module)));
    }
}
