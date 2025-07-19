package org.nan.cloud.core.service;

import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.DTO.res.TerminalGroupTreeResponse;
import org.nan.cloud.core.DTO.res.UserGroupPermissionStatusResponse;

import java.util.List;

/**
 * 业务缓存服务接口
 * 封装具体业务场景的缓存操作
 */
public interface BusinessCacheService {
    
    // 权限相关缓存
    Boolean getUserPermission(Long userId, Long orgId, String url, String method);
    void cacheUserPermission(Long userId, Long orgId, String url, String method, Boolean hasPermission);
    void evictUserPermissions(Long userId, Long orgId);
    
    UserGroupPermissionStatusResponse getUserGroupPermissionStatus(Long ugid);
    void cacheUserGroupPermissionStatus(Long ugid, UserGroupPermissionStatusResponse status);
    void evictUserGroupPermissionStatus(Long ugid);
    
    // 终端组缓存
    TerminalGroupTreeResponse getTerminalGroupTree(Long orgId);
    void cacheTerminalGroupTree(Long orgId, TerminalGroupTreeResponse tree);
    void evictTerminalGroupTree(Long orgId);
    
    TerminalGroup getTerminalGroup(Long tgid);
    void cacheTerminalGroup(TerminalGroup terminalGroup);
    void evictTerminalGroup(Long tgid);
    
    // 用户相关缓存
    User getUser(Long userId);
    void cacheUser(User user);
    void evictUser(Long userId);
    
    List<UserGroup> getUserGroups(Long orgId);
    void cacheUserGroups(Long orgId, List<UserGroup> userGroups);
    void evictUserGroups(Long orgId);
    
    // 组织相关缓存
    void evictOrganizationCache(Long orgId);
}