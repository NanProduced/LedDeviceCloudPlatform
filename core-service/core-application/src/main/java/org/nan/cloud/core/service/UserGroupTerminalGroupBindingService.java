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

    /**
     * 为用户组自动添加对新创建终端组的权限绑定
     * @param ugid 用户组ID
     * @param newTgid 新创建的终端组ID
     * @param parentTgid 父终端组ID
     * @param creatorId 创建者ID
     * @param oid 组织ID
     */
    void autoBindNewTerminalGroupPermission(Long ugid, Long newTgid, Long parentTgid, Long creatorId, Long oid);
}