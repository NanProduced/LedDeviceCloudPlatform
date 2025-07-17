package org.nan.cloud.core.service;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.DTO.BindUserGroupDTO;
import org.nan.cloud.core.DTO.QueryUserGroupBindingDTO;
import org.nan.cloud.core.DTO.UserGroupBindingDTO;

import java.util.List;

public interface UserGroupTerminalGroupBindingService {

    /**
     * 绑定用户组到终端组
     */
    void bindUserGroupToTerminalGroup(BindUserGroupDTO bindUserGroupDTO);

    /**
     * 解绑用户组与终端组
     */
    void unbindUserGroupFromTerminalGroup(Long tgid, Long ugid, Long operatorId);

    /**
     * 获取终端组绑定的用户组列表
     */
    PageVO<UserGroupBindingDTO> getTerminalGroupBindings(Integer pageNum, Integer pageSize, QueryUserGroupBindingDTO queryDTO);

    /**
     * 检查用户组是否有终端组权限
     */
    boolean hasTerminalGroupPermission(Long ugid, Long tgid);

    /**
     * 获取用户组可访问的终端组列表
     */
    List<Long> getAccessibleTerminalGroupIds(Long ugid);

    /**
     * 根据终端组ID获取有权限的用户组列表
     */
    List<Long> getAuthorizedUserGroupIds(Long tgid);
}