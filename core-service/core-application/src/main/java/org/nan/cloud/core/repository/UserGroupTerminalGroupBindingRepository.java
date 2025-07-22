package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.UserGroupTerminalGroupBinding;

import java.util.List;
import java.util.Map;

public interface UserGroupTerminalGroupBindingRepository {

    /**
     * 获取用户组的原始绑定数据（不进行权限计算）
     * 权限计算逻辑移至Service层处理INCLUDE/EXCLUDE混合场景
     */
    List<UserGroupTerminalGroupBinding> getUserGroupBindings(Long ugid);


    /**
     * 检查用户组是否有终端组权限（基于INCLUDE/EXCLUDE绑定类型精确计算）
     * 使用数据库CTE优化，一次查询返回精确结果
     */
    boolean hasTerminalGroupPermission(Long ugid, Long tgid);
    
    /**
     * 批量权限校验（高性能版本）
     * 一次查询检查多个终端组的权限状态，减少数据库访问次数
     */
    Map<Long, Boolean> batchCheckPermissions(Long ugid, List<Long> tgids);
    
    /**
     * 批量删除用户组的权限绑定
     */
    void deleteUserGroupBindings(Long ugid);
    
    /**
     * 批量创建权限绑定
     */
    void batchCreateBindings(List<UserGroupTerminalGroupBinding> bindings);

    /**
     * 创建组织时初始化根终端组和根用户组绑定关系
     * @param oid
     * @param ugid
     * @param tgid
     */
    void initOrgBindings(Long oid, Long ugid, Long tgid);
    
    /**
     * 获取用户组权限绑定详细信息（包含终端组名称和路径）
     * 支持INCLUDE/EXCLUDE绑定类型和完整的终端组信息
     */
    List<UserGroupTerminalGroupBinding> getUserGroupPermissionDetails(Long ugid);
    
    /**
     * 批量更新用户组绑定关系（支持混合INCLUDE/EXCLUDE类型）
     * 使用事务确保原子性：先删除后插入
     * @param ugid 用户组ID
     * @param newBindings 新的绑定关系列表（可包含INCLUDE/EXCLUDE混合类型）
     */
    void replaceUserGroupPermissions(Long ugid, List<UserGroupTerminalGroupBinding> newBindings);

}