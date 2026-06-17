package com.bookorder.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookorder.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {

    IPage<OperationLog> pageLogs(int pageNum, int pageSize, String module);

    void log(Long userId, String username, String module, String operation, String target, String detail, String ip);
}
