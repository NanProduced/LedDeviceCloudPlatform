package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.PermissionExpressionDTO;
import org.nan.cloud.core.DTO.PermissionExpressionResultDTO;
import org.nan.cloud.core.DTO.UserGroupPermissionStatusDTO;

import java.util.List;

public interface UserGroupTerminalGroupBindingService {

    /**
     * 获取用户组可访问的终端组列表
     */
    List<Long> getAccessibleTerminalGroupIds(Long ugid);

    /**
     * 更新权限表达式 - 全量替换用户组的权限绑定
     */
    PermissionExpressionResultDTO updatePermissionExpression(PermissionExpressionDTO request);

    /**
     * 获取用户组权限状态
     */
    UserGroupPermissionStatusDTO getUserGroupPermissionStatus(Long ugid);
}