package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.UserGroupTerminalGroupBinding;

import java.util.List;

public interface UserGroupTerminalGroupBindingRepository {

    /**
     * 获取用户组绑定的终端组列表
     */
    List<UserGroupTerminalGroupBinding> getUserGroupBindings(Long ugid);


    /**
     * 检查用户组是否有终端组权限（包含子组）
     */
    boolean hasTerminalGroupPermission(Long ugid, Long tgid);

    /**
     * 获取用户组可访问的终端组ID列表
     */
    List<Long> getAccessibleTerminalGroupIds(Long ugid);

    /**
     * 全量替换用户组的权限绑定
     * 先删除该用户组的所有绑定关系，然后批量插入新的绑定关系
     */
    void replaceUserGroupPermissions(Long ugid, List<UserGroupTerminalGroupBinding> newBindings);
    
    /**
     * 批量删除用户组的权限绑定
     */
    void deleteUserGroupBindings(Long ugid);
    
    /**
     * 批量创建权限绑定
     */
    void batchCreateBindings(List<UserGroupTerminalGroupBinding> bindings);
    
    /**
     * 获取用户组权限绑定详细信息（包含终端组名称和路径）
     */
    List<UserGroupTerminalGroupBinding> getUserGroupPermissionDetails(Long ugid);

}