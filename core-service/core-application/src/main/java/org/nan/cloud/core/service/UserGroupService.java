package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.CreateUserGroupDTO;
import org.nan.cloud.core.DTO.UserGroupRelDTO;
import org.nan.cloud.core.domain.UserGroup;

import java.util.List;

public interface UserGroupService {

    /**
     * 根据ID获取用户组
     */
    UserGroup getUserGroupById(Long ugid);
    
    /**
     * 根据ID获取用户组（带缓存，需要组织ID进行缓存隔离）
     */
    UserGroup getUserGroupById(Long oid, Long ugid);

    /**
     * 创建用户组（带缓存清理）
     */
    void createUserGroup(CreateUserGroupDTO createUserGroupDTO, Long orgId);

    
    /**
     * 删除用户组（带缓存清理）
     */
    void deleteUserGroup(Long oid, Long ugid, Long orgId);
    
    /**
     * 查询组下所有用户组（包含当前组）
     */
    List<UserGroupRelDTO> getAllUserGroupsByParent(Long ugid);
    
    /**
     * 将新创建的用户组添加到缓存
     */
    UserGroup cacheNewUserGroup(Long oid, Long ugid, UserGroup userGroup);
    
    /**
     * 清理指定用户组的缓存
     */
    void evictUserGroupCache(Long oid, Long ugid);
}
