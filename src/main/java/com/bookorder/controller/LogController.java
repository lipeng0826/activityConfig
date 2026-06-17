package com.bookorder.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookorder.common.Result;
import com.bookorder.entity.OperationLog;
import com.bookorder.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    @Autowired
    private OperationLogService logService;

    @GetMapping
    public Result<IPage<OperationLog>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String module) {
        return Result.success(logService.pageLogs(pageNum, pageSize, module));
    }
}
