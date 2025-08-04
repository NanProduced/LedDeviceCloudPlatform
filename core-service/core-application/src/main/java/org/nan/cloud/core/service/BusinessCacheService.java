package org.nan.cloud.core.service;

import org.nan.cloud.core.domain.Task;

import java.time.Duration;
import java.util.List;

/**
 * 业务缓存服务接口
 * 封装具体业务场景的缓存操作，支持组织级别的缓存隔离
 */
public interface BusinessCacheService {

    // 用户组-终端组绑定关系缓存
    <T> T getUserGroupPermissionStatus(Long ugid, Long orgId, Class<T> clazz);
    void cacheUserGroupPermissionStatus(Long ugid, Long orgId, Object status);
    void evictUserGroupPermissionStatus(Long ugid, Long orgId);
    void evictAllUserGroupPermissions(Long orgId); // 清理组织内所有用户组权限缓存
    // 任务进度缓存
    void cacheTaskProgress(Long oid, String taskId, Task task, Duration ttl);
    Task getTaskProgress(Long oid, String taskId);
    Task getTaskProgress(String taskId);
    void evictTaskProgress(String taskId);

    // 用户组可见终端组列表缓存
    <T> List<T> getUserAccessibleTerminalGroupIds(Long ugid, Long orgId, Class<T> clazz);
    void cacheUserAccessibleTerminalGroupIds(Long ugid, Long orgId, List<?> terminalGroupIds);
    void evictUserAccessibleTerminalGroupIds(Long ugid, Long orgId);
    void evictAllUserAccessibleTerminalGroups(Long orgId);
    
    // 终端组缓存（按组织隔离）
    <T> T getTerminalGroup(Long tgid, Long orgId, Class<T> clazz);
    void cacheTerminalGroup(Object terminalGroup, Long tgid, Long orgId);
    void evictTerminalGroup(Long tgid, Long orgId);
    void evictAllTerminalGroups(Long orgId); // 清理组织内所有终端组缓存
    
    // 用户组相关缓存（按组织隔离）
    <T> List<T> getUserGroups(Long orgId, Class<T> clazz);
    void cacheUserGroups(Long orgId, List<?> userGroups);
    void evictUserGroups(Long orgId);
    
    // 组织级别缓存管理
    void evictOrganizationCache(Long orgId); // 清理整个组织的所有缓存
    void evictMultiOrganizationCache(List<Long> orgIds); // 批量清理多个组织缓存
}