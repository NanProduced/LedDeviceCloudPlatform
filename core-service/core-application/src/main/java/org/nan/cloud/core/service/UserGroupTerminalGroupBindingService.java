package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.BatchBindingOperationDTO;
import org.nan.cloud.core.DTO.BatchBindingOperationResultDTO;

import java.util.List;

public interface UserGroupTerminalGroupBindingService {

    /**
     * 批量绑定操作 - 智能处理复杂的绑定关系调整
     */
    BatchBindingOperationResultDTO executeBatchBindingOperation(BatchBindingOperationDTO request);

    /**
     * 获取用户组可访问的终端组列表
     */
    List<Long> getAccessibleTerminalGroupIds(Long ugid);
}