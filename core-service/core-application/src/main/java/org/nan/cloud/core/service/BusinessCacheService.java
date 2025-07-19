package org.nan.cloud.core.service;

import java.util.List;

/**
 * 业务缓存服务接口
 * 封装具体业务场景的缓存操作，支持组织级别的缓存隔离
 */
public interface BusinessCacheService {
    
    // 权限相关缓存（按组织隔离）
    Boolean getUserPermission(Long userId, Long orgId, String url, String method);
    void cacheUserPermission(Long userId, Long orgId, String url, String method, Boolean hasPermission);
    void evictUserPermissions(Long userId, Long orgId);
    void evictAllUserPermissions(Long orgId); // 清理组织内所有用户权限缓存
    
    <T> T getUserGroupPermissionStatus(Long ugid, Long orgId, Class<T> clazz);
    void cacheUserGroupPermissionStatus(Long ugid, Long orgId, Object status);
    void evictUserGroupPermissionStatus(Long ugid, Long orgId);
    void evictAllUserGroupPermissions(Long orgId); // 清理组织内所有用户组权限缓存
    
    // 终端组缓存（按组织隔离）
    <T> T getTerminalGroupTree(Long orgId, Class<T> clazz);
    void cacheTerminalGroupTree(Long orgId, Object tree);
    void evictTerminalGroupTree(Long orgId);
    
    <T> T getTerminalGroup(Long tgid, Long orgId, Class<T> clazz);
    void cacheTerminalGroup(Object terminalGroup, Long tgid, Long orgId);
    void evictTerminalGroup(Long tgid, Long orgId);
    void evictAllTerminalGroups(Long orgId); // 清理组织内所有终端组缓存
    
    // 用户相关缓存（按组织隔离）
    <T> T getUser(Long userId, Long orgId, Class<T> clazz);
    void cacheUser(Object user, Long userId, Long orgId);
    void evictUser(Long userId, Long orgId);
    void evictAllUsers(Long orgId); // 清理组织内所有用户缓存
    
    <T> List<T> getUserGroups(Long orgId, Class<T> clazz);
    void cacheUserGroups(Long orgId, List<?> userGroups);
    void evictUserGroups(Long orgId);
    
    // 组织级别缓存管理
    void evictOrganizationCache(Long orgId); // 清理整个组织的所有缓存
    void evictMultiOrganizationCache(List<Long> orgIds); // 批量清理多个组织缓存
    
    // 全局缓存（不按组织隔离的系统级缓存）
    <T> T getSystemConfig(String configKey, Class<T> clazz);
    void cacheSystemConfig(String configKey, Object value);
    void evictSystemConfig(String configKey);
}