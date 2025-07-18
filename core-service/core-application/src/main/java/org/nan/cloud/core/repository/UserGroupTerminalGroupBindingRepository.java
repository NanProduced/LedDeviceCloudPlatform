package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.UserGroupTerminalGroupBinding;

import java.util.List;

public interface UserGroupTerminalGroupBindingRepository {

    /**
     * 创建用户组-终端组绑定
     */
    void createBinding(UserGroupTerminalGroupBinding binding);

    /**
     * 删除用户组-终端组绑定
     */
    void deleteBinding(Long tgid, Long ugid);

    /**
     * 更新用户组-终端组绑定
     */
    void updateBinding(UserGroupTerminalGroupBinding binding);

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

}